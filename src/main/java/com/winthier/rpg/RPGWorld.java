package com.winthier.rpg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@Getter
final class RPGWorld {
    final RPGPlugin plugin;
    final World world;
    final List<Town> towns = new ArrayList<>();
    boolean dirty = false;
    int addTownCooldown = 0;
    int ticks;

    RPGWorld(RPGPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "winthier.rpg.yml"));
        for (Map<?, ?> map: config.getMapList("towns")) {
            ConfigurationSection section = config.createSection("tmp", map);
            towns.add(new Town(section));
        }
    }

    void save() {
        dirty = false;
        YamlConfiguration config = new YamlConfiguration();
        config.set("towns", towns.stream().map(t -> t.serialize()).collect(Collectors.toList()));
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

    static final class House {
        final Cuboid boundingBox;
        final List<Cuboid> rooms = new ArrayList<>();

        House(Cuboid boundingBox, List<Cuboid> rooms) {
            this.boundingBox = boundingBox;
            this.rooms.addAll(rooms);
        }

        House(ConfigurationSection config) {
            boundingBox = new Cuboid(config.getIntegerList("bounding_box"));
            for (Object o: config.getList("rooms")) {
                rooms.add(new Cuboid((List<Integer>)o));
            }
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("bounding_box", boundingBox.serialize());
            result.put("rooms", rooms.stream().map(r -> r.serialize()).collect(Collectors.toList()));
            return result;
        }
    }

    static final class Town {
        final Rectangle area;
        final Rectangle questArea;
        final List<NPC> npcs = new ArrayList<>();
        final List<Quest> quests = new ArrayList<>();
        final List<String> tags = new ArrayList<>();
        final List<House> houses = new ArrayList<>();
        final String name;
        Generator.Flag fraction = Generator.Flag.VILLAGER;
        boolean visited = false;

        Town(Rectangle area, String name) {
            this.area = area;
            this.questArea = area.grow(128);
            this.name = name;
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
            for (Map<?, ?> map: config.getMapList("houses")) {
                ConfigurationSection section = config.createSection("tmp", map);
                this.houses.add(new House(section));
            }
            this.area = new Rectangle(config.getIntegerList("area"));
            this.questArea = area.grow(128);
            this.name = config.getString("name");
            try {
                this.fraction = Generator.Flag.valueOf(config.getString("fraction", "VILLAGER").toUpperCase());
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
            this.tags.addAll(config.getStringList("tags"));
            this.visited = config.getBoolean("visited");
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("npcs", npcs.stream().map(npc -> npc.serialize()).collect(Collectors.toList()));
            result.put("quests", quests.stream().map(quest -> quest.serialize()).collect(Collectors.toList()));
            result.put("houses", houses.stream().map(house -> house.serialize()).collect(Collectors.toList()));
            result.put("area", area.serialize());
            result.put("name", name);
            result.put("tags", tags);
            result.put("fraction", fraction.name().toLowerCase());
            result.put("visited", visited);
            return result;
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
            Map<String, Object> result = new HashMap<>();
            result.put("home", home.serialize());
            result.put("message", message);
            result.put("name", name);
            result.put("quest_id", questId);
            return result;
        }
    }

    static final class Quest {
        enum Type {
            NONE, KILL, SHEAR, BREAK, FISH;
        }

        final Type type;
        final Map<String, String> settings = new HashMap<>();
        final Map<UUID, Integer> progress = new HashMap<>();
        final int amount;
        int minReputation;
        transient Material material;
        transient EntityType entityType;
        transient int data;

        Quest(Type type, int amount, Map<String, String> settings) {
            this.type = type;
            this.amount = amount;
            for (String key: settings.keySet()) this.settings.put(key, settings.get(key));
        }

        Quest(ConfigurationSection config) {
            Type type;
            try {
                type = Type.valueOf(config.getString("type"));
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
                type = Type.NONE;
            }
            this.type = type;
            this.amount = config.getInt("amount");
            ConfigurationSection section = config.getConfigurationSection("settings");
            if (section != null) {
                for (String key: section.getKeys(false)) {
                    settings.put(key, section.getString(key));
                }
            }
            section = config.getConfigurationSection("progress");
            if (section != null) {
                for (String key: section.getKeys(false)) {
                    progress.put(UUID.fromString(key), section.getInt(key));
                }
            }
            minReputation = config.getInt("min_reputation", 0);
        }

        Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("type", type.name());
            result.put("amount", amount);
            result.put("settings", settings.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
            result.put("progress", progress.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue())));
            result.put("min_reputation", minReputation);
            return result;
        }

        int getProgress(Player player) {
            Integer result = progress.get(player.getUniqueId());
            if (result == null) return 0;
            return result;
        }

        boolean isSignedUp(Player player) {
            return progress.containsKey(player.getUniqueId());
        }

        void signUp(Player player) {
            giveProgress(player, 0);
        }

        void giveProgress(Player player, int prog) {
            Integer score = progress.get(player.getUniqueId());
            if (score == null) score = 0;
            score += prog;
            if (score > this.amount) score = this.amount;
            progress.put(player.getUniqueId(), score);
        }

        String getQuestDescription() {
            return type.name();
        }

        String getProgressReport(int progress) {
            return "" + progress + " out of " + amount;
        }
    }

    boolean tryToAddTown() {
        Generator generator = new Generator();
        int size = 8 + generator.random.nextInt(12);
        Generator.Town gt = generator.tryToPlantTown(world, size);
        if (gt == null) {
            return false;
        }
        Rectangle area = new Rectangle(gt.ax, gt.ay, gt.bx, gt.by);
        Rectangle questArea = area.grow(128);
        for (Town town: towns) {
            if (town.questArea.intersects(questArea)) {
                return false;
            }
        }
        for (Player player: world.getPlayers()) {
            Location loc = player.getLocation();
            if (questArea.contains(loc.getBlockX(), loc.getBlockZ())) return false;
        }
        Set<Generator.Flag> flags = EnumSet.noneOf(Generator.Flag.class);
        List<Generator.Flag> styleFlags = new ArrayList<>();
        List<Generator.Flag> fractionFlags = new ArrayList<>();
        for (Generator.Flag flag: Generator.Flag.values()) {
            if (flag.strategy == Generator.Flag.Strategy.STYLE) styleFlags.add(flag);
            if (flag.strategy == Generator.Flag.Strategy.FRACTION) fractionFlags.add(flag);
        }
        Generator.Flag flagStyle = styleFlags.get(generator.random.nextInt(styleFlags.size()));
        Generator.Flag flagFraction = fractionFlags.get(generator.random.nextInt(fractionFlags.size()));
        flags.add(flagStyle);
        flags.add(flagFraction);
        flags.add(Generator.Flag.SURFACE);
        gt.townId = towns.size();
        final String doTileDrops = "doTileDrops";
        String oldGameRuleValue = world.getGameRuleValue(doTileDrops);
        world.setGameRuleValue(doTileDrops, "false");
        generator.plantTown(world, gt, flags);
        world.setGameRuleValue(doTileDrops, oldGameRuleValue);
        Town town = new Town(area, generator.generateTownName());
        town.tags.addAll(flags.stream().map(f -> f.name().toLowerCase()).collect(Collectors.toList()));
        for (Generator.House gh: gt.houses) {
            town.houses.add(new House(gh.boundingBox, gh.rooms.stream().map(r -> r.boundingBox).collect(Collectors.toList())));
        }
        town.fraction = flagFraction;
        // Quests
        int totalQuests = Math.min(town.npcs.size(), 3 + generator.random.nextInt(3));
        for (int i = 0; i < totalQuests; i += 1) {
            Quest.Type questType = Quest.Type.values()[generator.random.nextInt(Quest.Type.values().length)];
            Map<String, String> questSettings = new HashMap<>();
            int questAmount;
            switch (questType) {
            case KILL:
                switch (generator.random.nextInt(4)) {
                case 0: questSettings.put("entity_type", "zombie"); break;
                case 1: questSettings.put("entity_type", "spider"); break;
                case 2: questSettings.put("entity_type", "creeper"); break;
                case 3: default: questSettings.put("entity_type", "skeleton");
                }
                questAmount = 5 + generator.random.nextInt(15);
                break;
            case SHEAR:
                questSettings.put("entity_type", "sheep");
                questAmount = 5 + generator.random.nextInt(25);
                break;
            case BREAK:
                switch (generator.random.nextInt(5)) {
                case 0: questSettings.put("material", "sand"); break;
                case 1: questSettings.put("material", "diamond_ore"); break;
                case 2: questSettings.put("material", "iron_ore"); break;
                case 3: questSettings.put("material", "gold_ore"); break;
                case 4: default: questSettings.put("material", "coal_ore");
                }
                questAmount = 5 + generator.random.nextInt(35);
                break;
            case FISH:
                questAmount = 5 + generator.random.nextInt(5);
                break;
            default:
                questAmount = 42;
                break;
            }
            Quest quest = new Quest(questType, questAmount, questSettings);
            if (i == 0) {
                quest.minReputation = -999;
            } else {
                quest.minReputation = (i - 1) * 20;
            }
            town.quests.add(quest);
        }
        // NPCs
        int npc_greetings = 1;
        int npc_quests = town.quests.size();
        for (Generator.House house: gt.houses) {
            for (Vec3 vec: house.npcs) {
                NPC npc = new NPC(new Vec3(vec.x, vec.y, vec.z));
                npc.name = generator.generateTownName();
                String message;
                if (npc_quests > 0) {
                    npc_quests -= 1;
                    npc.questId = npc_quests;
                    message = plugin.getMessages().deal(Messages.Type.RANDOM);
                } else if (npc_greetings > 0) {
                    npc_greetings -= 1;
                    message = plugin.getMessages().deal(Messages.Type.GREETING);
                    message = message.replace("%town_name%", town.name);
                } else {
                    message = plugin.getMessages().deal(Messages.Type.RANDOM);
                }
                message = message.replace("%npc_name%", npc.name);
                npc.message = message;
                town.npcs.add(npc);
            }
        }
        Collections.shuffle(town.npcs, generator.random);
        towns.add(town);
        save();
        plugin.getLogger().info("Town " + town.name + " created at " + gt.ax + " " + gt.ay);
        return true;
    }

    void onTick() {
        ticks += 1;
        if (plugin.isCreateTowns()) {
            if (addTownCooldown > 0) {
                addTownCooldown -= 1;
            } else {
                tryToAddTown();
                addTownCooldown = towns.size();
            }
        }
        if (ticks % 20 == 0 && dirty) save();
    }

    NPC findNPC(int townId, int npcId) {
        if (townId >= towns.size()) return null;
        Town town = towns.get(townId);
        if (npcId >= town.npcs.size()) return null;
        return town.npcs.get(npcId);
    }

    String getNPCMessage(int townId, int npcId, Player player) {
        if (townId >= towns.size()) return "Hello World";
        Town town = towns.get(townId);
        if (npcId >= town.npcs.size()) return "Hello World";
        NPC npc = town.npcs.get(npcId);
        return npc.message;
        // if (npc.questId < 0 || npc.questId >= town.quests.size()) {
        //     return npc.message;
        // } else {
        //     Quest quest = town.quests.get(npc.questId);
        //     if (quest.isSignedUp(player)) {
        //         int progress = quest.getProgress(player);
        //         if (progress == 0) {
        //             return quest.getQuestDescription();
        //         } else if (progress >= quest.amount) {
        //             return "YOU DID IT"; // TODO
        //         } else {
        //             return quest.getProgressReport(progress);
        //         }
        //     } else if (quest.minReputation > plugin.getReputations().getReputation(player, town.fraction)) {
        //         return "You are not worthy of my quest.";
        //     } else {
        //         quest.signUp(player);
        //         dirty = true;
        //         return quest.getQuestDescription();
        //     }
        // }
    }
}
