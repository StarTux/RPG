package com.winthier.rpg;

import com.winthier.rpg.Generator.House;
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
        for (int i = 0; i < 5; i += 1) {
            House house = generator.generateHouse(16, 16, EnumSet.noneOf(Generator.Flag.class));
            Debug.printHouse(house);
        }
        for (int i = 0; i < 10; i += 1) {
            System.out.println(generator.generateTownName());
        }
    }
}
