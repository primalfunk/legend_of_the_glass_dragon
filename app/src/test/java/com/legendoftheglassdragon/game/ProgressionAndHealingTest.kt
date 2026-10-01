package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionAndHealingTest {
    private val engine = GameEngine()

    @Test
    fun experienceThresholdCreatesTrainingEligibilityWithoutAutomaticLevelUp() {
        val state = newGame().let {
            it.copy(player = it.player.copy(experience = 50))
        }

        assertEquals(1, state.player.level)
        assertTrue(ProgressionRules.isTrainingEligible(state.player))
        assertEquals(0, ProgressionRules.experienceStillNeeded(state.player))
    }

    @Test
    fun successfulTrainingRaisesOneLevelAndPersistentCoreStats() {
        val original = newGame().let {
            it.copy(
                player = it.player.copy(
                    experience = 50,
                    currentHp = 4,
                ),
            )
        }

        val trained = (engine.train(original, original.revision) as GameActionResult.Applied).gameState

        assertEquals(2, trained.player.level)
        assertEquals(50, trained.player.experience)
        assertEquals(25, trained.player.maximumHp)
        assertEquals(25, trained.player.currentHp)
        assertEquals(6, trained.player.attack)
        assertEquals(3, trained.player.defense)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(trained))
    }

    @Test
    fun trainingWhenNotReadyIsExplainedAndStateUnchanged() {
        val state = newGame()

        assertEquals(
            GameActionResult.Rejected(GameActionRejection.TrainingNotReady(50)),
            engine.train(state, state.revision),
        )
        assertFalse(ProgressionRules.isTrainingEligible(state.player))
    }

    @Test
    fun staleTrainingConfirmationCannotGainTwoLevels() {
        val original = newGame().let {
            it.copy(player = it.player.copy(experience = 200))
        }
        val capturedRevision = original.revision
        val levelTwo = (engine.train(original, capturedRevision) as GameActionResult.Applied).gameState

        val duplicate = engine.train(levelTwo, capturedRevision)

        assertEquals(GameActionResult.Rejected(GameActionRejection.StaleState), duplicate)
        assertEquals(2, levelTwo.player.level)
    }

    @Test
    fun healerRestoresExactlyMissingHpAndChargesGold() {
        val original = newGame().let {
            it.copy(player = it.player.copy(currentHp = 11))
        }

        val healed = (engine.healFully(original, original.revision) as GameActionResult.Applied).gameState

        assertEquals(20, healed.player.currentHp)
        assertEquals(16, healed.player.gold)
        assertTrue(healed.player.currentHp <= healed.player.maximumHp)
        assertTrue(healed.player.gold >= 0)
    }

    @Test
    fun healerRefusesFullHealthAndUnaffordableTreatment() {
        val healthy = newGame()
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NoHealingNeeded),
            engine.healFully(healthy, healthy.revision),
        )

        val poor = healthy.copy(player = healthy.player.copy(currentHp = 1, gold = 0))
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NotEnoughGold(19)),
            engine.healFully(poor, poor.revision),
        )
        assertEquals(0, poor.player.gold)
        assertEquals(1, poor.player.currentHp)
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "player-001" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState
}
