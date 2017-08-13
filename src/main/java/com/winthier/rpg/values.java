package com.winthier.rpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.ChatColor;

@Value @RequiredArgsConstructor
final class Vec2 {
    public final int x, y;

    Vec2(List<Integer> list) {
        x = list.get(0);
        y = list.get(1);
    }

    List<Integer> serialize() {
        return Arrays.asList(x, y);
    }

    Vec2 relative(int x, int y) {
        return new Vec2(this.x + x, this.y + y);
    }

    Vec2 relative(Vec2 o) {
        return new Vec2(this.x + o.x, this.y + o.y);
    }

    int maxDistance(Vec2 o) {
        return Math.max(Math.abs(x - o.x), Math.abs(y - o.y));
    }

    int distanceSquared(Vec2 o) {
        int a = x - o.x;
        int b = y - o.y;
        return a * a + b * b;
    }
}

@Value @RequiredArgsConstructor
final class Vec3 {
    public final int x, y, z;

    Vec3(List<Integer> list) {
        x = list.get(0);
        y = list.get(1);
        z = list.get(2);
    }

    List<Integer> serialize() {
        return Arrays.asList(x, y, z);
    }

    Vec3 relative(int x, int y, int z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }
}

@Value @RequiredArgsConstructor
final class Cuboid {
    public final int ax, ay, az, bx, by, bz;

    Cuboid(List<Integer> list) {
        ax = list.get(0);
        ay = list.get(1);
        az = list.get(2);
        bx = list.get(3);
        by = list.get(4);
        bz = list.get(5);
    }

    List<Integer> serialize() {
        return Arrays.asList(ax, ay, az, bx, by, bz);
    }

    boolean contains(int x, int y, int z) {
        return x >= ax && y >= ay && z >= az && x <= bx && y <= by && z <= bz;
    }

    Cuboid grow(int blocks) {
        return new Cuboid(ax - 1, ay - 1, az - 1, bx + 1, by + 1, bz + 1);
    }
}

@Value @RequiredArgsConstructor
final class Rectangle {
    public final int ax, ay, bx, by;

    Rectangle(List<Integer> list) {
        ax = list.get(0);
        ay = list.get(1);
        bx = list.get(2);
        by = list.get(3);
    }

    List<Integer> serialize() {
        return Arrays.asList(ax, ay, bx, by);
    }

    boolean contains(int x, int y) {
        return x >= ax && x <= bx && y >= ay && y <= by;
    }

    public boolean intersects(Rectangle other) {
        if (bx < other.ax || ax >= other.bx) return false;
        if (by < other.ay || ay >= other.by) return false;
        return true;
    }

    Rectangle grow(int size) {
        return new Rectangle(ax - size, ay - size, bx + size, by + size);
    }
}

enum Orientation {
    HORIZONTAL,
    VERTICAL;
}

enum Facing {
    NORTH(new Vec2(0, -1), 4, 2, 2, 2),
    SOUTH(new Vec2(0, 1), 3, 3, 3, 0),
    WEST(new Vec2(-1, 0), 2, 4, 0, 1),
    EAST(new Vec2(1, 0), 1, 5, 1, 3);
    public final Vec2 vector;
    public final int dataTorch;
    public final int dataBlock;
    public final int dataStair;
    public final int dataBed;
    public final int dataFenceGate;
    Facing rotate() {
        switch (this) {
        case NORTH: return EAST;
        case EAST: return SOUTH;
        case SOUTH: return WEST;
        case WEST: default: return NORTH;
        }
    }
    Facing opposite() {
        switch (this) {
        case NORTH: return SOUTH;
        case EAST: return WEST;
        case SOUTH: return NORTH;
        case WEST: default: return EAST;
        }
    }
    Facing(Vec2 vector, int dataTorch, int dataBlock, int dataStair, int dataBed) {
        this.vector = vector;
        this.dataTorch = dataTorch;
        this.dataBlock = dataBlock; // stair, end rod, banner
        this.dataStair = dataStair;
        this.dataBed = dataBed;
        this.dataFenceGate = dataBed;
    }
}

