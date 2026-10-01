package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstPlayableLoopTest {
    @Test
    fun freshCharacterCanReachLevelTwoWithinOneDayEvenOnMinimumRewards() {
        val zeroRandom = GameRandom { world, _ ->
            RandomRoll(0, world.copy(randomStep = world.randomStep + 1))
        }
        val engine = GameEngine(zeroRandom)
        var state = (
            NewGameFactory(
                idProvider = { "player-001" },
                seedProvider = { 42L },
            ).create("Arcturus") as NewGameCreationResult.Created
            ).gameState

        repeat(9) {
            state = engine.startForestEncounter(state, state.revision).applied()
            while (state.activeCombat?.status == CombatStatus.ACTIVE) {
                state = engine.attack(state, state.revision).applied()
            }
            assertEquals(CombatStatus.VICTORY, state.activeCombat?.status)
            state = engine.leaveCompletedCombat(state, state.revision).applied()
        }

        assertEquals(54, state.player.experience)
        assertEquals(1, state.playerDay.fightsRemaining)
        assertTrue(state.player.currentHp > 0)
        assertTrue(ProgressionRules.isTrainingEligible(state.player))

        state = engine.train(state, state.revision).applied()
        assertEquals(2, state.player.level)
        assertEquals(25, state.player.currentHp)
        assertEquals(6, state.player.attack)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
    }

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}
