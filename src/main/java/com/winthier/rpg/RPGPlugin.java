package com.winthier.rpg;

import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.rpg.Generator.House;
import com.winthier.rpg.Generator.Town;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class RPGPlugin extends JavaPlugin implements Listener {
    private final Random random = new Random();
    String worldName = "";
    RPGWorld world = null;
    private Messages messages = null;
    private boolean createTowns = false;
    private Reputations reputations;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> { if (getRPGWorld() != null) world.onTick(); }, 1, 1);
    }

    @Override
    public void onDisable() {
        if (world != null && world.isDirty()) world.save();
        world = null;
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        saveDefaultConfig();
        messages = null;
        reputations = null;
        event.addEntity(new NPCEntity(this));
        event.addEntity(new NPCSpeechEntity(this));
        worldName = getConfig().getString("worlds", "Resource");
        createTowns = getConfig().getBoolean("create_towns");
        if (world != null && world.isDirty()) world.save();
        world = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if ("tp".equals(cmd) && args.length == 2) {
            if (player == null || getRPGWorld() == null) return false;
            String townName = args[1];
            RPGWorld.Town town = world.findTown(townName);
            if (town == null) {
                player.sendMessage("Town not found: " + townName);
                return true;
            } else {
                player.teleport(world.getWorld().getHighestBlockAt((town.area.ax + town.area.bx) / 2, (town.area.ay + town.area.by) / 2).getLocation().add(0.5, 0, 0.5));
                player.sendMessage("Teleported to " + town.name);
            }
        } else if ("whereami".equals(cmd) && args.length == 1) {
            if (player == null || getRPGWorld() == null) return false;
            Block block = player.getLocation().getBlock();
            int x = block.getX();
            int z = block.getZ();
            for (RPGWorld.Town town: world.towns) {
                if (town.area.contains(x, z)) {
                    sender.sendMessage("You are in " + town.name + ".");
                } else if (town.questArea.contains(x, z)) {
                    sender.sendMessage("You are in the outskirts of " + town.name + ".");
                }
            }
        } else if (cmd.equals("gen") && args.length >= 3) {
            Generator generator = new Generator();
            Set<Generator.Flag> flags = EnumSet.noneOf(Generator.Flag.class);
            int size = Integer.parseInt(args[2]);
            for (int i = 3; i < args.length; i += 1) {
                flags.add(Generator.Flag.valueOf(args[i].toUpperCase()));
            }
            String structure = args[1];
            generator.setFlags(flags);
            if (structure.equals("town")) {
                Town town = generator.tryToPlantTown(player.getLocation().getChunk(), size);
                if (town == null) {
                    sender.sendMessage("FAIL!");
                    return true;
                }
                generator.setFlags(flags);
                generator.plantTown(player.getWorld(), town);
                sender.sendMessage("Success!");
                Debug.printChunks(town.chunks);
                for (House house: town.houses) Debug.printHouse(house);
            } else if (structure.equals("house")) {
                House house = generator.generateHouse(size, size);
                generator.plantHouse(player.getLocation().getBlock().getRelative(-size / 2, 0, -size / 2), house);
                sender.sendMessage("House size " + size + " generated with " + flags);
                Debug.printHouse(house);
            } else if (structure.equals("fountain")) {
                generator.plantFountain(player.getLocation().getBlock().getRelative(-size / 2, 0, -size / 2), size);
                sender.sendMessage("Fountain generated");
            } else {
                sender.sendMessage("Unknown structure: '" + structure + "'");
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (getRPGWorld() == null) return null;
        if (cmd == null) return null;
        if (args.length == 1) {
            String term = args[0].toLowerCase();
            return Arrays.asList("tp", "whereami", "gen").stream().filter(i -> i.startsWith(term)).collect(Collectors.toList());
        } else if ("gen".equals(cmd) && args.length == 2) {
            String term = args[1].toLowerCase();
            return Arrays.asList("town", "house", "fountain").stream().filter(i -> i.startsWith(term)).collect(Collectors.toList());
        } else if ("gen".equals(cmd) && args.length >= 4) {
            String term = args[args.length - 1].toLowerCase();
            return Arrays.asList(Generator.Flag.values()).stream().filter(f -> f.name().toLowerCase().startsWith(term)).map(f -> f.name()).collect(Collectors.toList());
        } else if ("tp".equals(cmd) && args.length == 2) {
            String term = args[1].toLowerCase();
            return world.getTowns().stream().filter(t -> Generator.cleanSpecialChars(t.name.toLowerCase()).startsWith(term)).map(t -> t.name).collect(Collectors.toList());
        }
        return null;
    }

    RPGWorld getRPGWorld() {
        if (world == null) {
            World bworld = getServer().getWorld(worldName);
            if (bworld == null) return null;
            world = new RPGWorld(this, bworld);
            world.load();
        }
        return world;
    }

    RPGWorld getRPGWorld(World bworld) {
        if (!worldName.equals(bworld.getName())) return null;
        return getRPGWorld();
    }

    Messages getMessages() {
        if (messages == null) {
            messages = new Messages(this);
            messages.load();
        }
        return messages;
    }

    Reputations getReputations() {
        if (reputations == null) {
            reputations = new Reputations(this);
        }
        return reputations;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        RPGWorld rpgWorld = getRPGWorld(event.getBlock().getWorld());
        int x = event.getBlock().getX();
        int y = event.getBlock().getY();
        int z = event.getBlock().getZ();
        for (RPGWorld.Town town: rpgWorld.towns) {
            if (town.area.contains(x, z)) {
                if (!town.visited) {
                    town.visited = true;
                    rpgWorld.dirty = true;
                    for (RPGWorld.House house: town.houses) {
                        Cuboid bb = house.boundingBox.grow(1);
                        for (int az = bb.az; az <= bb.bz; az += 1) {
                            for (int ay = bb.ay; ay <= bb.by; ay += 1) {
                                for (int ax = bb.ax; ax <= bb.bx; ax += 1) {
                                    Block block = rpgWorld.world.getBlockAt(ax, ay, az);
                                    if (block.getType() == Material.AIR && block.getLightLevel() == 0) {
                                        block.setType(Material.GLOWSTONE);
                                        getServer().getScheduler().runTask(this, () -> block.setType(Material.AIR));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
