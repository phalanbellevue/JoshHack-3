package game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TileTest {

    @ParameterizedTest(name = "{0} is not ground")
    @EnumSource(value = Tile.class, names = { "WALL", "BOUNDS" })
    void wallsAndTheWorldEdgeAreNotGround(Tile tile) {
        assertFalse(tile.isGround());
    }

    @ParameterizedTest(name = "{0} is ground")
    @EnumSource(value = Tile.class, names = { "FLOOR", "STAIRS_DOWN", "STAIRS_UP" })
    void floorsAndStairsAreGround(Tile tile) {
        assertTrue(tile.isGround());
    }
}
