package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.event.CustomTickEvent;
import com.winthier.custom.util.Items;
import com.winthier.rpg.Generator.House;
import com.winthier.rpg.Generator.Town;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Getter
public final class RPGPlugin extends JavaPlugin implements Listener {
    private final Random random = new Random();
    String worldName = "";
    RPGWorld world = null;
    private Messages messages = null;
    private boolean createTowns = false;
    private Reputations reputations;
    private Set<GameMode> allowedGameModes = EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE);

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
        event.addItem(new DeliveryItem(this));
        worldName = getConfig().getString("worlds", "Resource");
        createTowns = getConfig().getBoolean("create_towns");
        if (world != null && world.isDirty()) world.save();
        world = null;
    }

    @EventHandler
    public void onCustomTick(CustomTickEvent event) {
        switch (event.getType()) {
        case WILL_TICK_ITEMS:
            if (event.getTicks() % 20 == 0) {
                for (Player player: getServer().getOnlinePlayers()) {
                    player.removeMetadata("MiniMapCursors", this);
                }
            }
            break;
        default: break;
        }
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
                player.teleport(world.getWorld().getHighestBlockAt((town.area.ax + town.area.bx) / 2, (town.area.ay + town.area.by) / 2).getLocation().add(0.5, 0.5, 0.5));
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
            if (player == null || getRPGWorld(player.getWorld()) == null) return false;
            Generator generator = new Generator(this);
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
            } else if (structure.equals("farm")) {
                generator.plantFarm(player.getLocation().getBlock().getRelative(-size / 2, 0, -size / 2), size, size);
                sender.sendMessage("Farm generated");
            } else if (structure.equals("pasture")) {
                generator.plantPasture(player.getLocation().getBlock().getRelative(-size / 2, 0, -size / 2), size, size);
                sender.sendMessage("Pasture generated");
            } else if (structure.equals("name")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 20; i += 1) sb.append(" ").append(generator.generateName(size));
                sender.sendMessage("Random names with " + size + " syllables: " + sb.toString());
            } else {
                sender.sendMessage("Unknown structure: '" + structure + "'");
            }
        } else if (cmd.equals("givedelivery") && args.length == 2) {
            if (getRPGWorld() == null) return false;
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            ItemStack item = CustomPlugin.getInstance().getItemManager().spawnItemStack(DeliveryItem.CUSTOM_ID, 1);
            world.updateDeliveryItem(item, target);
            Items.give(item, target);
            sender.sendMessage("Delivery book given to " + target.getName());
        } else if (cmd.equals("rep") && args.length >= 1 && args.length <= 2) {
            Player target;
            if (args.length >= 2) {
                target = getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found: " + args[1]);
                    return true;
                }
            } else {
                if (player == null) return false;
                target = player;
            }
            sender.sendMessage("Reputations of " + target.getName() + ":");
            List<RPGWorld.Fraction> fractions = new ArrayList<>();
            for (RPGWorld.Fraction f: RPGWorld.Fraction.values()) fractions.add(f);
            Collections.sort(fractions, (b, a) -> Integer.compare(getReputations().getReputation(target, a), getReputations().getReputation(target, b)));
            for (RPGWorld.Fraction fraction: fractions) {
                sender.sendMessage("" + getReputations().getReputation(target, fraction) + ") " + fraction.name().toLowerCase());
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
            return Arrays.asList("tp", "whereami", "gen", "givedelivery", "rep").stream().filter(i -> i.startsWith(term)).collect(Collectors.toList());
        } else if ("gen".equals(cmd) && args.length == 2) {
            String term = args[1].toLowerCase();
            return Arrays.asList("town", "house", "fountain", "name").stream().filter(i -> i.startsWith(term)).collect(Collectors.toList());
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        RPGWorld rpgWorld = getRPGWorld(block.getWorld());
        if (rpgWorld == null) return;
        RPGWorld.Belonging belonging = rpgWorld.getBelongingAt(block);
        if (belonging == null || belonging.town == null) return;
        Player player = event.getPlayer();
        StringBuilder sb = new StringBuilder();
        for (Struct struct: belonging.structs) {
            sb.append(" ").append(struct.type.name().toLowerCase());
            for (String tag: struct.tags) sb.append(" ").append(tag);
        }
        player.sendMessage("Structures: " + sb);
        if (!allowedGameModes.contains(player.getGameMode())) return;
        if (belonging.structs.isEmpty()) return;
        getReputations().giveReputation(player, belonging.town.fraction, -1);
        PotionEffect potion = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
        int level;
        int duration;
        if (potion != null) {
            level = Math.max(1, potion.getAmplifier());
            duration = Math.max(200, potion.getDuration());
        } else {
            level = 1;
            duration = 200;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, level), true);
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        player.spawnParticle(Particle.VILLAGER_ANGRY, loc, 3, .2, .2, .2, 0.0);
        player.playSound(loc, Sound.ENTITY_VILLAGER_HURT, 0.1f, 0.1f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        RPGWorld rpgWorld = getRPGWorld(block.getWorld());
        if (rpgWorld == null) return;
        RPGWorld.Belonging belonging = rpgWorld.getBelongingAt(block);
        if (belonging == null || belonging.town == null) return;
        Player player = event.getPlayer();
        if (!allowedGameModes.contains(player.getGameMode())) return;
        if (belonging.structs.isEmpty()) return;
        PotionEffect potion = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
        int level;
        int duration;
        if (potion != null) {
            level = potion.getAmplifier();
            duration = Math.max(100, potion.getDuration());
        } else {
            level = 0;
            duration = 100;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, level), true);
    }
}
