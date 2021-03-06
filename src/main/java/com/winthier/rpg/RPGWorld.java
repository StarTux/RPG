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
import java.util.IdentityHashMap;
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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MapCursor;

@Getter
final class RPGWorld {
    final RPGPlugin plugin;
    final World world;
    final List<Town> towns = new ArrayList<>();
    final Set<UUID> deliveries = new HashSet<>();
    boolean dirty = false;
    int addTownCooldown = 0;
    int ticks;
    long timestamp;
    private final Object LOCK = new Object();

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
        deliveries.addAll(config.getStringList("deliveries").stream().map(s -> UUID.fromString(s)).collect(Collectors.toList()));
        timestamp = config.getLong("timestamp", timestamp);
    }

    void save() {
        writeToDisk(false);
    }

    void saveImmediately() {
        writeToDisk(true);
    }

    private void writeToDisk(boolean immediately) {
        dirty = false;
        YamlConfiguration config = new YamlConfiguration();
        config.set("towns", towns.stream().map(t -> t.serialize()).collect(Collectors.toList()));
        config.set("deliveries", deliveries.stream().map(u -> u.toString()).collect(Collectors.toList()));
        config.set("timestamp", timestamp);
        if (immediately) {
            writeToDisk(config);
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeToDisk(config));
        }
    }

    private void writeToDisk(YamlConfiguration config) {
        synchronized(LOCK) {
            try {
                config.save(new File(world.getWorldFolder(), "winthier.rpg.yml"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    Town findTown(String name) {
        for (Town town: towns) if (name.equals(town.name)) return town;
        return null;
    }

    final class Town {
        final Rectangle area;
        final Rectangle questArea;
        final Rectangle exclusiveArea;
        final List<NPC> npcs = new ArrayList<>();
        final List<Quest> quests = new ArrayList<>();
        final List<String> tags = new ArrayList<>();
        final List<Struct> structs = new ArrayList<>();
        final String name;
        final Fraction fraction;
        boolean visited = false;
        transient int defenderCooldown = 0;

        Town(Rectangle area, String name, Fraction fraction) {
            this.area = area;
            this.questArea = area.grow(96);
            this.exclusiveArea = area.grow(255);
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
            this.questArea = area.grow(96);
            this.exclusiveArea = area.grow(255);
            this.name = config.getString("name");
            this.fraction = Fraction.valueOf(config.getString("fraction"));
            this.tags.addAll(config.getStringList("tags"));
            this.visited = config.getBoolean("visited");
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", name);
            result.put("fraction", fraction.name());
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
        UUID entityUuid;

        NPC(Vec3 home) {
            this.home = home;
        }

        NPC(ConfigurationSection config) {
            this.home = new Vec3(config.getIntegerList("home"));
            this.message = config.getString("message", "");
            this.name = config.getString("name", "");
            this.questId = config.getInt("quest_id", -1);
            if (config.isSet("entity_uuid")) this.entityUuid = UUID.fromString(config.getString("entity_uuid"));
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", name);
            result.put("home", home.serialize());
            result.put("quest_id", questId);
            result.put("message", message);
            if (entityUuid != null) result.put("entity_uuid", entityUuid.toString());
            return result;
        }

        NPCEntity.Watcher findEntityWatcher() {
            if (entityUuid == null) return null;
            return (NPCEntity.Watcher)CustomPlugin.getInstance().getEntityManager().getEntityWatcher(entityUuid);
        }
    }

    static final class Quest {
        final Type type;
        final Struct.Tag what;
        // State
        int amount = 16;
        int reputation = 10;
        int minReputation = 0;
        final Map<UUID, Integer> progress = new HashMap<>();
        final Map<MessageType, String> messages = new EnumMap<>(MessageType.class);
        State state = State.INIT;
        boolean unlocksNext;
        String tokenName;

        enum Type {
            MINE, FIND_GEM, KILL, SHEAR, BREED, TAME, HARVEST, FIND_LAIR;
        }
        enum State {
            INIT, ENABLED, COMPLETED, RETURNED;
        }
        enum MessageType {
            DESCRIPTION, PROGRESS, SUCCESS, UNWORTHY, EXPIRED;
        }

        Quest(Type type, Struct.Tag what) {
            this.type = type;
            this.what = what;
        }

        Quest(ConfigurationSection config) {
            type = Type.valueOf(config.getString("type"));
            what = Struct.Tag.valueOf(config.getString("what"));
            state = State.valueOf(config.getString("state"));
            amount = config.getInt("amount");
            reputation = config.getInt("reputation");
            minReputation = config.getInt("min_reputation", 0);
            unlocksNext = config.getBoolean("unlocks_next");
            ConfigurationSection section = config.getConfigurationSection("progress");
            if (section != null) {
                for (String key: section.getKeys(false)) {
                    progress.put(UUID.fromString(key), section.getInt(key));
                }
            }
            for (MessageType mt: MessageType.values()) {
                messages.put(mt, config.getString("message_" + mt.name().toLowerCase()));
            }
            if (config.isSet("token_name")) tokenName = config.getString("token_name");
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", type.name());
            result.put("what", what.name());
            result.put("state", state.name());
            result.put("amount", amount);
            result.put("reputation", reputation);
            result.put("min_reputation", minReputation);
            result.put("progress", progress.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue())));
            if (unlocksNext) result.put("unlocks_next", unlocksNext);
            if (tokenName != null) result.put("token_name", tokenName);
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

        boolean isActiveFor(Player player) {
            switch (state) {
            case INIT: return false;
            case ENABLED: return progress.containsKey(player.getUniqueId());
            case COMPLETED: return getProgress(player) == amount;
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
        Rectangle exclusiveArea = area.grow(512);
        for (Town town: towns) {
            if (town.exclusiveArea.intersects(exclusiveArea)) {
                return false;
            }
        }
        for (Player player: world.getPlayers()) {
            Location loc = player.getLocation();
            if (area.contains(loc.getBlockX(), loc.getBlockZ())) return false;
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
        gt.fraction = fraction;
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
        List<List<Quest>> quests = new ArrayList<>();
        for (Struct struct: town.structs) {
            switch (struct.type) {
            case FARM:
                for (Struct.Tag tag: struct.tags) {
                    if (tag.tile != null) {
                        quests.add(Arrays.asList(new Quest(Quest.Type.HARVEST, tag)));
                    }
                }
                break;
            case PASTURE:
                for (Struct.Tag tag: struct.tags) {
                    switch (tag) {
                    case SHEEP:
                        quests.add(Arrays.asList(new Quest(Quest.Type.BREED, tag),
                                                 new Quest(Quest.Type.SHEAR, tag)));
                        break;
                    case HORSE:
                    case DONKEY:
                        quests.add(Arrays.asList(new Quest(Quest.Type.TAME, tag),
                                                 new Quest(Quest.Type.BREED, tag)));
                        break;
                    case COW:
                    case PIG:
                    case CHICKEN:
                    case MUSHROOM_COW:
                        quests.add(Arrays.asList(new Quest(Quest.Type.BREED, tag)));
                        break;
                    default: break;
                    }
                }
                break;
            case LAIR:
                for (Struct.Tag tag: struct.tags) {
                    if (tag.entityType != null) {
                        quests.add(Arrays.asList(new Quest(Quest.Type.KILL, tag),
                                                 new Quest(Quest.Type.FIND_LAIR, tag)));
                    }
                }
                break;
            case MINE:
                quests.add(Arrays.asList(new Quest(Quest.Type.MINE, Struct.Tag.DIAMOND)));
                break;
            default: break;
            }
        }
        Collections.shuffle(quests, generator.random);
        // NPCs
        List<Vec3> vecNPCs = new ArrayList<>();
        for (Generator.House house: gt.houses) {
            for (Vec3 vec: house.npcs) {
                vecNPCs.add(vec);
            }
        }
        Collections.shuffle(vecNPCs, generator.random);
        int npcGreetings = 1;
        int npcQuests = Math.min(quests.size(), vecNPCs.size() / 3);
        for (Vec3 vec: vecNPCs) {
            int npcId = town.npcs.size();
            NPC npc = new NPC(vec);
            npc.name = generateUniqueName(generator, 1 + generator.randomInt(2));
            String message;
            if (npcQuests > 0) {
                npcQuests -= 1;
                npc.questId = town.quests.size();
                List<Quest> qs = quests.remove(quests.size() - 1);
                for (int i = 0; i < qs.size() - 1; i += 1) qs.get(i).unlocksNext = true;
                for (int i = 0; i < qs.size(); i += 1) {
                    qs.get(i).reputation += i * 5;
                }
                for (Quest quest: qs) {
                    enableQuest(quest, town, npc);
                    town.quests.add(quest);
                }
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
            npc.entityUuid = living.getUniqueId();
            living.setAI(false);
            living.setRemoveWhenFarAway(false);
            living.setCustomName("" + fraction.color + ChatColor.ITALIC + npc.name);
            NPCEntity.Watcher watcher = (NPCEntity.Watcher)CustomPlugin.getInstance().getEntityManager().wrapEntity(living, NPCEntity.CUSTOM_ID);
            watcher.setIds(townId, npcId);
            watcher.save();
        }
        towns.add(town);
        plugin.getLogger().info("Town " + town.name + "(" + flagStyle.name().toLowerCase() + "," + fraction.name().toLowerCase() + ") created at " + gt.ax + " " + gt.ay);
        dirty = true;
        return true;
    }

    void onTick() {
        ticks += 1;
        if (plugin.isCreateTowns()) {
            if (addTownCooldown > 0) {
                addTownCooldown -= 1;
            } else {
                tryToAddTown();
                addTownCooldown = towns.size() * 2;
            }
        }
        Map<Town, List<Player>> townPlayers = new IdentityHashMap<>();
        for (Player player: world.getPlayers()) {
            Block block = player.getLocation().getBlock();
            Belonging belonging = getBelongingAt(block);
            if (belonging != null && belonging.town != null) {
                List<Player> players = townPlayers.get(belonging.town);
                if (players == null) {
                    players = new ArrayList<>();
                    townPlayers.put(belonging.town, players);
                }
                players.add(player);
                if (belonging.lay == Belonging.Lay.CENTRAL) {
                    if (!belonging.town.visited) {
                        belonging.town.visit();
                    }
                }
            }
        }
        for (Town town: townPlayers.keySet()) {
            tickTown(town, townPlayers.get(town));
        }
        if (ticks % (20 * 60) == 0 && dirty) save();
    }

    private boolean flipFlop;
    void tickTown(Town town, List<Player> players) {
        int townId = towns.indexOf(town);
        if (plugin.isUpdateMiniMapCursors()) {
            flipFlop = !flipFlop;
            for (Player player: players) {
                for (NPC npc: town.npcs) {
                    if (npc.questId >= 0) {
                        Quest quest = findQuest(townId, npc.questId);
                        if (quest != null
                            && quest.isActiveFor(player)) {
                            if (quest.getProgress(player) == quest.amount && flipFlop) continue;
                            NPCEntity.Watcher watcher = npc.findEntityWatcher();
                            Block block;
                            if (watcher != null) {
                                block = watcher.getEntity().getLocation().getBlock();
                            } else {
                                block = world.getBlockAt(npc.home.x, npc.home.y, npc.home.z);
                            }
                            Map map = new HashMap();
                            map.put("block", block);
                            map.put("type", MapCursor.Type.SMALL_WHITE_CIRCLE);
                            plugin.getMiniMapCursors(player).add(map);
                        }
                    }
                }
            }
        }
        if (town.defenderCooldown > 0) {
            town.defenderCooldown -= 1;
        } else {
            for (Player player: players) {
                if (!plugin.getAllowedGameModes().contains(player.getGameMode())) continue;
                Block pb = player.getLocation().getBlock();
                int rep = plugin.getReputations().getReputation(player, town.fraction);
                if (rep < 0 && town.area.contains(pb.getX(), pb.getZ()) && pb.getLightFromSky() > 0) {
                    int defenderCount = 0;
                    for (Entity nearby: player.getNearbyEntities(16, 16, 16)) {
                        if (nearby.getScoreboardTags().contains("Winthier.RPG.Defender")) defenderCount += 1;
                    }
                    if (defenderCount >= Math.abs(rep)) continue;
                    EntityType et;
                    if (town.fraction == Fraction.VILLAGER) {
                        et = EntityType.IRON_GOLEM;
                    } else {
                        et = town.fraction.villagerTypes.get(plugin.getRandom().nextInt(town.fraction.villagerTypes.size()));
                    }
                    final int rad = 8;
                    int dx, dz;
                    if (plugin.getRandom().nextBoolean()) {
                        dx = plugin.getRandom().nextBoolean() ? rad : -rad;
                        dz = plugin.getRandom().nextInt(rad * 2) - rad;
                    } else {
                        dx = plugin.getRandom().nextInt(rad * 2) - rad;
                        dz = plugin.getRandom().nextBoolean() ? rad : -rad;
                    }
                    Location loc = world.getHighestBlockAt(pb.getX() + dx, pb.getZ() + dz).getLocation().add(0.5, 0.5, 0.5);
                    LivingEntity defender = (LivingEntity)world.spawnEntity(loc, et);
                    defender.addScoreboardTag("Winthier.RPG.Defender");
                    defender.setRemoveWhenFarAway(true);
                    defender.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0);
                    defender.setHealth(100.0);
                    defender.setCustomName("" + ChatColor.RESET + town.fraction.color + town.name + " Defender");
                    if (defender instanceof Creature) ((Creature)defender).setTarget(player);
                    switch (et) {
                    case SKELETON:
                    case ZOMBIE:
                    case HUSK:
                    case STRAY:
                        ItemStack helmet = defender.getEquipment().getHelmet();
                        if (helmet == null || helmet.getType() == Material.AIR) {
                            defender.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                        }
                        break;
                    default: break;
                    }
                }
            }
            town.defenderCooldown = 20;
        }
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

    Quest findQuest(int townId, int questId) {
        if (townId < 0 || townId >= towns.size()) return null;
        Town town = towns.get(townId);
        if (questId < 0 || questId >= town.quests.size()) return null;
        return town.quests.get(questId);
    }

    String onPlayerInteractNPC(Player player, NPCEntity.Watcher entity, int townId, int npcId) {
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
                    if (progress == quest.amount) {
                        result = quest.messages.get(Quest.MessageType.SUCCESS);
                    } else if (progress < 2) {
                        result = quest.messages.get(Quest.MessageType.DESCRIPTION);
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
                    String msg;
                    String what = Msg.capitalize(quest.what.name().replace("_", " "));
                    switch (quest.type) {
                    case MINE: msg = "Mine " + quest.amount + " " + what; break;
                    case FIND_GEM: msg = "Find the " + quest.tokenName; break;
                    case KILL: msg = "Kill " + quest.amount + " " + what; break;
                    case SHEAR: msg = "Shear " + quest.amount + " " + what; break;
                    case BREED: msg = "Breed " + quest.amount + " " + what; break;
                    case TAME: msg = "Tame " + quest.amount + " " + what; break;
                    case HARVEST: msg = "Harvest " + quest.amount + " " + what; break;
                    case FIND_LAIR: msg = "Find the " + what + " lair"; break;
                    default: msg = null; break;
                    }
                    if (msg != null) Msg.sendActionBar(player, "&aQuest Received: " + msg);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_XYLOPHONE, 0.5f, 0.5f), 0);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_XYLOPHONE, 0.5f, 0.6f), 3);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_XYLOPHONE, 0.5f, 0.8f), 6);
                }
                break;
            case COMPLETED:
                int progress = quest.getProgress(player);
                if (progress == quest.amount) {
                    if (quest.tokenName != null) {
                        result = quest.messages.get(Quest.MessageType.DESCRIPTION);
                    } else {
                        result = quest.messages.get(Quest.MessageType.SUCCESS);
                        quest.state = Quest.State.RETURNED;
                        plugin.getReputations().giveReputation(player, town.fraction, 10);
                        player.spawnParticle(Particle.HEART, entity.getEntity().getEyeLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                        player.playSound(entity.getEntity().getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 1.75f);
                        if (quest.unlocksNext) npc.questId += 1;
                        dirty = true;
                    }
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

    String generateUniqueName(Generator generator, int syllables) {
        String result = null;
        do {
            result = generator.generateName(syllables);
            String cleaned = generator.cleanSpecialChars(result);
            final String[] forbiddenWords = {"nigger", "nigga", "nygger", "nygga", "penis", "penys", "dick", "fuck", "sperm"};
            for (String forbiddenWord: forbiddenWords) {
                if (cleaned.contains(forbiddenWord)) {
                    continue;
                }
            }
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
        final Set<Struct.Type> types = EnumSet.noneOf(Struct.Type.class);
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
            } else {
                result.lay = Belonging.Lay.OUTSKIRTS;
            }
            for (Struct struct: town.structs) {
                if (struct.boundingBox.contains(x, y, z)) {
                    result.struct = struct;
                    result.tags.addAll(struct.tags);
                    result.types.add(struct.type);
                    for (Struct sub: struct.deepSubs()) {
                        if (sub.boundingBox.contains(x, y, z)) {
                            result.structs.add(sub);
                            result.tags.addAll(sub.tags);
                            result.types.add(sub.type);
                        }
                    }
                    break;
                }
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
                      "If you do not know where to find " + npc.name + ", check your Mini Map and follow the white dots.\n\nThe canon travel station at Spawn will be happy to shoot you to " + town.name + ".",
                      "To hand this item over, left-click the recipient with it.");
        meta.setAuthor(senderName);
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        item.setItemMeta(meta);
        return recipientVec;
    }

    int giveProgress(Player player, Quest quest, int prog) {
        Integer score = quest.progress.get(player.getUniqueId());
        if (score == null) score = 0;
        score += prog;
        if (score > quest.amount) score = quest.amount;
        quest.progress.put(player.getUniqueId(), score);
        if (score == quest.amount) quest.state = Quest.State.COMPLETED;
        dirty = true;
        return score;
    }

    void enableQuest(Quest quest, Town town, NPC npc) {
        quest.messages.put(Quest.MessageType.EXPIRED, plugin.getMessages().deal(Messages.Type.QUEST_EXPIRED));
        quest.messages.put(Quest.MessageType.UNWORTHY, plugin.getMessages().deal(Messages.Type.QUEST_UNWORTHY));
        switch (quest.type) {
        case MINE:
            String ore = quest.what.name().toLowerCase();
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_MINE));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_MINE_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_MINE_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%ore%", ore));
            }
            quest.amount = 15 + plugin.getRandom().nextInt(16);
            break;
        case FIND_GEM:
            String gemstone;
            String fine = Msg.capitalize(plugin.getMessages().deal(Messages.Type.SYNONYM_FINE_ITEM));
            ore = quest.what.name().toLowerCase();
            String legendary = plugin.getMessages().deal(Messages.Type.SYNONYM_LEGENDARY_ITEM);
            switch (plugin.getRandom().nextInt(10)) {
            case 0: gemstone = fine + " " + Msg.capitalize(ore) + " of " + town.name; break;
            case 1: gemstone = fine + " " + town.name + " " + Msg.capitalize(ore); break;
            case 2: default: gemstone = town.name + " " + fine + " " + Msg.capitalize(ore);
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_FIND_GEM));
            quest.messages.put(Quest.MessageType.PROGRESS, quest.messages.get(Quest.MessageType.DESCRIPTION));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_FIND_GEM_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%legendary%", legendary)
                                   .replace("%gem%", gemstone)
                                   .replace("%ore%", ore));
            }
            quest.amount = 20 + plugin.getRandom().nextInt(21);
            quest.tokenName = gemstone;
            break;
        case KILL:
            String singular = quest.what.name().toLowerCase().replace("_", " ");
            String plural;
            switch (quest.what) {
            case ENDERMAN: plural = "Endermen"; break;
            default: plural = singular + "s";
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_KILL));
            quest.messages.put(Quest.MessageType.PROGRESS, quest.messages.get(Quest.MessageType.DESCRIPTION));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_KILL_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural));
            }
            quest.amount = 10 + plugin.getRandom().nextInt(11);
            quest.minReputation += 10;
            break;
        case FIND_LAIR:
            singular = quest.what.name().toLowerCase().replace("_", " ");
            switch (quest.what) {
            case ENDERMAN: plural = "Endermen"; break;
            default: plural = singular + "s";
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_FIND_LAIR));
            quest.messages.put(Quest.MessageType.PROGRESS, quest.messages.get(Quest.MessageType.DESCRIPTION));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_FIND_LAIR_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural));
            }
            quest.amount = 2;
            quest.minReputation += 10;
            break;
        case SHEAR:
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_SHEAR));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_SHEAR_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_SHEAR_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt));
            }
            quest.amount = 10 + plugin.getRandom().nextInt(6);
            break;
        case BREED:
            singular = quest.what.name().toLowerCase().replace("_", " ");
            switch (quest.what) {
            case SHEEP: plural = "sheep"; break;
            default: plural = singular + "s";
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_BREED));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_BREED_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_BREED_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural));
            }
            quest.amount = 5 + plugin.getRandom().nextInt(6);
            break;
        case TAME:
            singular = quest.what.name().toLowerCase().replace("_", " ");
            plural = singular + "s";
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_TAME));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_TAME_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_TAME_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural));
            }
            quest.amount = 2 + plugin.getRandom().nextInt(3);
            break;
        case HARVEST:
            singular = quest.what.name().toLowerCase().replace("_", " ");
            switch (quest.what) {
            case POTATO: plural = "potatoes"; break;
            default: plural = singular + "s";
            }
            quest.messages.put(Quest.MessageType.DESCRIPTION, plugin.getMessages().deal(Messages.Type.QUEST_HARVEST));
            quest.messages.put(Quest.MessageType.PROGRESS, plugin.getMessages().deal(Messages.Type.QUEST_HARVEST_PROGRESS));
            quest.messages.put(Quest.MessageType.SUCCESS, plugin.getMessages().deal(Messages.Type.QUEST_HARVEST_SUCCESS));
            for (Quest.MessageType mt: Quest.MessageType.values()) {
                quest.messages.put(mt, quest.messages.get(mt)
                                   .replace("%singular%", singular)
                                   .replace("%plural%", plural));
            }
            quest.amount = 64 + plugin.getRandom().nextInt(65);
            break;
        }
        for (Quest.MessageType mt: Quest.MessageType.values()) {
            if (quest.messages.get(mt) == null) {
                plugin.getLogger().warning("Quest " + quest.type + " has no message " + mt);
                quest.messages.put(mt, "Hello");
            }
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
