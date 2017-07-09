package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
        private Watcher master;
        private int ticks = 0;
        private List<String> messages = new ArrayList<>();
        private ChatColor color = ChatColor.WHITE;
        private int oldshown = 0;
        private boolean teleported = false;

        void onTick() {
            if (master != null) {
                if (master.entity == null || !master.entity.isValid()) {
                    remove();
                    return;
                }
                if (!master.teleported) return;
                entity.teleport(master.entity.getLocation().add(0, 0.25, 0));
            } else {
                if (living == null || !living.isValid()) {
                    remove();
                    return;
                }
                if (living.getCustomName() == null) {
                    entity.teleport(living.getEyeLocation().add(0, 0.15, 0));
                } else {
                    entity.teleport(living.getEyeLocation().add(0, 0.3, 0));
                }
                if (!teleported) {
                    teleported = true;
                    return;
                }
                ticks += 1;
                if (messages.isEmpty()) {
                    remove();
                    return;
                }
                String message = messages.get(0);
                int shown = ticks / 2;
                if (shown > message.length()) {
                    if (messages.size() == 1) {
                        if (shown > message.length() * 2) {
                            remove();
                        }
                    } else {
                        messages.remove(0);
                        this.master = (NPCSpeechEntity.Watcher)CustomPlugin.getInstance().getEntityManager().spawnEntity(entity.getLocation().add(0, 10, 0), NPCSpeechEntity.CUSTOM_ID);
                        master.setLiving(living);
                        master.setMessages(messages);
                        master.setColor(color);
                        this.living = null;
                    }
                } else {
                    if (shown != oldshown) {
                        oldshown = shown;
                        entity.setCustomName(color + message.substring(0, shown));
                        entity.setCustomNameVisible(true);
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_BREAK, SoundCategory.NEUTRAL, 0.25f, 2.0f);
                    }
                }
            }
        }

        void remove() {
            entity.remove();
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
        }
    }
}
