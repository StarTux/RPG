package com.winthier.rpg;

import com.winthier.rpg.Generator.House;
import com.winthier.rpg.Generator.RoomTile;
import com.winthier.rpg.Generator.Vec2;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class Debug {
    private Debug() { }

    static void printHouse(House house) {
        int sx = 0;
        int sy = 0;
        for (Vec2 vec: house.tiles.keySet()) {
            if (vec.x > sx) sx = vec.x;
            if (vec.y > sy) sy = vec.y;
        }
        System.out.println("" + sx + "," + sy);
        for (int y = 0; y <= sy; y += 1) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x <= sx; x += 1) {
                RoomTile tile = house.tiles.get(new Vec2(x, y));
                if (tile == null) {
                    sb.append(" ");
                } else if (tile == RoomTile.WALL) {
                    RoomTile left = house.tiles.get(new Vec2(x, y + 1));
                    RoomTile right = house.tiles.get(new Vec2(x, y - 1));
                    if (left == null || right == null || left == RoomTile.FLOOR || right == RoomTile.FLOOR) {
                        sb.append("=");
                    } else {
                        sb.append("|");
                    }
                } else {
                    sb.append(tile.stringIcon);
                }
            }
            System.out.println(sb.toString());
        }
    }

    static void printChunks(List<Vec2> chunks) {
        Vec2 vecCenter = chunks.get(0);
        int minx = vecCenter.x;
        int miny = vecCenter.y;
        int maxx = minx;
        int maxy = miny;
        for (Vec2 chunk: chunks) {
            if (chunk.x < minx) minx = chunk.x;
            if (chunk.y < miny) miny = chunk.y;
            if (chunk.x > maxx) maxx = chunk.x;
            if (chunk.y > maxy) maxy = chunk.y;
        }
        for (int y = 0; y <= maxy - miny; y += 1) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x <= maxx - minx; x += 1) {
                Vec2 vec = new Vec2(x + minx, y + miny);
                if (vec.equals(vecCenter)) {
                    sb.append("X");
                } else if (chunks.contains(vec)) {
                    sb.append("O");
                } else {
                    sb.append(".");
                }
            }
            System.out.println(sb.toString());
        }
    }
}
