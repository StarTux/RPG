package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.util.Dirty.TagWrapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

@Getter
final class RPGWorld {
    final RPGPlugin plugin;
    final World world;
    final List<Town> towns = new ArrayList<>();
    boolean dirty = false;
    int addTownCooldown = 0;
    int ticks;
    long timestamp;

    RPGWorld(RPGPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        this.timestamp = System.currentTimeMillis();
    }

    void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "winthier.rpg.yml"));
        for (Map<?, ?> map: config.getMapList("towns")) {
            ConfigurationSection section = config.createSection("tmp", map);
            towns.add(new Town(section));
        }
        timestamp = config.getLong("timestamp", timestamp);
    }

    void save() {
        dirty = false;
        YamlConfiguration config = new YamlConfiguration();
        config.set("towns", towns.stream().map(t -> t.serialize()).collect(Collectors.toList()));
        config.set("timestamp", timestamp);
        try {
            config.save(new File(world.getWorldFolder(), "winthier.rpg.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    Town findTown(String name) {
        for (Town town: towns) if (name.equals(town.name)) return town;
        return null;
    }

    final class Town {
        final Rectangle area;
        final Rectangle questArea;
        final List<NPC> npcs = new ArrayList<>();
        final List<Quest> quests = new ArrayList<>();
        final List<String> tags = new ArrayList<>();
        final List<Struct> structs = new ArrayList<>();
        final String name;
        final Fraction fraction;
        boolean visited = false;

        Town(Rectangle area, String name, Fraction fraction) {
            this.area = area;
            this.questArea = area.grow(64);
            this.name = name;
            this.fraction = fraction;
        }

        Town(ConfigurationSection config) {
            for (Map<?, ?> map: config.getMapList("npcs")) {
                ConfigurationSection section = config.createSection("tmp", map);
                this.npcs.add(new NPC(section));
            }
            for (Map<?, ?> map: config.getMapList("quests")) {
                ConfigurationSection section = config.createSection("tmp", map);
                this.quests.add(new Quest(section));
            }
            for (Map<?, ?> map: config.getMapList("structs")) {
                ConfigurationSection section = config.createSection("tmp", map);
                this.structs.add(new Struct(section));
            }
            this.area = new Rectangle(config.getIntegerList("area"));
            this.questArea = area.grow(64);
            this.name = config.getString("name");
            Fraction fraction;
            try {
                fraction = Fraction.valueOf(config.getString("fraction", "VILLAGER").toUpperCase());
            } catch (IllegalArgumentException iae) {
                fraction = Fraction.VILLAGER;
                iae.printStackTrace();
            }
            this.fraction = fraction;
            this.tags.addAll(config.getStringList("tags"));
            this.visited = config.getBoolean("visited");
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", name);
            result.put("fraction", fraction.name().toLowerCase());
            result.put("area", area.serialize());
            result.put("tags", tags);
            result.put("visited", visited);
            result.put("structs", structs.stream().map(struct -> struct.serialize()).collect(Collectors.toList()));
            result.put("npcs", npcs.stream().map(npc -> npc.serialize()).collect(Collectors.toList()));
            result.put("quests", quests.stream().map(quest -> quest.serialize()).collect(Collectors.toList()));
            return result;
        }

        void visit() {
            if (visited) return;
            visited = true;
            dirty = true;
            for (Struct struct: structs) {
                Cuboid bb = struct.boundingBox.grow(1);
                for (int az = bb.az; az <= bb.bz; az += 1) {
                    for (int ay = bb.ay; ay <= bb.by; ay += 1) {
                        for (int ax = bb.ax; ax <= bb.bx; ax += 1) {
                            Block block = world.getBlockAt(ax, ay, az);
                            if (block.getType() == Material.AIR && block.getLightLevel() == 0) {
                                block.setType(Material.GLOWSTONE);
                                plugin.getServer().getScheduler().runTask(plugin, () -> block.setType(Material.AIR));
                            }
                        }
                    }
                }
            }
        }
    }

    static final class NPC {
        final Vec3 home;
        String message = "";
        String name = "";
        int questId = -1;

        NPC(Vec3 home) {
            this.home = home;
        }

        NPC(ConfigurationSection config) {
            this.home = new Vec3(config.getIntegerList("home"));
            this.message = config.getString("message", "");
            this.name = config.getString("name", "");
            this.questId = config.getInt("quest_id", -1);
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", name);
            result.put("home", home.serialize());
            result.put("quest_id", questId);
            result.put("message", message);
            return result;
        }
    }

    static final class Quest {
        final Type type;
        final Enum<?> what;
        // State
        int amount = 16;
        int minReputation = 0;
        final Map<UUID, Integer> progress = new HashMap<>();
        final Map<MessageType, String> messages = new EnumMap<>(MessageType.class);
        State state = State.INIT;

        enum Type {
            MINE(MineWhat.class),
            KILL(KillWhat.class),
            SHEAR(ShearWhat.class),
            BREED(BreedWhat.class);
            final Class<? extends Enum> whatType;
            Type(Class<? extends Enum> whatType) {
                this.whatType = whatType;
            }
        }
        enum State {
            INIT, ENABLED, COMPLETED, RETURNED;
        }
        enum KillWhat {
            CREEPER(EnumSet.of(EntityType.CREEPER)),
            ZOMBIE(EnumSet.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.ZOMBIE_VILLAGER)),
            SPIDER(EnumSet.of(EntityType.SPIDER, EntityType.CAVE_SPIDER)),
            ENDERMAN(EnumSet.of(EntityType.ENDERMAN));
            final Set<EntityType> killEntities;
            KillWhat(Set<EntityType> killEntities) {
                this.killEntities = killEntities;
            }
        }
        enum BreedWhat {
            COW(EnumSet.of(EntityType.COW), Struct.Tag.COW),
            PIG(EnumSet.of(EntityType.PIG), Struct.Tag.PIG),
            SHEEP(EnumSet.of(EntityType.SHEEP), Struct.Tag.SHEEP),
            CHICKEN(EnumSet.of(EntityType.CHICKEN), Struct.Tag.CHICKEN),
            HORSE(EnumSet.of(EntityType.HORSE), Struct.Tag.HORSE),
            DONKEY(EnumSet.of(EntityType.DONKEY), Struct.Tag.DONKEY),
            MULE(EnumSet.of(EntityType.MULE), Struct.Tag.MULE),
            MUSHROOM_COW(EnumSet.of(EntityType.MUSHROOM_COW), Struct.Tag.MUSHROOM_COW);
            final Set<EntityType> breedEntities;
            Struct.Tag breedTag;
            BreedWhat(Set<EntityType> breedEntities, Struct.Tag breedTag) {
                this.breedEntities = breedEntities;
                this.breedTag = breedTag;
            }
            BreedWhat(EntityType breedEntity) {
                this.breedEntities = EnumSet.of(breedEntity);
            }
        }
        enum ShearWhat {
            SHEEP(EnumSet.of(EntityType.SHEEP));
            final Set<EntityType> shearEntities;
            ShearWhat(Set<EntityType> shearEntities) {
                this.shearEntities = shearEntities;
            }
        }
        enum MineWhat {
            DIAMOND(EnumSet.of(Material.DIAMOND_ORE)),
            IRON(EnumSet.of(Material.IRON_ORE)),
            GOLD(EnumSet.of(Material.GOLD_ORE)),
            COAL(EnumSet.of(Material.COAL_ORE));
            final Set<Material> mineMaterials;
            MineWhat(Set<Material> mineMaterials) {
                this.mineMaterials = mineMaterials;
            }
        }
        enum MessageType {
            DESCRIPTION, PROGRESS, SUCCESS, UNWORTHY, EXPIRED;
        }

        Quest(Type type, Enum<?> what) {
            this.type = type;
            this.what = what;
        }

        Quest(ConfigurationSection config) {
            type = Type.valueOf(config.getString("type"));
            what = type.valueOf(type.whatType, config.getString("what"));
            state = State.valueOf(config.getString("state"));
            this.amount = config.getInt("amount");
            ConfigurationSection section = config.getConfigurationSection("progress");
            if (section != null) {
                for (String key: section.getKeys(false)) {
                    progress.put(UUID.fromString(key), section.getInt(key));
                }
            }
            minReputation = config.getInt("min_reputation", 0);
            for (MessageType mt: MessageType.values()) {
                messages.put(mt, config.getString("message_" + mt.name().toLowerCase()));
            }
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", type.name());
            result.put("what", what.name());
            result.put("state", state.name());
            result.put("amount", amount);
            result.put("progress", progress.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue())));
            result.put("min_reputation", minReputation);
            for (MessageType mt: MessageType.values()) {
                if (messages.containsKey(mt)) result.put("message_" + mt.name().toLowerCase(), messages.get(mt));
            }
            return result;
        }

        int getProgress(Player player) {
            Integer result = progress.get(player.getUniqueId());
            if (result == null) return 0;
            return result;
        }

        boolean isSignedUp(Player player) {
            switch (state) {
            case INIT: return false;
            case ENABLED: return progress.containsKey(player.getUniqueId());
            case COMPLETED:
            case RETURNED:
            default:
                return false;
            }
        }
    }

    boolean tryToAddTown() {
        Generator generator = new Generator(plugin);
        int size = 8 + generator.randomInt(8);
        Generator.Town gt = generator.tryToPlantTown(world, size);
        if (gt == null) {
            return false;
        }
        Rectangle area = new Rectangle(gt.ax, gt.ay, gt.bx, gt.by);
        Rectangle questArea = area.grow(64);
        for (Town town: towns) {
            if (town.questArea.intersects(questArea)) {
                return false;
            }
        }
        for (Player player: world.getPlayers()) {
            Location loc = player.getLocation();
            if (questArea.contains(loc.getBlockX(), loc.getBlockZ())) return false;
        }
        List<Fraction> fractions = new ArrayList<>();
        for (Fraction fraction: Fraction.values()) {
            for (int i = 0; i < fraction.chance; i += 1) fractions.add(fraction);
        }
        Fraction fraction = fractions.get(generator.randomInt(fractions.size()));
        Set<Generator.Flag> flags = EnumSet.noneOf(Generator.Flag.class);
        Generator.Flag flagStyle;
        if (fraction == Fraction.NETHER) {
            flagStyle = Generator.Flag.NETHER;
        } else {
            List<Generator.Flag> styleFlags = new ArrayList<>();
            for (Generator.Flag flag: Generator.Flag.values()) {
                if (flag.strategy == Generator.Flag.Strategy.STYLE && !flag.rare) styleFlags.add(flag);
            }
            flagStyle = styleFlags.get(generator.randomInt(styleFlags.size()));
        }
        flags.add(flagStyle);
        flags.add(Generator.Flag.SURFACE);
        int townId = towns.size();
        String townName = generateUniqueName(generator, 1 + generator.randomInt(2));
        Town town = new Town(area, townName, fraction);
        town.tags.addAll(flags.stream().map(f -> f.name().toLowerCase()).collect(Collectors.toList()));
        gt.name = townName;
        // Plant the town
        final String doTileDrops = "doTileDrops";
        String oldGameRuleValue = world.getGameRuleValue(doTileDrops);
        world.setGameRuleValue(doTileDrops, "false");
        generator.setFlags(flags);
        try {
            generator.plantTown(world, gt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        world.setGameRuleValue(doTileDrops, oldGameRuleValue);
        // Fetch generation info
        town.structs.addAll(gt.structs);
        // Quests
        List<Quest> possibleQuests = new ArrayList<>();
        for (Struct struct: town.structs) {
            switch (struct.type) {
            case FARM:
                break;
            case PASTURE:
                for (Struct.Tag tag: struct.tags) {
                    switch (tag) {
                    case COW:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.COW));
                        break;
                    case PIG:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.PIG));
                        break;
                    case SHEEP:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.SHEEP));
                        possibleQuests.add(new Quest(Quest.Type.SHEAR, Quest.ShearWhat.SHEEP));
                        break;
                    case CHICKEN:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.CHICKEN));
                        break;
                    case HORSE:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.HORSE));
                        break;
                    case DONKEY:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.MULE));
                        break;
                    case MUSHROOM_COW:
                        possibleQuests.add(new Quest(Quest.Type.BREED, Quest.BreedWhat.MUSHROOM_COW));
                        break;
                    }
                }
                break;
            default: break;
            }
        }
        for (Quest.KillWhat what: Quest.KillWhat.values()) {
            EnumSet<EntityType> intersection = EnumSet.noneOf(EntityType.class);
            intersection.addAll(what.killEntities);
            intersection.retainAll(town.fraction.villagerTypes);
            if (intersection.isEmpty()) {
                possibleQuests.add(new Quest(Quest.Type.KILL, what));
            }
        }
        for (Quest.MineWhat what: Quest.MineWhat.values()) {
            possibleQuests.add(new Quest(Quest.Type.MINE, what));
        }
        Collections.shuffle(possibleQuests, generator.random);
        // NPCs
        List<Vec3> vecNPCs = new ArrayList<>();
        for (Generator.House house: gt.houses) {
            for (Vec3 vec: house.npcs) {
                vecNPCs.add(vec);
            }
        }
        Collections.shuffle(vecNPCs, generator.random);
        int npcGreetings = 1;
        int npcQuests = Math.min(possibleQuests.size(), vecNPCs.size()/* / 2*/); // TODO
        for (Vec3 vec: vecNPCs) {
            int npcId = town.npcs.size();
            NPC npc = new NPC(vec);
            npc.name = generateUniqueName(generator, 1 + generator.randomInt(2));
            String message;
            if (npcQuests > 0) {
                npcQuests -= 1;
                npc.questId = town.quests.size();
                Quest quest = possibleQuests.remove(possibleQuests.size() - 1);
                enableQuest(quest, town, npc);
                town.quests.add(quest);
                message = plugin.getMessages().deal(Messages.Type.RANDOM);
            } else if (npcGreetings > 0) {
                npcGreetings -= 1;
                message = plugin.getMessages().deal(Messages.Type.GREETING);
            } else {
                message = plugin.getMessages().deal(Messages.Type.RANDOM);
            }
            message = message.replace("%npc%", npc.name);
            message = message.replace("%town%", town.name);
            npc.message = message;
            town.npcs.add(npc);
            EntityType et = fraction.villagerTypes.get(generator.randomInt(fraction.villagerTypes.size()));
            Location loc = world.getBlockAt(vec.x, vec.y, vec.z).getLocation().add(0.5, 0.0, 0.5);
            LivingEntity living = (LivingEntity)world.spawnEntity(loc, et);
            living.setAI(false);
            living.setRemoveWhenFarAway(false);
            living.setCustomName("" + fraction.color + ChatColor.ITALIC + npc.name);
            NPCEntity.Watcher watcher = (NPCEntity.Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(living, NPCEntity.CUSTOM_ID);
            watcher.setIds(townId, npcId);
            watcher.save();
        }
        towns.add(town);
        save();
        plugin.getLogger().info("Town " + town.name + "(" + flagStyle.name().toLowerCase() + "," + fraction.name().toLowerCase() + ") created at " + gt.ax + " " + gt.ay);
        return true;
    }

    void onTick() {
        ticks += 1;
        if (plugin.isCreateTowns()) {
            if (addTownCooldown > 0) {
                addTownCooldown -= 1;
            } else {
                tryToAddTown();
                addTownCooldown = towns.size() * 20;
            }
        }
        for (Player player: world.getPlayers()) {
            Block block = player.getLocation().getBlock();
            Belonging belonging = getBelongingAt(block);
            if (belonging == null || belonging.town == null) continue;
            if (belonging.lay == Belonging.Lay.CENTRAL) {
                if (!belonging.town.visited) {
                    player.sendMessage("Visit " + belonging.town.name);
                    belonging.town.visit();
                }
            }
        }
        if (ticks % (20 * 60) == 0 && dirty) save();
    }

    Town findTown(int townId) {
        if (townId < 0 || townId >= towns.size()) return null;
        return towns.get(townId);
    }

    NPC findNPC(int townId, int npcId) {
        if (townId < 0 || townId >= towns.size()) return null;
        Town town = towns.get(townId);
        if (npcId < 0 || npcId >= town.npcs.size()) return null;
        return town.npcs.get(npcId);
    }

    String onPlayerInteractNPC(Player player, int townId, int npcId) {
        if (townId >= towns.size()) return "Hello World";
        Town town = towns.get(townId);
        if (npcId >= town.npcs.size()) return "Hello World";
        NPC npc = town.npcs.get(npcId);
        if (npc.questId < 0) {
            return npc.message;
        } else {
            Quest quest = town.quests.get(npc.questId);
            String result;
            switch (quest.state) {
            case ENABLED:
                if (quest.isSignedUp(player)) {
                    int progress = quest.getProgress(player);
                    if (progress == 0) {
                        result = quest.messages.get(Quest.MessageType.DESCRIPTION);
                    } else if (progress == quest.amount) {
                        result = quest.messages.get(Quest.MessageType.SUCCESS);
                    } else {
                        result = quest.messages.get(Quest.MessageType.PROGRESS)
                            .replace("%done%", "" + progress)
                            .replace("%todo%", "" + (quest.amount - progress))
                            .replace("%amount%", "" + quest.amount);
                    }
                } else if (quest.minReputation > plugin.getReputations().getReputation(player, town.fraction)) {
                    result = quest.messages.get(Quest.MessageType.UNWORTHY);
                } else {
                    giveProgress(player, quest, 0);
                    dirty = true;
                    result = quest.messages.get(Quest.MessageType.DESCRIPTION);
                }
                break;
            case COMPLETED:
                int progress = quest.getProgress(player);
                if (progress == quest.amount) {
                    result = quest.messages.get(Quest.MessageType.SUCCESS);
                    quest.state = Quest.State.RETURNED;
                    dirty = true;
                    // TODO
                } else {
                    result = quest.messages.get(Quest.MessageType.EXPIRED);
                }
                break;
            case RETURNED: default:
                progress = quest.getProgress(player);
                if (progress == quest.amount) {
                    result = quest.messages.get(Quest.MessageType.SUCCESS);
                } else {
                    result = quest.messages.get(Quest.MessageType.EXPIRED);
                }
            }
            return result;
        }
    }

    enum Fraction {
        VILLAGER(5, Arrays.asList(EntityType.VILLAGER), ChatColor.GREEN),
        SKELETON(5, Arrays.asList(EntityType.SKELETON, EntityType.STRAY), ChatColor.WHITE),
        ZOMBIE(5, Arrays.asList(EntityType.ZOMBIE, EntityType.HUSK), ChatColor.DARK_GREEN),
        ZOMBIE_VILLAGER(3, Arrays.asList(EntityType.ZOMBIE_VILLAGER), ChatColor.DARK_GREEN),
        OCCULT(2, Arrays.asList(EntityType.WITCH, EntityType.EVOKER, EntityType.VINDICATOR), ChatColor.LIGHT_PURPLE),
        NETHER(1, Arrays.asList(EntityType.PIG_ZOMBIE, EntityType.BLAZE, EntityType.WITHER_SKELETON), ChatColor.RED),
        CREEPER(0, Arrays.asList(EntityType.CREEPER), ChatColor.DARK_GREEN);

        public final int chance;
        public final List<EntityType> villagerTypes;
        public final ChatColor color;

        Fraction(int chance, List<EntityType> villagerTypes, ChatColor color) {
            this.chance = chance;
            this.villagerTypes = villagerTypes;
            this.color = color;
        }
    }

    String generateUniqueName(Generator generator, int syllables) {
        String result = null;
        do {
            result = generator.generateName(syllables);
            for (Town town: towns) {
                if (result.equals(town.name)) {
                    result = null;
                } else {
                    for (NPC npc: town.npcs) {
                        if (result.equals(npc.name)) {
                            result = null;
                            break;
                        }
                    }
                }
                if (result == null) break;
            }
        } while (result == null);
        return result;
    }

    static class Belonging {
        Town town;
        Lay lay;
        Struct struct; // top level struct
        final List<Struct> structs = new ArrayList<>();
        final Set<Struct.Tag> tags = EnumSet.noneOf(Struct.Tag.class);

        enum Lay {
            OUTSKIRTS, CENTRAL;
        }

        boolean isCentral() {
            return lay == Lay.CENTRAL;
        }
    }

    Belonging getBelongingAt(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (Town town: towns) {
            if (!town.questArea.contains(x, z)) continue;
            Belonging result = new Belonging();
            result.town = town;
            if (town.area.contains(x, z)) {
                result.lay = Belonging.Lay.CENTRAL;
                for (Struct struct: town.structs) {
                    if (struct.boundingBox.contains(x, y, z)) {
                        result.struct = struct;
                        result.structs.add(struct);
                        result.tags.addAll(struct.tags);
                        for (Struct sub: struct.deepSubs()) {
                            if (sub.boundingBox.contains(x, y, z)) {
                                result.structs.add(sub);
                                result.tags.addAll(sub.tags);
                            }
                        }
                        break;
                    }
                }
            } else {
                result.lay = Belonging.Lay.OUTSKIRTS;
            }
            return result;
        }
        return null;
    }

    Vec2 updateDeliveryItem(ItemStack item, Player player) {
        TagWrapper config = TagWrapper.getItemConfigOf(item);
        NPC senderNPC = null;
        Vec2 senderVec = config.isSet(DeliveryItem.KEY_RECIPIENT) ? new Vec2(config.getIntList(DeliveryItem.KEY_RECIPIENT)) : null;
        senderNPC = senderVec != null ? findNPC(senderVec.x, senderVec.y) : null;
        Set<Vec2> usedNPCs = new HashSet<>();
        List<Integer> newUsed = new ArrayList<>();
        for (Iterator<Integer> iter = config.getIntList(DeliveryItem.KEY_USED).iterator(); iter.hasNext();) {
            int x = iter.next();
            if (iter.hasNext()) {
                int y = iter.next();
                usedNPCs.add(new Vec2(x, y));
                newUsed.add(x);
                newUsed.add(y);
            }
        }
        if (senderNPC != null) {
            usedNPCs.add(senderVec);
            newUsed.add(senderVec.x);
            newUsed.add(senderVec.y);
        }
        List<Vec2> npcs = new ArrayList<>();
        for (int i = 0; i < towns.size(); i += 1) {
            if (senderVec != null && senderVec.x == i) continue;
            int townc = towns.get(i).npcs.size();
            for (int j = 0; j < townc; j += 1) {
                Vec2 vec = new Vec2(i, j);
                if (!usedNPCs.contains(vec)) npcs.add(vec);
            }
        }
        if (npcs.size() < 2) {
            return null;
        }
        Collections.shuffle(npcs, plugin.getRandom());
        if (senderNPC == null) {
            senderVec = npcs.get(1);
            senderNPC = findNPC(senderVec.x, senderVec.y);
            newUsed.add(senderVec.x);
            newUsed.add(senderVec.y);
        }
        String senderName = senderNPC.name;
        Vec2 recipientVec = npcs.get(0);
        Town town = towns.get(recipientVec.x);
        NPC npc = town.npcs.get(recipientVec.y);
        config.setIntList(DeliveryItem.KEY_USED, newUsed);
        config.setString(DeliveryItem.KEY_OWNER, player.getUniqueId().toString());
        config.setLong(DeliveryItem.KEY_TIMESTAMP, timestamp);
        config.setIntList(DeliveryItem.KEY_SENDER, senderVec.serialize());
        config.setIntList(DeliveryItem.KEY_RECIPIENT, recipientVec.serialize());
        BookMeta meta = (BookMeta)item.getItemMeta();
        meta.setTitle("Deliver to " + npc.name + " in " + town.name + ".");
        meta.setPages("Dear "
                      + player.getName() + ",\n\nplease "
                      + plugin.getMessages().deal(Messages.Type.SYNONYM_DELIVER) + " this "
                      + plugin.getMessages().deal(Messages.Type.SYNONYM_DELIVERY) + " to my "
                      + plugin.getMessages().deal(Messages.Type.DISTANT_RELATIONSHIP) + " "
                      + npc.name + " who "
                      + plugin.getMessages().deal(Messages.Type.SYNONYM_LIVES_IN) + " "
                      + plugin.getMessages().deal(Messages.Type.SYNONYM_A_PLACE_CALLED) + " "
                      + town.name + ".\n\n"
                      + plugin.getMessages().deal(Messages.Type.SYNONYM_SINCERELY) + ", "
                      + senderName + ".",
                      "If you do not know where to find " + npc.name + ", check your Mini Map and follow the white dots.\n\nTo hand this item over, left-click the recipient with it.");
        meta.setAuthor(senderName);
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        item.setItemMeta(meta);
        return recipientVec;
    }

    void giveProgress(Player player, Quest quest, int prog) {
        Integer score = quest.progress.get(player.getUniqueId());
        if (score == null) score = 0;
        score += prog;
        if (score > quest.amount) score = quest.amount;
        quest.progress.put(player.getUniqueId(), score);
        if (score == quest.amount) quest.state = Quest.State.COMPLETED;
        dirty = true;
    }

    void enableQuest(Quest quest, Town town, NPC npc) {
        quest.messages.put(Quest.MessageType.EXPIRED, plugin.getMessages().deal(Messages.Type.QUEST_EXPIRED));
        quest.messages.put(Quest.MessageType.UNWORTHY, plugin.getMessages().deal(Messages.Type.QUEST_UNWORTHY));
        switch (quest.type) {
        case MINE:
            String gemstone;
            String fine = plugin.getMessages().deal(Messages.Type.SYNONYM_FINE_ITEM);
            String ore = quest.what.name().toLowerCase();
            String legendary = plugin.getMessages().deal(Messages.Type.SYNONYM_LEGENDARY_ITEM);
            switch (plugin.getRandom().nextInt(10)) {
            case 0: gemstone = Msg.capitalize(fine) + " " + Msg.capitalize(ore) + " of " + town.name; break;
            case 1: gemstone = Msg.capitalize(fine) + " " + town.name + " " + Msg.capitalize(ore); break;
            case 2: default: gemstone = town.name + " " + fine + " " + Msg.capitalize(ore);
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_MINE));
            quest.messages.put(Quest.MessageType.PROGRESS, quest.messages.get(Quest.MessageType.DESCRIPTION));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_MINE_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%legendary%", legendary)
                                   .replace("%gem%", gemstone)
                                   .replace("%ore%", ore));
            }
            break;
        case KILL:
            String pet = plugin.getMessages().deal(Messages.Type.QUEST_KILL_STOLEN_BABY_PETS);
            String singular = Msg.capitalize(((Quest.KillWhat)quest.what).name());
            String plural = quest.what == Quest.KillWhat.ENDERMAN ? "Endermen" : singular + "s";
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_KILL));
            quest.messages.put(Quest.MessageType.PROGRESS, quest.messages.get(Quest.MessageType.DESCRIPTION));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_KILL_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%pet%", pet)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural));
            }
            break;
        case SHEAR:
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_SHEAR));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_SHEAR_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_SHEAR_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%amount%", "" + quest.amount));
            }
            break;
        case BREED:
            singular = ((Quest.BreedWhat)quest.what).name().toLowerCase().replace("_", " ");
            switch ((Quest.BreedWhat)quest.what) {
            case SHEEP: plural = "sheep"; break;
            default: plural = singular + "s";
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_BREED));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_BREED_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_BREED_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural)
                                   .replace("%amount%", "" + quest.amount));
            }
            break;
        }
        for (Quest.MessageType mt: Quest.MessageType.values()) {
            quest.messages.put(mt, quest.messages.get(mt)
                               .replace("%town%", town.name)
                               .replace("%npc%", npc.name));
        }
        quest.state = Quest.State.ENABLED;
        dirty = true;
    }
}
