package com.winthier.rpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.configuration.ConfigurationSection;

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
        HOUSE, ROOM, FOUNTAIN, FARM, CROPS, PASTURE, UNKNOWN;
    }

    final Type type;
    final Cuboid boundingBox;
    final List<Struct> subs = new ArrayList<>();
    final List<String> tags = new ArrayList<>();

    Struct(Type type, Cuboid boundingBox, List<Struct> subs, List<String> tags) {
        this.type = type;
        this.boundingBox = boundingBox;
        if (subs != null) this.subs.addAll(subs);
        if (tags != null) this.tags.addAll(tags);
    }

    Struct(ConfigurationSection config) {
        Type type;
        try {
            type = Type.valueOf(config.getString("type", "").toUpperCase());
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            type = Type.UNKNOWN;
        }
        this.type = type;
        boundingBox = new Cuboid(config.getIntegerList("bounding_box"));
        for (Map<?, ?> map: config.getMapList("subs")) {
            ConfigurationSection section = config.createSection("tmp", map);
            subs.add(new Struct(section));
        }
        tags.addAll(config.getStringList("tags"));
    }

    Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type.name().toLowerCase());
        result.put("bounding_box", boundingBox.serialize());
        result.put("subs", subs.stream().map(r -> r.serialize()).collect(Collectors.toList()));
        result.put("tags", tags);
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
