package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.block.CustomBlock;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public final class CanonTravelBlock implements CustomBlock {
    public static final String CUSTOM_ID = "rpg:canon_travel";
    private final RPGPlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public void setBlock(Block block) {
        block.setType(Material.IRON_PLATE);
    }

    @Override
    public void blockWasLoaded(BlockWatcher watcher) {
        switch (watcher.getBlock().getType()) {
        case GOLD_PLATE:
        case IRON_PLATE:
        case STONE_PLATE:
        case WOOD_PLATE:
            return;
        default:
            CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(watcher);
        }
    }

    void canonTask(TaskData data) {
        int ticks = data.ticks++;
        int seconds = ticks / 20;
        if (!data.player.isOnline()) {
            data.cancel();
            return;
        }
        if (ticks <= 120) {
            Block pb = data.player.getLocation().getBlock();
            Block block = data.blockWatcher.getBlock();
            if (!pb.equals(block) && !pb.equals(block.getRelative(0, 1, 0))) {
                data.cancel();
                return;
            }
            if (ticks % 20 == 0) {
                if (seconds == 0) {
                    if (data.town != null) {
                        Msg.sendActionBar(data.player, "&aDestination: &o" + data.town.name);
                    }
                } else if (seconds < 4) {
                    data.player.sendTitle(ChatColor.GRAY + "" + (4 - seconds), ChatColor.DARK_GRAY + "Cannon Travel", 10, 10, 10);
                    data.player.playSound(data.player.getEyeLocation(), Sound.BLOCK_NOTE_BASEDRUM, SoundCategory.MASTER, 0.5f, 2.0f);
                } else if (seconds == 4) {
                    data.player.playSound(data.player.getEyeLocation(), Sound.ENTITY_TNT_PRIMED, SoundCategory.MASTER, 0.5f, 0.5f);
                } else if (seconds == 6) {
                    double vx = plugin.getRandom().nextBoolean() ? 8.0 : -8.0;
                    double vz = plugin.getRandom().nextBoolean() ? 8.0 : -8.0;
                    data.player.setVelocity(new Vector(0, 10.0, 0));
                }
            } else if (ticks == 118) {
                data.player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, data.player.getLocation(), 15, 2, 2, 2, 0);
                data.player.getWorld().playSound(data.player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 2.0f, 0.75f);
            }
        } else {
            if (!data.player.getWorld().equals(data.blockWatcher.getBlock().getWorld())) {
                data.cancel();
                return;
            }
            if (seconds > 9 || data.player.getLocation().getY() > 255.0) {
                plugin.getFreeFalls().add(data.player.getUniqueId());
                Location loc = data.to.getLocation().add(0.5, 0, 0.5);
                Location pl = data.player.getLocation();
                loc.setPitch(pl.getPitch());
                loc.setYaw(pl.getYaw());
                data.player.teleport(loc);
                data.cancel();
                return;
            } else {
                data.player.setVelocity(new Vector(0, 10.0, 0));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event, BlockContext context) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (getTask(event.getPlayer()) != null) return;
        if (plugin.getRPGWorld() == null) return;
        TaskData data = new TaskData(event.getPlayer(), context.getBlockWatcher());
        data.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> canonTask(data), 0, 1);
        storeTask(event.getPlayer(), data.task);
        for (ItemStack item: event.getPlayer().getInventory()) {
            if (item != null && item.getType() != Material.AIR && CustomPlugin.getInstance().getItemManager().getCustomItem(item) instanceof DeliveryItem) {
                Vec2 recipient = DeliveryItem.getRecipient(item);
                data.town = plugin.getRPGWorld().findTown(recipient.x);
                break;
            }
        }
        if (data.town == null) {
            double size = plugin.getRPGWorld().getWorld().getWorldBorder().getSize() * 0.5 - 128.0;
            int isize = (int)size * 2;
            Block min = plugin.getRPGWorld().getWorld().getWorldBorder().getCenter().add(-size, 0, -size).getBlock();
            data.to = plugin.getRPGWorld().getWorld().getBlockAt(min.getX() + plugin.getRandom().nextInt(isize),
                                                                 255,
                                                                 min.getZ() + plugin.getRandom().nextInt(isize));
        } else {
            int cx = (data.town.area.ax + data.town.area.bx) / 2;
            int cy = (data.town.area.ay + data.town.area.by) / 2;
            int dx = cx < 0 ? 1 : -1;
            int dy = cy < 0 ? 1 : -1;
            int full = 250;
            int part = plugin.getRandom().nextInt(full);
            int x, y;
            if (plugin.getRandom().nextBoolean()) {
                x = cx + dx * full;
                y = cy + dy * part;
            } else {
                x = cx + dx * part;
                y = cy + dy * full;
            }
            data.to = plugin.getRPGWorld().getWorld().getBlockAt(x, 255, y);
        }
    }

    BukkitTask getTask(Player player) {
        for (MetadataValue value: player.getMetadata("CanonTask")) {
            if (value.getOwningPlugin() == plugin) {
                return (BukkitTask)value.value();
            }
        }
        return null;
    }

    void storeTask(Player player, BukkitTask task) {
        player.setMetadata("CanonTask", new FixedMetadataValue(plugin, task));
    }


    @RequiredArgsConstructor
    class TaskData {
        final Player player;
        final BlockWatcher blockWatcher;
        BukkitTask task;
        RPGWorld.Town town;
        Block to;
        int ticks = 0;
        void cancel() {
            player.removeMetadata("CanonTask", plugin);
            task.cancel();
        }
    }
}
