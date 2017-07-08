package com.winthier.rpg;

import java.util.Map;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public final class RPGTest {
    private Random random = new Random(0);

    @Test
    public void main() {
        Generator generator = new Generator();
        for (int i = 0; i < 5; i += 1) {
            House house = generator.generateHouse(16, 16);
            draw(house);
        }
    }

    void draw(House house) {
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
}
