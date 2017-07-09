package com.winthier.rpg;

import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.rpg.Generator.House;
import com.winthier.rpg.Generator.Town;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class RPGPlugin extends JavaPlugin implements Listener {
    Generator generator = new Generator();
    private final Random random = new Random();
    private final List<String> allowedWorlds = new ArrayList<>();
    private final Map<UUID, RPGWorld> worlds = new HashMap<>();
    private Messages messages = null;
    private boolean doCreateTowns = false;
    private Reputations reputations;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> on20Ticks(), 20, 20);
    }

    @Override
    public void onDisable() {
        for (RPGWorld world: worlds.values()) {
            if (world.isDirty()) world.save();
        }
    }

    void on20Ticks() {
        for (String name: allowedWorlds) {
            World world = getServer().getWorld(name);
            if (world != null) {
                getRPGWorld(world).on20Ticks();
            }
        }
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        saveDefaultConfig();
        messages = null;
        reputations = null;
        event.addEntity(new NPCEntity(this));
        event.addEntity(new NPCSpeechEntity(this));
        worlds.clear();
        allowedWorlds.clear();
        allowedWorlds.addAll(getConfig().getStringList("worlds"));
        doCreateTowns = getConfig().getBoolean("create_towns");
        for (RPGWorld world: worlds.values()) {
            if (world.isDirty()) world.save();
        }
        worlds.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("genhouse")) {
            int size;
            if (args.length >= 2) {
                size = Integer.parseInt(args[1]);
            } else {
                size = 16;
            }
            Set<Generator.Flag> flags = EnumSet.noneOf(Generator.Flag.class);
            for (int i = 2; i < args.length; i += 1) {
                flags.add(Generator.Flag.valueOf(args[i].toUpperCase()));
            }
            House house = generator.generateHouse(size, size, flags);
            generator.plantHouse(player.getLocation().getBlock().getRelative(-size/2, 0, -size/2), house, flags);
            sender.sendMessage("House size " + size + " generated with " + flags);
            Debug.printHouse(house);
        } else if (cmd.equals("gentown")) {
            int size;
            if (args.length >= 2) {
                size = Integer.parseInt(args[1]);
            } else {
                size = 8;
            }
            Set<Generator.Flag> flags = EnumSet.noneOf(Generator.Flag.class);
            for (int i = 2; i < args.length; i += 1) {
                flags.add(Generator.Flag.valueOf(args[i].toUpperCase()));
            }
            Town town = generator.tryToPlantTown(player.getLocation().getChunk(), size);
            if (town == null) {
                sender.sendMessage("FAIL!");
                return true;
            }
            generator.plantTown(player.getWorld(), town, flags);
            sender.sendMessage("Success!");
            Debug.printChunks(town.chunks);
            for (House house: town.houses) Debug.printHouse(house);
        } else {
            return false;
        }
        return true;
    }

    RPGWorld getRPGWorld(World world) {
        if (!allowedWorlds.contains(world.getName())) return null;
        RPGWorld result = worlds.get(world.getUID());
        if (result == null) {
            result = new RPGWorld(this, world);
            worlds.put(world.getUID(), result);
            result.load();
        }
        return result;
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
}
