package com.legendoftheglassdragon.simulation

import com.legendoftheglassdragon.game.CommunityFactory
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.GameRandom
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.NpcPersonality
import com.legendoftheglassdragon.game.NpcStatus
import com.legendoftheglassdragon.game.RandomRoll
import com.legendoftheglassdragon.game.WorldEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunitySimulatorTest {
    @Test
    fun everyNpcAdvancesAtMostOnceAndEventsStayCurated() {
        val game = newGame()
        val simulator = CommunitySimulator(ConstantGameRandom(50))

        val once = simulator.advance(game.community, game.player, game.world, 1)
        val twice = simulator.advance(once.community, game.player, once.world, 1)

        assertTrue(once.community.adventurers.all { it.lastAdvancedDay == 1 })
        assertEquals(once.community, twice.community)
        assertEquals(once.world, twice.world)
        assertTrue(twice.transitionEvents.isEmpty())
        assertTrue(once.community.recentEvents.size <= CommunityRules.MAX_DAILY_EVENTS)
    }

    @Test
    fun successfulAdventuringGrantsExperienceAndGold() {
        val game = newGame()
        val before = game.community.adventurers.associateBy { it.id }

        val result = CommunitySimulator(ConstantGameRandom(50))
            .advance(game.community, game.player, game.world, 1)

        assertTrue(result.community.adventurers.all {
            it.experience > requireNotNull(before[it.id]).experience
        })
        assertTrue(result.community.adventurers.all { it.gold >= 0 })
    }

    @Test
    fun defeatCreatesRecoveryAndNextDayReturnsNpcToActive() {
        val game = newGame()
        val defeated = CommunitySimulator(ConstantGameRandom(0))
            .advance(game.community, game.player, game.world, 1)

        assertTrue(defeated.community.adventurers.all { it.status == NpcStatus.RECOVERING })
        assertTrue(defeated.community.recentEvents.any { it.type == WorldEventType.DEFEAT })

        val recovered = CommunitySimulator(ConstantGameRandom(50))
            .advance(defeated.community, game.player, defeated.world, 2)

        assertTrue(recovered.community.adventurers.all { it.status == NpcStatus.ACTIVE })
        assertTrue(recovered.community.adventurers.all { it.recoveryDaysRemaining == 0 })
        assertTrue(recovered.community.recentEvents.any {
            it.day == 2 && it.type == WorldEventType.RECOVERY
        })
    }

    @Test
    fun eligibleNpcCanTrainOnlyOneLevelDuringOneDay() {
        val game = newGame()
        val ambitious = game.community.adventurers.mapIndexed { index, npc ->
            if (index == 0) {
                npc.copy(experience = 30, personality = NpcPersonality.AMBITIOUS)
            } else {
                npc
            }
        }
        val community = game.community.copy(adventurers = ambitious)

        val advanced = CommunitySimulator(ConstantGameRandom(50))
            .advance(community, game.player, game.world, 1).community
        val trained = advanced.adventurers.first()

        assertEquals(2, trained.level)
        assertEquals(ambitious.first().maximumHp + 5, trained.maximumHp)
        assertTrue(advanced.recentEvents.any {
            it.characterId == trained.id && it.type == WorldEventType.LEVEL_GAIN
        })
    }

    @Test
    fun earnedGoldCanFundOneEquipmentUpgradeAndNoNpcGoesNegative() {
        val game = newGame()
        val npcs = game.community.adventurers.mapIndexed { index, npc ->
            when (index) {
                0 -> npc.copy(gold = 100, personality = NpcPersonality.GREEDY)
                1 -> npc.copy(gold = 0, personality = NpcPersonality.GREEDY)
                else -> npc
            }
        }

        val advanced = CommunitySimulator(ConstantGameRandom(50))
            .advance(game.community.copy(adventurers = npcs), game.player, game.world, 1)
            .community.adventurers
        val wealthy = advanced[0]
        val poor = advanced[1]

        assertEquals("Studded Leather", wealthy.armor)
        assertTrue(wealthy.defense >= npcs[0].defense + 1)
        assertEquals(105, wealthy.gold)
        assertTrue(wealthy.gold >= 0)
        assertEquals("Studded Leather", poor.armor)
        assertTrue(poor.gold >= 0)
    }

    @Test
    fun realProgressChangesCanGenerateRankAndLevelNews() {
        val game = newGame()
        val challenger = game.community.adventurers.mapIndexed { index, npc ->
            when (index) {
                0 -> npc.copy(experience = 49, personality = NpcPersonality.AMBITIOUS)
                1 -> npc.copy(
                    level = 2,
                    experience = 30,
                    maximumHp = npc.maximumHp + 5,
                    baseAttack = npc.baseAttack + 1,
                    baseDefense = npc.baseDefense + 1,
                    personality = NpcPersonality.CAUTIOUS,
                )
                else -> npc
            }
        }

        val result = CommunitySimulator(ConstantGameRandom(50)).advance(
            game.community.copy(adventurers = challenger),
            game.player,
            game.world,
            1,
        )

        assertTrue(result.community.recentEvents.any { it.type == WorldEventType.LEVEL_GAIN })
        assertTrue(result.transitionEvents.any { it.type == WorldEventType.RANK_CHANGE })
        assertTrue(result.community.recentEvents.all { it.day == 1 })
    }

    private fun newGame() = (
        NewGameFactory(
            idProvider = { "player-001" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState
}

private class ConstantGameRandom(
    private val value: Int,
) : GameRandom {
    override fun nextInt(
        world: com.legendoftheglassdragon.game.WorldState,
        bound: Int,
    ): RandomRoll = RandomRoll(
        value = value.coerceIn(0, bound - 1),
        world = world.copy(randomStep = world.randomStep + 1),
    )
}
