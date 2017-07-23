package com.winthier.rpg;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import com.winthier.custom.util.Dirty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
    public void onEntityCombust(EntityCombustEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event, EntityContext context) {
        event.setCancelled(true);
        if (event.getDamager() instanceof Player) {
            Player player = (Player)event.getDamager();
            ((Watcher)context.getEntityWatcher()).onTouch(player, player.getInventory().getItemInMainHand());
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event, EntityContext context) {
        if (((Watcher)context.getEntityWatcher()).pluginTeleport) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ((Watcher)context.getEntityWatcher()).onTouch(player, null);
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
        int moveCooldown = 20;
        int touchCooldown = 0;
        boolean moving, falling;
        Vector moveFrom, moveTo;
        int moveProgress;
        float moveYaw;
        BlockFace moveDirection;
        int moveDuration;
        boolean pluginTeleport = false;
        Player talkingToPlayer;

        void onTick() {
            if (touchCooldown > 0) touchCooldown -= 1;
            if (talkingToPlayer != null) {
                NPCSpeechEntity.Watcher speechWatcher = findSpeechWatcher();
                if (!talkingToPlayer.isOnline()
                    || !talkingToPlayer.getWorld().equals(entity.getWorld())
                    || talkingToPlayer.getLocation().distanceSquared(entity.getLocation()) > 25.0) {
                    if (speechWatcher != null) speechWatcher.setQuit(true);
                    talkingToPlayer = null;
                } else {
                    if (speechWatcher == null) {
                        talkingToPlayer = null;
                    } else {
                        Location loc = entity.getLocation();
                        Vector dir = talkingToPlayer.getEyeLocation().toVector().subtract(entity.getEyeLocation().toVector()).normalize();
                        loc.setDirection(dir);
                        teleport(loc);
                    }
                }
            } else if (falling || moving) {
                moveProgress += falling ? 2 : 1;
                Vector v = moveFrom.clone().multiply(10 - moveProgress).add(moveTo.clone().multiply(moveProgress)).multiply(0.1);
                Location loc = v.toLocation(entity.getWorld(), moveYaw, 0);
                teleport(loc);
                if (moveProgress >= 10) {
                    moving = falling = false;
                }
            } else if (moveDuration > 0) {
                Block blockTo = entity.getLocation().getBlock().getRelative(moveDirection);
                if (blockTo.getType().isSolid()) {
                    blockTo = blockTo.getRelative(0, 1, 0);
                } else if (!blockTo.getRelative(0, -1, 0).getType().isSolid()) {
                    blockTo = blockTo.getRelative(0, -1, 0);
                }
                if (blockTo.getRelative(0, -1, 0).getType().isSolid()
                    && !blockTo.getType().isSolid()
                    && !blockTo.getRelative(0, 1, 0).getType().isSolid()
                    && !blockTo.getRelative(0, 2, 0).getType().isSolid()) {
                    moveFrom = entity.getLocation().toVector();
                    moveTo = blockTo.getLocation().add(0.5, 0, 0.5).toVector();
                    moveProgress = 0;
                    moveDuration -= 1;
                    moving = true;
                } else {
                    moveDuration = 0;
                }
            } else if (moveCooldown > 0) {
                moveCooldown -= 1;
            } else {
                moveCooldown = 20 + plugin.getRandom().nextInt(80);
                entity.setAI(false);
                talkingToPlayer = null;
                Block block = entity.getLocation().getBlock();
                if (!block.getRelative(0, -1, 0).getType().isSolid()) {
                    moving = false;
                    falling = true;
                    moveDuration = 0;
                    moveFrom = entity.getLocation().toVector();
                    moveTo = block.getRelative(0, -1, 0).getLocation().add(0.5, 0.0, 0.5).toVector();
                    moveProgress = 0;
                    moveYaw = entity.getLocation().getYaw();
                } else {
                    switch (plugin.getRandom().nextInt(4)) {
                    case 0:
                        moveDirection = BlockFace.NORTH;
                        moveYaw = 180.0f;
                        break;
                    case 1:
                        moveDirection = BlockFace.EAST;
                        moveYaw = 270.0f;
                        break;
                    case 2:
                        moveDirection = BlockFace.SOUTH;
                        moveYaw = 0.0f;
                        break;
                    case 3: default:
                        moveDirection = BlockFace.WEST;
                        moveYaw = 90.0f;
                    }
                    moveDuration = 1 + plugin.getRandom().nextInt(5);
                }
            }
        }

        void onTouch(Player player, ItemStack item) {
            if (touchCooldown > 0) return;
            touchCooldown = 20;
            if (townId < 0 || npcId < 0) return;
            RPGWorld rpgworld = plugin.getRPGWorld();
            if (rpgworld == null) return;
            if (falling) return;
            if (talkingToPlayer != null) return;
            moving = false;
            String itemId = item != null ? CustomPlugin.getInstance().getItemManager().getCustomId(item) : null;
            String message = null;
            if (itemId != null && item.getAmount() == 1 && QuestTokenItem.CUSTOM_ID.equals(itemId)) {
                RPGWorld.Town town = rpgworld.findTown(townId);
                RPGWorld.NPC npc = rpgworld.findNPC(townId, npcId);
                RPGWorld.Quest quest = rpgworld.findQuest(townId, npc.questId);
                if (town != null && npc != null && quest != null
                    && quest.state == RPGWorld.Quest.State.COMPLETED
                    && quest.tokenName != null
                    && quest.tokenName.equals(QuestTokenItem.getToken(item))) {
                    item.setAmount(0);
                    quest.state = RPGWorld.Quest.State.RETURNED;
                    message = quest.messages.get(RPGWorld.Quest.MessageType.SUCCESS);
                    if (quest.unlocksNext) npc.questId += 1;
                    plugin.getReputations().giveReputation(player, town.fraction, quest.reputation);
                    plugin.playQuestCompleteEffect(player, entity.getEyeLocation().add(0, 0.5, 0));
                    rpgworld.dirty = true;
                }
            } else if (itemId != null && item.getAmount() == 1 && DeliveryItem.CUSTOM_ID.equals(itemId)) {
                Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
                Vec2 recipient = DeliveryItem.getRecipient(item);
                String owner = config.getString(DeliveryItem.KEY_OWNER);
                long timestamp = config.getLong(DeliveryItem.KEY_TIMESTAMP);
                if (timestamp == rpgworld.getTimestamp()
                    && townId == recipient.x && npcId == recipient.y
                    && player.getUniqueId().toString().equals(owner)) {
                    RPGWorld.Town town = rpgworld.findTown(townId);
                    RPGWorld.NPC npc = rpgworld.findNPC(townId, npcId);
                    Vec2 vecSender = DeliveryItem.getSender(item);
                    RPGWorld.NPC senderNPC = vecSender != null ? rpgworld.findNPC(vecSender.x, vecSender.y) : null;
                    Vec2 vecNext = rpgworld.updateDeliveryItem(item, player);
                    RPGWorld.NPC nextNPC = rpgworld.findNPC(vecNext.x, vecNext.y);
                    String npcName = npc != null ? npc.name : "me";
                    String nextName = nextNPC != null ? nextNPC.name : "someone";
                    String senderName = senderNPC != null ? senderNPC.name : "The Post Office";
                    message = plugin.getMessages().deal(Messages.Type.DELIVERY_THANKS);
                    message = message.replace("%sender%", senderName);
                    message = message.replace("%npc%", npc.name);
                    message = message.replace("%next%", nextNPC.name);
                    plugin.getReputations().giveReputation(player, town.fraction, 10);
                    plugin.playQuestCompleteEffect(player, entity.getEyeLocation().add(0, 0.5, 0));
                }
            }
            if (message == null) {
                message = rpgworld.onPlayerInteractNPC(player, this, townId, npcId);
            }
            if (message == null) return;
            NPCSpeechEntity.Watcher speechWatcher = (NPCSpeechEntity.Watcher)CustomPlugin.getInstance().getEntityManager().spawnEntity(entity.getEyeLocation().add(0, 5, 0), NPCSpeechEntity.CUSTOM_ID);
            speechWatcher.setLiving(entity);
            speechWatcher.setMessage(message);
            speechWatcher.setColor(rpgworld.towns.get(townId).fraction.color);
            talkingToPlayer = player;
        }

        void setIds(int town, int npc) {
            this.townId = town;
            this.npcId = npc;
        }

        void teleport(Location loc) {
            pluginTeleport = true;
            entity.teleport(loc);
            pluginTeleport = false;
        }

        void save() {
            entity.addScoreboardTag("winthier.rpg.npc=" + townId + ":" + npcId);
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

        NPCSpeechEntity.Watcher findSpeechWatcher() {
            for (Entity nearby: entity.getWorld().getNearbyEntities(entity.getEyeLocation().add(0, 3, 0), 1, 4, 1)) {
                if (!nearby.equals(this.entity) && nearby.getType() == EntityType.ARMOR_STAND) {
                    EntityWatcher nearbyWatcher = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby);
                    if (nearbyWatcher != null && nearbyWatcher instanceof NPCSpeechEntity.Watcher) {
                        NPCSpeechEntity.Watcher watcher = (NPCSpeechEntity.Watcher)nearbyWatcher;
                        if (this.entity.equals(watcher.getLiving())) {
                            return watcher;
                        }
                    }
                }
            }
            return null;
        }
    }
}
