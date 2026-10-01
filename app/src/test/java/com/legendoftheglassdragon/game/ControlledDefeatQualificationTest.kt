package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlledDefeatQualificationTest {
    private val engine = GameEngine(ZeroGameRandom)

    @Test
    fun defeatIsLawfulOnFirstMiddleAndFinalDailyFight() {
        listOf(1, 5, 10).forEach { fightNumber ->
            val original = newGame().let { state ->
                state.copy(
                    player = state.player.copy(currentHp = 1, gold = 100, experience = 49),
                    playerDay = state.playerDay.copy(fightsRemaining = 11 - fightNumber),
                )
            }

            val defeated = defeat(original)

            assertEquals(CombatStatus.DEFEAT, defeated.activeCombat?.status)
            assertTrue(defeated.playerDay.wasDefeatedToday)
            assertEquals(0, defeated.playerDay.fightsRemaining)
            assertEquals(75, defeated.player.gold)
            assertEquals(49, defeated.player.experience)
            assertEquals(1, defeated.player.level)
            assertEquals(10, defeated.player.currentHp)
            assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(defeated))
        }
    }

    @Test
    fun goldLossRoundsUpAtTheFloorAndNeverBecomesNegative() {
        val expected = mapOf(0 to 0, 1 to 1, 3 to 1, 4 to 1, 5 to 2, 400 to 100)

        expected.forEach { (startingGold, expectedLoss) ->
            val base = newGame()
            val defeated = defeat(
                base.copy(player = base.player.copy(currentHp = 1, gold = startingGold)),
            )

            assertEquals(expectedLoss, defeated.activeCombat?.goldLost)
            assertEquals(startingGold - expectedLoss, defeated.player.gold)
            assertTrue(defeated.player.gold >= 0)
        }
    }

    @Test
    fun forestRemainsClosedUntilEndDayThenFullCapabilityReturns() {
        val original = newGame().let { state ->
            state.copy(
                player = state.player.copy(currentHp = 1, experience = 49, gold = 80),
            )
        }
        val defeated = defeat(original)
        val townState = engine.leaveCompletedCombat(defeated, defeated.revision).applied()

        assertEquals(null, townState.activeCombat)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NoForestFights),
            engine.startForestEncounter(townState, townState.revision),
        )

        val tomorrow = WorldAdvancer.advanceWorldToNextDay(townState, townState.revision).applied()

        assertEquals(2, tomorrow.currentDay)
        assertEquals(GameBalance.FOREST_FIGHTS_PER_DAY, tomorrow.playerDay.fightsRemaining)
        assertFalse(tomorrow.playerDay.wasDefeatedToday)
        assertEquals(tomorrow.player.maximumHp, tomorrow.player.currentHp)
        assertEquals(49, tomorrow.player.experience)
        assertEquals(60, tomorrow.player.gold)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(tomorrow))
    }

    @Test
    fun defeatBeforeAndAfterTrainingEligibilityRetainsProgression() {
        listOf(49, 50).forEach { experience ->
            val original = newGame().let { state ->
                state.copy(player = state.player.copy(currentHp = 1, experience = experience))
            }

            val defeated = defeat(original)

            assertEquals(experience, defeated.player.experience)
            assertEquals(experience >= 50, ProgressionRules.isTrainingEligible(defeated.player))
        }

        val experienced = levelFourState(
            experience = ProgressionRules.experienceRequiredForLevel(5) - 1,
            gold = 200,
        )
        val defeatedVeteran = defeat(experienced.copy(player = experienced.player.copy(currentHp = 1)))

        assertEquals(4, defeatedVeteran.player.level)
        assertEquals(1_249, defeatedVeteran.player.experience)
        assertEquals(150, defeatedVeteran.player.gold)
        assertEquals(35, defeatedVeteran.player.maximumHp)
        assertEquals(10, defeatedVeteran.player.attack)
        assertEquals(5, defeatedVeteran.player.defense)
    }

    @Test
    fun repeatedDefeatsAcrossDaysNeverReduceLongTermCapability() {
        var state = levelFourState(experience = 1_000, gold = 40)
        val expectedStats = state.player.copy(currentHp = state.player.maximumHp)

        repeat(3) { index ->
            state = state.copy(player = state.player.copy(currentHp = 1))
            state = defeat(state)
            assertTrue(state.playerDay.wasDefeatedToday)
            assertEquals(expectedStats.level, state.player.level)
            assertEquals(expectedStats.experience, state.player.experience)
            assertEquals(expectedStats.maximumHp, state.player.maximumHp)
            assertEquals(expectedStats.attack, state.player.attack)
            assertEquals(expectedStats.defense, state.player.defense)

            state = engine.leaveCompletedCombat(state, state.revision).applied()
            state = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                state,
                state.revision,
            ).applied()
            assertEquals(index + 2, state.currentDay)
            assertEquals(state.player.maximumHp, state.player.currentHp)
            assertFalse(state.playerDay.wasDefeatedToday)
        }

        assertTrue(state.player.gold >= 0)
        assertEquals(expectedStats.level, state.player.level)
        assertEquals(expectedStats.experience, state.player.experience)
    }

    private fun defeat(state: GameState): GameState {
        val encounter = engine.startForestEncounter(state, state.revision).applied()
        return engine.attack(encounter, encounter.revision).applied()
    }

    private fun levelFourState(experience: Int, gold: Int): GameState = newGame().let { state ->
        state.copy(
            player = state.player.copy(
                level = 4,
                experience = experience,
                currentHp = 35,
                maximumHp = 35,
                baseAttack = 10,
                baseDefense = 5,
                gold = gold,
            ),
        )
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "defeat-player" },
            seedProvider = { 81L },
        ).create("Arcturus") as NewGameCreationResult.Created
        ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private object ZeroGameRandom : GameRandom {
    override fun nextInt(world: WorldState, bound: Int): RandomRoll =
        RandomRoll(0, world.copy(randomStep = world.randomStep + 1))
}