final class Struct {
    enum Type {
        HOUSE, ROOM, FOUNTAIN, FARM, CROPS, PASTURE, MINE, LAIR, LAIR_ROOM, UNKNOWN;
    }
    enum Tag {
        NETHER_WART(Tile.of(Material.NETHER_WARTS, 3)),
        WHEAT(Tile.of(Material.CROPS, 7)),
        POTATO(Tile.of(Material.POTATO, 7)),
        CARROT(Tile.of(Material.CARROT, 7)),
        BEETROOT(Tile.of(Material.BEETROOT_BLOCK, 3)),
        DIAMOND(Tile.of(Material.DIAMOND_ORE)),
        COW, PIG, CHICKEN, SHEEP, MUSHROOM_COW, HORSE, ZOMBIE_HORSE, SKELETON_HORSE, DONKEY, MULE,
        ZOMBIE, HUSK, SKELETON, STRAY, CREEPER, SPIDER, CAVE_SPIDER, ENDERMAN;
        final EntityType entityType;
        final Tile tile;
        Tag() {
            EntityType et = null;
            try {
                et = EntityType.valueOf(name());
            } catch (IllegalArgumentException iae) {}
            this.entityType = et;
            this.tile = null;
        }
        Tag(Tile tile) {
            this.entityType = null;
            this.tile = tile;
        }
        static Tag of(EntityType et) {
            for (Tag tag: Tag.values()) {
                if (tag.entityType == et) return tag;
            }
            return null;
        }
        static Tag of(Material mat) {
            for (Tag tag: Tag.values()) {
                if (tag.tile.mat == mat) return tag;
            }
            return null;
        }
    }

    final Type type;
    final Set<Tag> tags = EnumSet.noneOf(Tag.class);
    final Cuboid boundingBox;
    final List<Struct> subs = new ArrayList<>();

    Struct(Type type, Cuboid boundingBox, List<Struct> subs, Collection<Tag> tags) {
        this.type = type;
        this.boundingBox = boundingBox;
        if (subs != null) this.subs.addAll(subs);
        if (tags != null) this.tags.addAll(tags);
    }

    Struct(ConfigurationSection config) {
        type = Type.valueOf(config.getString("type").toUpperCase());
        tags.addAll(config.getStringList("tags").stream().map(s -> Tag.valueOf(s.toUpperCase())).collect(Collectors.toList()));
        boundingBox = new Cuboid(config.getIntegerList("bounding_box"));
        for (Map<?, ?> map: config.getMapList("subs")) {
            ConfigurationSection section = config.createSection("tmp", map);
            subs.add(new Struct(section));
        }
    }

    Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type.name());
        result.put("tags", tags.stream().map(t -> t.name()).collect(Collectors.toList()));
        result.put("bounding_box", boundingBox.serialize());
        result.put("subs", subs.stream().map(r -> r.serialize()).collect(Collectors.toList()));
        return result;
    }

    List<Struct> deepSubs() {
        List<Struct> result = new ArrayList<>();
        for (Struct sub: subs) {
            result.add(sub);
            result.addAll(sub.deepSubs());
        }
        return result;
    }
}

enum Fraction {
    VILLAGER(5, Arrays.asList(EntityType.VILLAGER), ChatColor.GREEN),
    SKELETON(5, Arrays.asList(EntityType.SKELETON, EntityType.STRAY), ChatColor.WHITE),
    ZOMBIE(5, Arrays.asList(EntityType.ZOMBIE, EntityType.HUSK), ChatColor.DARK_GREEN),
    ZOMBIE_VILLAGER(3, Arrays.asList(EntityType.ZOMBIE_VILLAGER), ChatColor.DARK_GREEN),
    OCCULT(2, Arrays.asList(EntityType.WITCH, EntityType.EVOKER, EntityType.VINDICATOR, EntityType.ILLUSIONER), ChatColor.LIGHT_PURPLE),
    NETHER(1, Arrays.asList(EntityType.PIG_ZOMBIE, EntityType.BLAZE, EntityType.WITHER_SKELETON), ChatColor.RED),
    CREEPER(0, Arrays.asList(EntityType.CREEPER), ChatColor.DARK_GREEN);

    public final int chance;
    public final List<EntityType> villagerTypes;
    public final ChatColor color;

    Fraction(int chance, List<EntityType> villagerTypes, ChatColor color) {
        this.chance = chance;
        this.villagerTypes = villagerTypes;
        this.color = color;
    }
}
