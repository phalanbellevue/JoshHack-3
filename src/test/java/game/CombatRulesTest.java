package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CombatRulesTest {

    private static final int MAX_HP = 100;

    @ParameterizedTest(name = "[{index}] hp={0}, amount={1} -> {2}, dead={3} ({4})")
    @CsvSource({
       
        "50,  20,  70, false, heal within health range",
        "50,   0,  50, false, no change in health",
        "50, -20,  30, false, normal damage, creature survives",
        "90,  10, 100, false, maxHp capped",
        "90,  11, 100, false, >maxHp, should be capped at 100",
        "10,  -9,   1, false, lowest health possible to be alive",
        "10, -10,   0, true,  highest dead hp",
        "10, -50, -40, true,  overkill, damage higher than health"
    })

    void calculateNewHpAndIsDead(int currentHp, int amount, int expectedHp,
                                 boolean expectedDead, String reason) {
        int actualHp = CombatRules.calculateNewHp(currentHp, MAX_HP, amount);

        assertEquals(expectedHp, actualHp, "hp: " + reason);
        assertEquals(expectedDead, CombatRules.isDead(actualHp), "isDead: " + reason);
    }
}
