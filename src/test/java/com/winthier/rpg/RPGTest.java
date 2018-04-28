package com.winthier.rpg;

import com.winthier.rpg.Generator.House;
import com.winthier.rpg.Generator.DungeonFloor;
import java.util.EnumSet;
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
        // for (int i = 0; i < 10; i += 1) {
        //     House house = generator.generateHouse(16, 16);
        //     Debug.printHouse(house.tiles);
        // }
        DungeonFloor floor = generator.generateDungeonFloor(16, 16, 7, 7);
        Debug.printHouse(floor.tiles);
        // for (int syllables = 1; syllables < 5; syllables += 1) {
        //     System.out.print("[" + syllables + "]");
        //     for (int i = 0; i < 20; i += 1) {
        //         System.out.print(" " + generator.generateName(syllables));
        //     }
        //     System.out.println();
        // }
        // for (int i = 0; i < 9999; i += 1) {
        //     String name = generator.generateName(5);
        //     String name2 = generator.cleanSpecialChars(name.toLowerCase());
        //     if (name2.contains("ass")) System.out.println("= " + name);
        // }
        System.out.println();
    }
}
