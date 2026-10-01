package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityFactoryAndRankingsTest {
    @Test
    fun communityCreationIsDeterministicVariedAndLawful() {
        val first = CommunityFactory.create(42L)
        val again = CommunityFactory.create(42L)

        assertEquals(CommunityFactory.POPULATION_SIZE, first.adventurers.size)
        assertEquals(first, again)
        assertEquals(first.adventurers.size, first.adventurers.map { it.id }.distinct().size)
        assertEquals(first.adventurers.size, first.adventurers.map { it.name }.distinct().size)
        assertEquals(NpcPersonality.entries.toSet(), first.adventurers.map { it.personality }.toSet())
        assertTrue(first.adventurers.all { it.level == 1 && it.gold >= 0 && it.maximumHp > 0 })
        assertNotEquals(
            first.adventurers.map { it.name },
            CommunityFactory.create(43L).adventurers.map { it.name },
        )
    }

    @Test
    fun rankingsIncludeEveryoneOnceAndUseLevelExperienceThenStableId() {
        val state = newGame()
        val modified = state.community.adventurers.mapIndexed { index, npc ->
            when (index) {
                0 -> npc.copy(level = 2, experience = 31)
                1 -> npc.copy(level = 2, experience = 35)
                2 -> npc.copy(level = 2, experience = 35)
                else -> npc
            }
        }
        val rankedState = state.copy(community = state.community.copy(adventurers = modified))

        val rankings = CommunityRules.rankings(rankedState)

        assertEquals(CommunityFactory.POPULATION_SIZE + 1, rankings.size)
        assertEquals(rankings.size, rankings.map { it.characterId }.distinct().size)
        assertEquals(modified[1].id, rankings[0].characterId)
        assertEquals(modified[2].id, rankings[1].characterId)
        assertEquals(1, rankings.count { it.isPlayer })
    }

    @Test
    fun innVisitorsAreStableForTheGameDayAndReferenceRealNpcs() {
        val state = newGame()

        val first = CommunityRules.innVisitors(state)
        val second = CommunityRules.innVisitors(state)

        assertEquals(first, second)
        assertEquals(CommunityRules.INN_VISITOR_COUNT, first.size)
        assertTrue(first.all { visitor -> state.community.adventurers.any { it.id == visitor.id } })
    }

    @Test
    fun innMeetingRaisesFamiliarityOnlyOncePerDay() {
        val state = newGame()
        val visitorIds = CommunityRules.innVisitors(state).map { it.id }.toSet()

        val met = (
            CommunityActions.recordInnMeeting(state, state.revision, visitorIds)
                as GameActionResult.Applied
            ).gameState
        val repeated = CommunityActions.recordInnMeeting(met, met.revision, visitorIds)

        assertTrue(met.community.adventurers.filter { it.id in visitorIds }.all {
            it.familiarity == 1 && it.lastMetDay == 1
        })
        assertEquals(GameActionResult.Rejected(GameActionRejection.NoCommunityChange), repeated)
    }

    @Test
    fun chronicleAndDawnAcknowledgementsArePersistentAndAtomic() {
        val state = newGame()
        val advanced = (
            WorldAdvancer.advanceWorldToNextDay(state, state.revision)
                as GameActionResult.Applied
            ).gameState
        val capturedRevision = advanced.revision

        val dawn = (
            CommunityActions.advanceToDawnBrief(advanced, capturedRevision)
                as GameActionResult.Applied
            ).gameState
        val staleDuplicate = CommunityActions.advanceToDawnBrief(
            dawn,
            capturedRevision,
        )
        val completed = (
            CommunityActions.completeDayTransition(dawn, dawn.revision)
                as GameActionResult.Applied
            ).gameState

        assertEquals(DayTransitionPhase.CHRONICLE, advanced.pendingDayTransition?.phase)
        assertEquals(DayTransitionPhase.DAWN_BRIEF, dawn.pendingDayTransition?.phase)
        assertEquals(null, completed.pendingDayTransition)
        assertEquals(GameActionResult.Rejected(GameActionRejection.StaleState), staleDuplicate)
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "player-001" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState
}
