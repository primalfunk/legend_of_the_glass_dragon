package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldAdvancerTest {
    @Test
    fun nextDayIncrementsOnceResetsFightsHealsAndPreservesProgression() {
        val original = newGame().let {
            it.copy(
                player = it.player.copy(
                    experience = 30,
                    currentHp = 3,
                    gold = 41,
                ),
                playerDay = it.playerDay.copy(fightsRemaining = 2),
            )
        }

        val next = (
            WorldAdvancer.advanceWorldToNextDay(original, original.revision)
                as GameActionResult.Applied
            ).gameState

        assertEquals(2, next.currentDay)
        assertEquals(2, next.playerDay.currentDay)
        assertEquals(GameBalance.FOREST_FIGHTS_PER_DAY, next.playerDay.fightsRemaining)
        assertEquals(next.player.maximumHp, next.player.currentHp)
        assertEquals(30, next.player.experience)
        assertEquals(41, next.player.gold)
        assertEquals(original.player.level, next.player.level)
    }

    @Test
    fun staleEndDayTapCannotAdvanceTwice() {
        val original = newGame()
        val capturedRevision = original.revision
        val dayTwo = (
            WorldAdvancer.advanceWorldToNextDay(original, capturedRevision)
                as GameActionResult.Applied
            ).gameState

        val duplicate = WorldAdvancer.advanceWorldToNextDay(dayTwo, capturedRevision)

        assertEquals(GameActionResult.Rejected(GameActionRejection.StaleState), duplicate)
        assertEquals(2, dayTwo.currentDay)
    }

    @Test
    fun campaignDayTransitionsRemainLawfulAndTerminate() {
        var state = newGame()
        var transitions = 0

        while (state.season.stage != DragonStage.RESOLVED && transitions < 60) {
            state = (
                WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(state, state.revision)
                    as GameActionResult.Applied
                ).gameState
            transitions++
            assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
            assertEquals(GameBalance.FOREST_FIGHTS_PER_DAY, state.playerDay.fightsRemaining)
            assertTrue(state.player.currentHp in 1..state.player.maximumHp)
            assertTrue(state.player.gold >= 0)
            assertTrue(state.player.experience >= 0)
        }

        assertEquals(DragonStage.RESOLVED, state.season.stage)
        assertTrue(transitions in 15..60)
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "player-001" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState
}
