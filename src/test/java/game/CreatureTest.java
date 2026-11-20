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

    // ---------------------------------------------------------------
    // attackValue() / defenseValue(): base stats plus weapon and armor
    // The creature from setUp has base attack 10 and defense 5.
    // ---------------------------------------------------------------

    private static Item gear(String name, int attack, int defense) {
        Item item = new Item(')', Color.WHITE, name, null);
        item.modifyAttackValue(attack);
        item.modifyDefenseValue(defense);
        return item;
    }

    /** Same stats as StuffFactory.newStaff(): attack >= defense, so equip() wields it as a weapon. */
    private static Item staff() {
        return gear("staff", 5, 3);
    }

    /** Armor with a small attack bonus: attack < defense, so equip() wears it as armor. */
    private static Item spikedArmor() {
        return gear("spiked armor", 1, 4);
    }

    @Test
    void attackAndDefenseAreBaseStatsWhenNothingIsEquipped() {
        assertNull(creature.weapon());
        assertNull(creature.armor());

        assertEquals(10, creature.attackValue());
        assertEquals(5, creature.defenseValue());
    }

    @Test
    void modifyingBaseStatsChangesAttackAndDefense() {
        // Same changes a potion of warrior makes when it starts and ends.
        creature.modifyAttackValue(5);
        creature.modifyDefenseValue(5);
        assertEquals(15, creature.attackValue());
        assertEquals(10, creature.defenseValue());

        creature.modifyAttackValue(-5);
        creature.modifyDefenseValue(-5);
        assertEquals(10, creature.attackValue());
        assertEquals(5, creature.defenseValue());
    }

    @Test
    void equippedWeaponAddsItsAttackAndDefenseBonuses() {
        attachRecordingAiAndAddToWorld();
        Item staff = staff();

        creature.equip(staff);

        assertSame(staff, creature.weapon());
        assertNull(creature.armor());
        assertEquals(10 + 5, creature.attackValue());
        assertEquals(5 + 3, creature.defenseValue());
    }

    @Test
    void equippedArmorAddsItsAttackAndDefenseBonuses() {
        attachRecordingAiAndAddToWorld();
        Item armor = spikedArmor();

        creature.equip(armor);

        assertNull(creature.weapon());
        assertSame(armor, creature.armor());
        assertEquals(10 + 1, creature.attackValue());
        assertEquals(5 + 4, creature.defenseValue());
    }

    @Test
    void weaponAndArmorBonusesStackWithBaseStats() {
        attachRecordingAiAndAddToWorld();

        creature.equip(staff());
        creature.equip(spikedArmor());

        assertEquals(10 + 5 + 1, creature.attackValue());
        assertEquals(5 + 3 + 4, creature.defenseValue());
    }

    @Test
    void unequippingRemovesTheBonuses() {
        attachRecordingAiAndAddToWorld();
        Item staff = staff();
        Item armor = spikedArmor();
        creature.equip(staff);
        creature.equip(armor);

        creature.unequip(staff);
        creature.unequip(armor);

        assertNull(creature.weapon());
        assertNull(creature.armor());
        assertEquals(10, creature.attackValue());
        assertEquals(5, creature.defenseValue());
    }

    // ---------------------------------------------------------------
    // modifyMana(): mana stays between 0 and maxMana
    // A new creature has maxMana 5 and starts with full mana.
    // ---------------------------------------------------------------

    @Test
    void newCreatureStartsWithFullMana() {
        assertEquals(5, creature.maxMana());
        assertEquals(5, creature.mana());
    }

    @ParameterizedTest(name = "mana {0} + ({1}) -> {2}")
    @CsvSource({
        "5, -1, 4",  // spend some
        "5, -5, 0",  // spend exactly all of it
        "5, -6, 0",  // overspending stops at 0
        "5,  0, 5",  // no change
        "5,  1, 5",  // already full: cannot go above maxMana
        "2,  2, 4",  // restore some
        "2,  3, 5",  // restore exactly to maxMana
        "2,  4, 5",  // over-restoring stops at maxMana
        "0, -1, 0",  // already empty stays at 0
        "0,  1, 1"   // restore from empty
    })
    void modifyManaStaysBetweenZeroAndMaxMana(int startingMana, int amount, int expectedMana) {
        creature.modifyMana(startingMana - creature.mana());
        assertEquals(startingMana, creature.mana(), "setup: starting mana");

        creature.modifyMana(amount);

        assertEquals(expectedMana, creature.mana());
    }

    @Test
    void manaCapFollowsMaxMana() {
        creature.modifyMaxMana(5);  // maxMana 5 -> 10, mana still 5

        creature.modifyMana(3);
        assertEquals(8, creature.mana());

        creature.modifyMana(10);
        assertEquals(10, creature.mana());
    }
}
