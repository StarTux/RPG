package com.winthier.rpg;

import com.winthier.custom.event.CustomRegisterEvent;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import lombok.Getter;
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

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        event.addEntity(new NPCEntity(this));
        event.addEntity(new NPCSpeechEntity(this));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("test")) {
            int size;
            if (args.length >= 2) {
                size = Integer.parseInt(args[1]);
            } else {
                size = 16;
            }
            Set<GeneratorFlag> flags = EnumSet.noneOf(GeneratorFlag.class);
            for (int i = 2; i < args.length; i += 1) {
                flags.add(GeneratorFlag.valueOf(args[i].toUpperCase()));
            }
            generator.plantHouse(player.getLocation().getBlock().getRelative(-size/2, 0, -size/2), generator.generateHouse(size, size, flags), flags);
            sender.sendMessage("House size " + size + " generated with " + flags);
        } else {
            return false;
        }
        return true;
    }
}
