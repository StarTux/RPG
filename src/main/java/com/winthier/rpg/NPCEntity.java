package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import com.winthier.custom.util.Msg;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

@Getter @RequiredArgsConstructor
public final class NPCEntity implements CustomEntity, TickableEntity {
    public static final String CUSTOM_ID = "rpg:npc";
    private final RPGPlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Villager.class);
    }

    @Override
    public Entity spawnEntity(Location location, Object conf) {
        System.out.println(conf);
        EntityType et = null;
        if (conf instanceof Map) {
            Map config = (Map)conf;
            if (config.containsKey("type")) {
                et = EntityType.fromName(config.get("type").toString().toUpperCase());
            }
        }
        if (et == null) et = EntityType.VILLAGER;
        return location.getWorld().spawnEntity(location, et);
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity entity) {
        return new Watcher((LivingEntity)entity, this);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event, EntityContext context) {
        event.setCancelled(true);
        if (event.getDamager() instanceof Player) {
            ((Watcher)context.getEntityWatcher()).onTouch((Player)event.getDamager());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        event.setCancelled(true);
        ((Watcher)context.getEntityWatcher()).onTouch(event.getPlayer());
    }

    @Override
    public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    public final class Watcher implements EntityWatcher {
        private final LivingEntity entity;
        private final NPCEntity customEntity;
        private int townId = -1;
        private int npcId = -1;
        int ticks = -1;
        int touchCooldown = 0;
        int messageIndex = 0;
        List<String> messages = Arrays.asList("Welcome to Poto Village!", "I heard rumors of monsters returning.", "How did you get here?");

        void onTick() {
            ticks += 1;
            if (ticks % 20 == 0) {
                entity.setAI(false);
                if (plugin.getRandom().nextBoolean()) {
                    Location loc = entity.getLocation();
                    loc.setYaw(loc.getYaw() + plugin.getRandom().nextFloat() * 45.0f - plugin.getRandom().nextFloat() * 45.0f);
                    entity.teleport(loc);
                }
            }
            if (touchCooldown > 0) {
                touchCooldown -= 1;
            }
        }

        void onTouch(Player player) {
            if (touchCooldown > 0) return;
            touchCooldown = 20;
            if (messages.isEmpty()) return;
            for (Entity nearby: entity.getWorld().getNearbyEntities(entity.getEyeLocation().add(0, 0.3, 0), 1, 1, 1)) {
                if (!nearby.equals(entity) && nearby.getType() == EntityType.ARMOR_STAND) {
                    EntityWatcher nearbyWatcher = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby);
                    if (nearbyWatcher != null && nearbyWatcher instanceof NPCSpeechEntity.Watcher && ((NPCSpeechEntity.Watcher)nearbyWatcher).getLiving().equals(entity)) {
                        return;
                    }
                }
            }
            NPCSpeechEntity.Watcher speechWatcher = (NPCSpeechEntity.Watcher)CustomPlugin.getInstance().getEntityManager().spawnEntity(entity.getEyeLocation().add(0, 10, 0), NPCSpeechEntity.CUSTOM_ID);
            speechWatcher.setLiving(entity);
            speechWatcher.getMessages().addAll(Msg.wrap(messages.get(messageIndex), 16));
            speechWatcher.setColor(ChatColor.GREEN);
            messageIndex += 1;
            if (messageIndex >= messages.size()) messageIndex = 0;
        }
    }

    // public enum Type {
    //     public final EntityType entityType;
    //     Type(EntityType
    // }
}
