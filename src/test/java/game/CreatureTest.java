package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CreatureTest {

    private World world;
    private Creature creature;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[3][3][1];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }

        world = new World(tiles);

        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);

        creature.x = 1;
        creature.y = 1;
        creature.z = 0;
    }

    private List<String> attachRecordingAiAndAddToWorld() {
        List<String> messages = new ArrayList<>();
        new CreatureAi(creature) {
            @Override
            public void onNotify(String message) {
                messages.add(message);
            }
        };
        world.add(creature);
        return messages;
    }

    @Test
    void nonLethalDamageLeavesCreatureAliveAtOneHp() {
        List<String> messages = attachRecordingAiAndAddToWorld();

        creature.modifyHp(-99, "test damage");

        assertEquals(1, creature.hp());
        assertEquals("test damage", creature.causeOfDeath());
        assertSame(creature, world.creature(1, 1, 0), "creature should still be in the world");
        assertNull(world.item(1, 1, 0), "no corpse should be left");
        assertTrue(messages.isEmpty(), "no death message should be sent");
    }

    @Test
    void lethalDamageRunsDeathPath() {
        List<String> messages = attachRecordingAiAndAddToWorld();

        creature.modifyHp(-100, "killed by a test");

        assertEquals(0, creature.hp());
        assertEquals("killed by a test", creature.causeOfDeath());
        assertTrue(messages.contains("You die."), "doAction(\"die\") should notify the creature");

        Item corpse = world.item(1, 1, 0);
        assertNotNull(corpse, "leaveCorpse() should drop a corpse");
        assertEquals("josh corpse", corpse.name());
        assertEquals(500, corpse.foodValue(), "corpse food value should be maxHp * 5");

        assertNull(world.creature(1, 1, 0), "world.remove(this) should remove the creature");
    }

    @Test
    void healingIsCappedAtMaxHp() {
        creature.modifyHp(-50, "setup damage");

        creature.modifyHp(80, "heal");

        assertEquals(100, creature.hp());
        assertEquals(creature.maxHp(), creature.hp());
    }

    @ParameterizedTest(name = "modifyHp({0}) after death")
    @ValueSource(ints = { -1, 15 })  // another hit; a thrown health potion's +15 after the kill
    void modifyHpAfterDeathHasNoEffect(int amountAfterDeath) {
        attachRecordingAiAndAddToWorld();
        creature.modifyHp(-130, "first lethal hit");  // overkill: hp = -30

        creature.modifyHp(amountAfterDeath, "after death");

        assertEquals(1, itemsInWorld(), "exactly one corpse should be left");
        assertEquals(-30, creature.hp(), "hp should not change after death");
        assertEquals("first lethal hit", creature.causeOfDeath());
    }

    private int itemsInWorld() {
        int count = 0;
        for (int x = 0; x < world.width(); x++)
            for (int y = 0; y < world.height(); y++)
                if (world.item(x, y, 0) != null)
                    count++;
        return count;
    }

    @Test
    void newCreatureStartsTwoThirdsFed() {
        assertEquals(1000, creature.maxFood());
        assertEquals(666, creature.food());
    }

    @Test
    void unknownItemIsNamedByItsAppearance() {
        attachRecordingAiAndAddToWorld();
        Item potion = new Item('!', Color.RED, "health potion", "bubbly potion");

        assertEquals("bubbly potion", creature.nameOf(potion));
    }

    @Test
    void learningAnItemRevealsItsRealName() {
        List<String> messages = attachRecordingAiAndAddToWorld();
        Item potion = new Item('!', Color.RED, "health potion", "bubbly potion");

        creature.learnName(potion);

        assertEquals("health potion", creature.nameOf(potion));
        assertTrue(messages.contains("The bubbly potion is a health potion!"));
    }

    @ParameterizedTest(name = "observer at offset ({0}, {1}) is told: {2}")
    @CsvSource({
        " 9,  0, true",   // exactly on the radius-9 circle, in each direction
        "-9,  0, true",
        " 0,  9, true",
        " 0, -9, true",
        " 6,  6, true",   // inside the circle diagonally (36 + 36 = 72 <= 81)
        " 9,  1, false",  // just outside the circle (81 + 1 = 82 > 81)
        " 7,  7, false",  // inside the scanned square, outside the circle (98 > 81)
        "10,  0, false"   // beyond the radius
    })
    void onlyCreaturesWithinRadiusNineAreToldAboutAnAction(int dx, int dy, boolean expectedToBeTold) {
        World bigWorld = floorWorld(21, 21);

        Creature dying = new Creature(bigWorld, '@', Color.GRAY, "josh", 100, 10, 5);
        dying.x = 10;
        dying.y = 10;
        dying.z = 0;
        recordMessages(dying);
        bigWorld.add(dying);

        Creature observer = new Creature(bigWorld, 'g', Color.GREEN, "goblin", 10, 0, 0);
        observer.x = 10 + dx;
        observer.y = 10 + dy;
        observer.z = 0;
        List<String> observerMessages = recordMessages(observer);
        bigWorld.add(observer);

        dying.modifyHp(-100, "test");

        assertEquals(expectedToBeTold, observerMessages.contains("The josh dies."));
    }

    private static World floorWorld(int width, int height) {
        Tile[][][] tiles = new Tile[width][height][1];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                tiles[x][y][0] = Tile.FLOOR;
        return new World(tiles);
    }

    private static List<String> recordMessages(Creature c) {
        List<String> messages = new ArrayList<>();
        new CreatureAi(c) {
            @Override
            public void onNotify(String message) {
                messages.add(message);
            }
        };
        return messages;
    }
}
