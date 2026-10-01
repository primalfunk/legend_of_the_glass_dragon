package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingQualificationTest {
    private val engine = GameEngine(TrainingZeroRandom)

    @Test
    fun campaignThresholdsAreExplicitMonotonicAndFrontLoaded() {
        val expected = listOf(0, 50, 250, 600, 1_250, 2_800, 5_600, 9_500, 14_500, 20_500)
        val actual = (1..10).map(ProgressionRules::experienceRequiredForLevel)

        assertEquals(expected, actual)
        assertTrue(actual.zipWithNext().all { (left, right) -> right > left })
        assertTrue(ProgressionRules.experienceRequiredForLevel(11) > actual.last())
    }

    @Test
    fun earliestLawfulTrainingPromotesOnceAndRejectsStaleConfirmation() {
        val eligible = newGame().let { state ->
            state.copy(player = state.player.copy(experience = 50, currentHp = 3))
        }
        val capturedRevision = eligible.revision

        val promoted = engine.train(eligible, capturedRevision).applied()

        assertEquals(2, promoted.player.level)
        assertEquals(50, promoted.player.experience)
        assertEquals(25, promoted.player.maximumHp)
        assertEquals(25, promoted.player.currentHp)
        assertEquals(6, promoted.player.attack)
        assertEquals(3, promoted.player.defense)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.StaleState),
            engine.train(promoted, capturedRevision),
        )
        assertFalse(ProgressionRules.isTrainingEligible(promoted.player))
    }

    @Test
    fun repeatedPromotionsApplyLinearLawfulGrowthWithoutConsumingExperience() {
        var state = newGame().let { original ->
            original.copy(
                player = original.player.copy(
                    experience = ProgressionRules.experienceRequiredForLevel(5),
                ),
            )
        }

        repeat(4) { promotion ->
            state = engine.train(state, state.revision).applied()
            if (promotion < 3) {
                state = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                    state,
                    state.revision,
                ).applied()
            }
        }

        assertEquals(5, state.player.level)
        assertEquals(1_250, state.player.experience)
        assertEquals(40, state.player.maximumHp)
        assertEquals(40, state.player.currentHp)
        assertEquals(12, state.player.attack)
        assertEquals(6, state.player.defense)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
        assertFalse(ProgressionRules.isTrainingEligible(state.player))
    }

    @Test
    fun promotionIsImmediatelyVisibleAgainstTheSameGoblin() {
        val before = withGoblinCombat(newGame())
        val promotedBase = engine.train(
            newGame().let { it.copy(player = it.player.copy(experience = 50)) },
            0L,
        ).applied()
        val after = withGoblinCombat(promotedBase)

        val beforeAttack = engine.attack(before, before.revision).applied()
        val afterAttack = engine.attack(after, after.revision).applied()

        assertEquals(9, beforeAttack.activeCombat?.monsterCurrentHp)
        assertEquals(7, afterAttack.activeCombat?.monsterCurrentHp)
        assertEquals(18, beforeAttack.player.currentHp)
        assertEquals(24, afterAttack.player.currentHp)
    }

    @Test
    fun endDayWhileEligibleDoesNotApplyOrLosePromotion() {
        val eligible = newGame().let { state ->
            state.copy(player = state.player.copy(experience = 50, currentHp = 2))
        }

        val tomorrow = WorldAdvancer.advanceWorldToNextDay(eligible, eligible.revision).applied()

        assertEquals(1, tomorrow.player.level)
        assertEquals(50, tomorrow.player.experience)
        assertTrue(ProgressionRules.isTrainingEligible(tomorrow.player))
        assertEquals(tomorrow.player.maximumHp, tomorrow.player.currentHp)
    }

    @Test
    fun failedTrainingPreservesExperienceAndDefersRetryUntilTomorrow() {
        val failureEngine = GameEngine(TrainingHighRandom)
        val eligible = newGame().let { state ->
            state.copy(player = state.player.copy(experience = 50))
        }

        val failed = failureEngine.train(eligible, eligible.revision).applied()

        assertEquals(1, failed.player.level)
        assertEquals(50, failed.player.experience)
        assertTrue(failed.playerDay.trainingAttemptedToday)
        assertEquals(false, failed.playerDay.lastTrainingSucceeded)
        assertTrue(failed.player.currentHp in 1 until failed.player.maximumHp)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.TrainingAttemptUsedToday),
            failureEngine.train(failed, failed.revision),
        )
        val tomorrow = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
            failed,
            failed.revision,
        ).applied()
        assertFalse(tomorrow.playerDay.trainingAttemptedToday)
        assertEquals(null, tomorrow.playerDay.lastTrainingSucceeded)
        assertTrue(ProgressionRules.isTrainingEligible(tomorrow.player))
    }

    private fun withGoblinCombat(state: GameState): GameState = state.copy(
        playerDay = state.playerDay.copy(fightsRemaining = 9),
        activeCombat = CombatState(
            monsterId = "goblin",
            monsterCurrentHp = MonsterCatalog.require("goblin").maximumHp,
            status = CombatStatus.ACTIVE,
            fightConsumed = true,
            recentMessages = listOf(
                CombatMessage("A Goblin leaps from behind a fallen tree!", CombatMessageKind.ENCOUNTER),
            ),
            escapeAvailable = true,
            rewardsGranted = false,
        ),
    )

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "training-player" },
            seedProvider = { 72L },
        ).create("Arcturus") as NewGameCreationResult.Created
        ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private object TrainingZeroRandom : GameRandom {
    override fun nextInt(world: WorldState, bound: Int): RandomRoll =
        RandomRoll(0, world.copy(randomStep = world.randomStep + 1))
}

private object TrainingHighRandom : GameRandom {
    override fun nextInt(world: WorldState, bound: Int): RandomRoll =
        RandomRoll(bound - 1, world.copy(randomStep = world.randomStep + 1))
}
