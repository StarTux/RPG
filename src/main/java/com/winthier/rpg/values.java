package com.winthier.rpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
final class Vec2 {
    public final int x, y;
    Vec2 relative(int x, int y) {
        return new Vec2(this.x + x, this.y + y);
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
