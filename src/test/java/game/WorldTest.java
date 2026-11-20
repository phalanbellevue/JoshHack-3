package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorldTest {

    private World world;

    @BeforeEach
    void setUp() {
        // A 3x3x1 world of floor, with one wall in the far corner so we can
        // tell a stored tile apart from the default.
        Tile[][][] tiles = new Tile[3][3][1];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }
        tiles[2][2][0] = Tile.WALL;

        world = new World(tiles);
    }

    private List<String> recordMessages(Creature creature) {
        List<String> messages = new ArrayList<>();
        new CreatureAi(creature) {
            @Override
            public void onNotify(String message) {
                messages.add(message);
            }
        };
        return messages;
    }

    @ParameterizedTest(name = "tile({0}, {1}, {2}) is {3}")
    @CsvSource({
        "-1,  0,  0, BOUNDS",  // just past each edge of the 3x3x1 world
        " 3,  0,  0, BOUNDS",
        " 0, -1,  0, BOUNDS",
        " 0,  3,  0, BOUNDS",
        " 0,  0, -1, BOUNDS",
        " 0,  0,  1, BOUNDS",
        " 0,  0,  0, FLOOR",   // lowest valid x, y and z
        " 2,  0,  0, FLOOR",   // highest valid x
        " 0,  2,  0, FLOOR",   // highest valid y
        " 2,  2,  0, WALL"     // stored tile is returned as-is
    })
    void tileReturnsBoundsOnlyOutsideTheWorld(int x, int y, int z, Tile expected) {
        assertEquals(expected, world.tile(x, y, z));
    }

    @Test
    void itemDroppedOnAnEmptyTileIsPlacedThere() {
        Item rock = new Item(',', Color.GRAY, "rock", "grey rock");

        boolean placed = world.addAtEmptySpace(rock, 1, 1, 0);

        assertTrue(placed);
        assertSame(rock, world.item(1, 1, 0));
    }

    @Test
    void itemDroppedOnACreatureLandsAtItsFeetAndNotifiesIt() {
        Creature creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);
        creature.x = 1;
        creature.y = 1;
        creature.z = 0;
        List<String> messages = recordMessages(creature);
        world.add(creature);
        Item rock = new Item(',', Color.GRAY, "rock", "grey rock");

        boolean placed = world.addAtEmptySpace(rock, 1, 1, 0);

        assertTrue(placed);
        assertSame(rock, world.item(1, 1, 0));
        assertEquals(List.of("A grey rock lands between your feet."), messages);
    }
}
