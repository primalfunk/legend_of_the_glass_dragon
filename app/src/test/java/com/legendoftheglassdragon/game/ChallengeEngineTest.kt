package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeEngineTest {
    @Test
    fun acceptedChallengeConsumesDailyOpportunityAndVictoryRewardsExactlyOnce() {
        val original = newGame().let { state ->
            state.copy(player = state.player.copy(baseAttack = 99))
        }
        val opponent = CommunityRules.innVisitors(original).first()
        val engine = ChallengeEngine(ChallengeSequenceRandom(0, 0))

        val begun = engine.begin(original, original.revision, opponent.id).applied()
        assertEquals(0, begun.playerDay.challengesRemaining)
        assertEquals(ChallengeStatus.ACTIVE, begun.activeChallenge?.status)

        val victory = engine.attack(begun, begun.revision).applied()
        val updatedOpponent = victory.community.adventurers.first { it.id == opponent.id }
        assertEquals(ChallengeStatus.PLAYER_VICTORY, victory.activeChallenge?.status)
        assertEquals(1, victory.player.challengeWins)
        assertEquals(1, updatedOpponent.playerWinsAgainst)
        assertEquals(1, updatedOpponent.challengeLosses)
        assertTrue(updatedOpponent.rivalry >= GameBalance.CHALLENGE_RIVALRY_WIN_GAIN)
        assertEquals(opponent.gold - victory.activeChallenge!!.goldTransferred, updatedOpponent.gold)
        assertTrue(victory.community.recentEvents.any {
            it.type == WorldEventType.CHALLENGE && it.detail == "PLAYER_WIN"
        })
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(victory))

        assertEquals(
            GameActionResult.Rejected(GameActionRejection.ChallengeAlreadyComplete),
            engine.attack(victory, victory.revision),
        )
    }

    @Test
    fun duelLossIsRecoverableDoesNotCloseForestAndPersistsHistory() {
        var original = newGame()
        val visitor = CommunityRules.innVisitors(original).first()
        val dangerous = visitor.copy(baseAttack = 100, baseDefense = 100, maximumHp = 100, gold = 50)
        original = original.copy(
            player = original.player.copy(currentHp = 1, baseAttack = 0, gold = 25),
            community = original.community.copy(
                adventurers = original.community.adventurers.map {
                    if (it.id == dangerous.id) dangerous else it
                },
            ),
        )
        val engine = ChallengeEngine(ChallengeSequenceRandom(0, 0, 0))
        val begun = engine.begin(original, original.revision, dangerous.id).applied()
        val defeated = engine.attack(begun, begun.revision).applied()
        val updatedOpponent = defeated.community.adventurers.first { it.id == dangerous.id }

        assertEquals(ChallengeStatus.PLAYER_DEFEAT, defeated.activeChallenge?.status)
        assertEquals(defeated.player.maximumHp / 2, defeated.player.currentHp)
        assertEquals(GameBalance.FOREST_FIGHTS_PER_DAY, defeated.playerDay.fightsRemaining)
        assertFalse(defeated.playerDay.wasDefeatedToday)
        assertEquals(1, defeated.player.challengeLosses)
        assertEquals(1, updatedOpponent.playerLossesAgainst)
        assertTrue(defeated.player.gold >= 0)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(defeated))

        val left = engine.leave(defeated, defeated.revision).applied()
        assertTrue(GameEngine(ChallengeSequenceRandom(0)).startForestEncounter(
            left,
            left.revision,
        ) is GameActionResult.Applied)
    }

    @Test
    fun yieldRecordsLossAndDailyLimitPreventsFarming() {
        val original = newGame()
        val opponent = CommunityRules.innVisitors(original).first()
        val engine = ChallengeEngine(ChallengeSequenceRandom(0))
        val begun = engine.begin(original, original.revision, opponent.id).applied()
        val yielded = engine.yield(begun, begun.revision).applied()

        assertEquals(ChallengeStatus.YIELDED, yielded.activeChallenge?.status)
        assertEquals(1, yielded.player.challengeLosses)
        assertEquals(0, yielded.activeChallenge?.goldTransferred)
        val left = engine.leave(yielded, yielded.revision).applied()
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NoChallengesRemaining),
            engine.begin(left, left.revision, opponent.id),
        )
    }

    @Test
    fun refusalLeavesOpportunityAndUnavailableOpponentIsRejected() {
        var original = newGame()
        val visitor = CommunityRules.innVisitors(original).first()
        val cautious = visitor.copy(personality = NpcPersonality.CAUTIOUS)
        original = original.copy(
            community = original.community.copy(
                adventurers = original.community.adventurers.map {
                    if (it.id == cautious.id) cautious else it
                },
            ),
        )
        val engine = ChallengeEngine(ChallengeSequenceRandom(99))
        val refused = engine.begin(original, original.revision, cautious.id).applied()
        assertEquals(ChallengeStatus.REFUSED, refused.activeChallenge?.status)
        assertEquals(GameBalance.CHALLENGES_PER_DAY, refused.playerDay.challengesRemaining)

        val left = engine.leave(refused, refused.revision).applied()
        val absent = left.community.adventurers.first { npc ->
            CommunityRules.innVisitors(left).none { it.id == npc.id }
        }
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.ChallengeOpponentUnavailable),
            engine.begin(left, left.revision, absent.id),
        )
    }

    @Test
    fun activeChallengeBlocksWorldAdvancementAndAtomicMoneyActions() {
        val original = newGame()
        val opponent = CommunityRules.innVisitors(original).first()
        val begun = ChallengeEngine(ChallengeSequenceRandom(0))
            .begin(original, original.revision, opponent.id)
            .applied()

        assertEquals(
            GameActionResult.Rejected(GameActionRejection.ActiveChallenge),
            WorldAdvancer.advanceWorldToNextDay(begun, begun.revision),
        )
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.ActiveChallenge),
            EconomyEngine.deposit(begun, begun.revision, 1),
        )
    }

    @Test
    fun gossipReferencesRivalryWithoutLeakingPrivateBankBalance() {
        var state = newGame().let { it.copy(player = it.player.copy(bankedGold = 987_654)) }
        val rival = state.community.adventurers.first().copy(rivalry = 10)
        state = state.copy(
            community = state.community.copy(
                adventurers = listOf(rival) + state.community.adventurers.drop(1),
            ),
        )
        val gossip = WorldEventFormatter.gossip(state).joinToString(" ")
        assertTrue(gossip.contains(rival.name))
        assertTrue(gossip.contains("rival", ignoreCase = true))
        assertFalse(gossip.contains("987654"))
        assertFalse(gossip.contains("987,654"))
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "challenge-player" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private class ChallengeSequenceRandom(vararg values: Int) : GameRandom {
    private val sequence = values.toList()
    private var index = 0

    override fun nextInt(world: WorldState, bound: Int): RandomRoll {
        val value = sequence.getOrElse(index++) { 0 }.coerceIn(0, bound - 1)
        return RandomRoll(value, world.copy(randomStep = world.randomStep + 1))
    }
}
