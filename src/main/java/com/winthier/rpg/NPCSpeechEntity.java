package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffectType;

public final class NPCSpeechEntity implements CustomEntity, TickableEntity {
    public static final String CUSTOM_ID = "rpg:npc_speech";
    private final RPGPlugin plugin;

    NPCSpeechEntity(RPGPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setMarker(true);
                as.setSmall(true);
                as.setGravity(false);
            });
    }

    @Override
    public Watcher createEntityWatcher(Entity entity) {
        return new Watcher((ArmorStand)entity, this);
    }

    @Override
    public void onTick(EntityWatcher entityWatcher) {
        ((Watcher)entityWatcher).onTick();
    }

    @Override
    public void entityWillUnload(EntityWatcher entityWatcher) {
        ((Watcher)entityWatcher).remove();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event, EntityContext context) {
        event.setCancelled(true);
        ((Watcher)context.getEntityWatcher()).remove();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        event.setCancelled(true);
        ((Watcher)context.getEntityWatcher()).remove();
    }

    @Getter @Setter @RequiredArgsConstructor
    public final class Watcher implements EntityWatcher {
        private final ArmorStand entity;
        private final NPCSpeechEntity customEntity;
        private LivingEntity living;
        private int ticks = 0;
        private String message = "";
        private ChatColor color = ChatColor.WHITE;

        void onTick() {
            if (living == null || !living.isValid()) {
                remove();
            } else {
                ticks += 1;
                if (living.getCustomName() == null) {
                    entity.teleport(living.getEyeLocation().add(0, 0.35, 0));
                } else {
                    entity.teleport(living.getEyeLocation().add(0, 0.5, 0));
                }
                int shown = Math.min(message.length(), ticks / 2);
                if (ticks > message.length() * 4) {
                    remove();
                } else {
                    entity.setCustomName(color + message.substring(0, shown));
                    entity.setCustomNameVisible(true);
                }
            }
        }

        void remove() {
            entity.remove();
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
        }
    }
}
