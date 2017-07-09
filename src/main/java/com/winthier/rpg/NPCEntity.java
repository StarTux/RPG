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
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;

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
        EntityType et = null;
        int townId = -1;
        int npcId = -1;
        if (conf instanceof Map) {
            Map config = (Map)conf;
            if (config.containsKey("type")) et = EntityType.fromName(config.get("type").toString().toUpperCase());
            if (config.containsKey("town_id") && config.containsKey("npc_id")) {
                townId = ((Number)config.get("town_id")).intValue();
                npcId = ((Number)config.get("npc_id")).intValue();
            }
        }
        if (et == null) et = EntityType.VILLAGER;
        Entity e = location.getWorld().spawnEntity(location, et);
        if (townId > -1 && npcId > -1) {
            e.addScoreboardTag("winthier.rpg.npc=" + townId + ":" + npcId);
        }
        return e;
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity entity) {
        return new Watcher((LivingEntity)entity, this);
    }

    @Override
    public void entityWatcherDidRegister(EntityWatcher watcher) {
        ((Watcher)watcher).load();
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

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event, EntityContext context) {
        context.getEntity().setPortalCooldown(999);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event, EntityContext context) {
        event.setCancelled(true);
        context.getEntity().setPortalCooldown(999);
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
            if (townId < 0 || npcId < 0) return;
            RPGWorld rpgworld = plugin.getRPGWorld(entity.getWorld());
            if (rpgworld == null) return;
            for (Entity nearby: entity.getWorld().getNearbyEntities(entity.getEyeLocation().add(0, 0.3, 0), 1, 1, 1)) {
                if (!nearby.equals(entity) && nearby.getType() == EntityType.ARMOR_STAND) {
                    EntityWatcher nearbyWatcher = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby);
                    if (nearbyWatcher != null && nearbyWatcher instanceof NPCSpeechEntity.Watcher) {
                        return;
                    }
                }
            }
            NPCSpeechEntity.Watcher speechWatcher = (NPCSpeechEntity.Watcher)CustomPlugin.getInstance().getEntityManager().spawnEntity(entity.getEyeLocation().add(0, 10, 0), NPCSpeechEntity.CUSTOM_ID);
            speechWatcher.setLiving(entity);
            speechWatcher.getMessages().addAll(Msg.wrap(rpgworld.getNPCMessage(townId, npcId, player), 16));
            speechWatcher.setColor(ChatColor.GREEN);
            Location loc = entity.getLocation();
            Vector dir = player.getEyeLocation().toVector().subtract(entity.getEyeLocation().toVector()).normalize();
            loc.setDirection(dir);
            entity.teleport(loc);
        }

        void setIds(int town, int npc) {
            this.townId = town;
            this.npcId = npc;
        }

        void load() {
            try {
                for (String tag: entity.getScoreboardTags()) {
                    if (tag.startsWith("winthier.rpg.npc=")) {
                        String[] tok1 = tag.split("=", 2);
                        String[] tok2 = tok1[1].split(":", 2);
                        this.townId = Integer.parseInt(tok2[0]);
                        this.npcId = Integer.parseInt(tok2[1]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // public enum Type {
    //     public final EntityType entityType;
    //     Type(EntityType
    // }
}
