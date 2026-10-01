package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    @Test
    fun encounterConsumesExactlyOneFightAndSelectsLevelAppropriateMonster() {
        val engine = GameEngine(SequenceGameRandom(0))
        val original = newGame()

        val encountered = engine.startForestEncounter(original, original.revision).applied()

        assertEquals(9, encountered.playerDay.fightsRemaining)
        assertTrue(encountered.activeCombat?.fightConsumed == true)
        assertEquals(1, MonsterCatalog.require(encountered.activeCombat!!.monsterId).intendedLevel)
        assertEquals(original.revision + 1, encountered.revision)
    }

    @Test
    fun noEncounterStartsWhenNoFightsRemainAndCounterNeverBecomesNegative() {
        val original = newGame().copy(
            playerDay = newGame().playerDay.copy(fightsRemaining = 0),
        )
        val result = GameEngine(SequenceGameRandom(0))
            .startForestEncounter(original, original.revision)

        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NoForestFights),
            result,
        )
        assertEquals(0, original.playerDay.fightsRemaining)
        assertEquals(null, original.activeCombat)
    }

    @Test
    fun attackDamagesBothCombatantsAndKeepsHpLawful() {
        val engine = GameEngine(SequenceGameRandom(0, 0, 0))
        val encounter = engine.startForestEncounter(newGame(), 0L).applied()

        val attacked = engine.attack(encounter, encounter.revision).applied()

        assertEquals(4, attacked.activeCombat?.monsterCurrentHp)
        assertEquals(19, attacked.player.currentHp)
        assertTrue(attacked.player.currentHp in 0..attacked.player.maximumHp)
    }

    @Test
    fun defeatedMonsterDoesNotRetaliateAndRewardsAreGrantedExactlyOnce() {
        val engine = GameEngine(SequenceGameRandom(0, 0, 0))
        val encounter = engine.startForestEncounter(
            newGame().copy(player = newGame().player.copy(baseAttack = 99)),
            0L,
        ).applied()
        val hpBefore = encounter.player.currentHp

        val victory = engine.attack(encounter, encounter.revision).applied()

        assertEquals(CombatStatus.VICTORY, victory.activeCombat?.status)
        assertEquals(hpBefore, victory.player.currentHp)
        assertEquals(6, victory.player.experience)
        assertEquals(27, victory.player.gold)
        assertTrue(victory.activeCombat?.rewardsGranted == true)

        val secondClaim = engine.attack(victory, victory.revision)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.CombatAlreadyComplete),
            secondClaim,
        )
        assertEquals(6, victory.player.experience)
        assertEquals(27, victory.player.gold)
    }

    @Test
    fun playerDefeatRecoversLawfullyClosesForestAndNeverGrantsRewards() {
        val engine = GameEngine(SequenceGameRandom(3, 0, 0))
        val base = newGame()
        val encounter = engine.startForestEncounter(
            base.copy(player = base.player.copy(currentHp = 1)),
            base.revision,
        ).applied()

        val defeat = engine.attack(encounter, encounter.revision).applied()
        val combat = requireNotNull(defeat.activeCombat)

        assertEquals(CombatStatus.DEFEAT, combat.status)
        assertEquals(10, defeat.player.currentHp)
        assertEquals(0, defeat.playerDay.fightsRemaining)
        assertEquals(18, defeat.player.gold)
        assertEquals(0, defeat.player.experience)
        assertFalse(combat.rewardsGranted)
        assertEquals(7, combat.goldLost)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(defeat))
    }

    @Test
    fun successfulRunEndsEncounterWithoutRefundOrReward() {
        val engine = GameEngine(SequenceGameRandom(0, 0))
        val encounter = engine.startForestEncounter(newGame(), 0L).applied()

        val escaped = engine.runAway(encounter, encounter.revision).applied()

        assertEquals(CombatStatus.ESCAPED, escaped.activeCombat?.status)
        assertEquals(9, escaped.playerDay.fightsRemaining)
        assertEquals(0, escaped.player.experience)
        assertEquals(25, escaped.player.gold)
    }

    @Test
    fun failedRunAllowsOneMonsterAttackAndCombatContinues() {
        val engine = GameEngine(SequenceGameRandom(0, 99, 0))
        val encounter = engine.startForestEncounter(newGame(), 0L).applied()

        val failedRun = engine.runAway(encounter, encounter.revision).applied()

        assertEquals(CombatStatus.ACTIVE, failedRun.activeCombat?.status)
        assertEquals(19, failedRun.player.currentHp)
        assertEquals(9, failedRun.playerDay.fightsRemaining)
    }

    @Test
    fun staleRapidTapCannotResolveASecondAttack() {
        val engine = GameEngine(SequenceGameRandom(0, 0, 0, 0, 0))
        val encounter = engine.startForestEncounter(newGame(), 0L).applied()
        val capturedRevision = encounter.revision
        val firstAttack = engine.attack(encounter, capturedRevision).applied()

        val staleSecondAttack = engine.attack(firstAttack, capturedRevision)

        assertEquals(GameActionResult.Rejected(GameActionRejection.StaleState), staleSecondAttack)
        assertEquals(capturedRevision + 1, firstAttack.revision)
    }

    @Test
    fun searchAgainConsumesOneAdditionalFightButCannotStartAtZero() {
        val engine = GameEngine(SequenceGameRandom(0, 0, 0, 0))
        val encounter = engine.startForestEncounter(
            newGame().copy(player = newGame().player.copy(baseAttack = 99)),
            0L,
        ).applied()
        val victory = engine.attack(encounter, encounter.revision).applied()

        val next = engine.searchAgain(victory, victory.revision).applied()

        assertEquals(8, next.playerDay.fightsRemaining)
        assertEquals(CombatStatus.ACTIVE, next.activeCombat?.status)

        val noFights = victory.copy(playerDay = victory.playerDay.copy(fightsRemaining = 0))
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NoForestFights),
            engine.searchAgain(noFights, noFights.revision),
        )
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "player-001" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private class SequenceGameRandom(vararg values: Int) : GameRandom {
    private val sequence = values.toList()
    private var index = 0

    override fun nextInt(world: WorldState, bound: Int): RandomRoll {
        val value = sequence.getOrElse(index) { 0 }
        index += 1
        return RandomRoll(
            value = value.coerceIn(0, bound - 1),
            world = world.copy(randomStep = world.randomStep + 1),
        )
    }
}
