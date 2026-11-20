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
}
