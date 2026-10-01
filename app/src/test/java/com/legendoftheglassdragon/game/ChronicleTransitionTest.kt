package com.legendoftheglassdragon.game

import com.legendoftheglassdragon.data.GameSaveRepository
import com.legendoftheglassdragon.data.SaveLoadResult
import com.legendoftheglassdragon.data.SaveWriteResult
import com.legendoftheglassdragon.data.StringSaveStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChronicleTransitionTest {
    @Test
    fun endDayResolvesOnceAndCreatesStructuredChronicleCheckpoint() {
        val start = newGame()
        val advanced = WorldAdvancer.advanceWorldToNextDay(start, start.revision).applied()

        assertEquals(2, advanced.currentDay)
        assertEquals(DayTransitionPhase.CHRONICLE, advanced.pendingDayTransition?.phase)
        assertEquals(1, advanced.community.chronicleHistory.single().day)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.StaleState),
            WorldAdvancer.advanceWorldToNextDay(advanced, start.revision),
        )
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.PendingDayTransition),
            WorldAdvancer.advanceWorldToNextDay(advanced, advanced.revision),
        )
    }

    @Test
    fun lowNoiseIsFilteredWhileSignificantFactsAreRetained() {
        val state = newGame()
        val npc = state.community.adventurers.first()
        val low = event("low", npc.id, WorldEventType.SOCIAL, 1)
        val high = event("high", npc.id, WorldEventType.LEVEL_GAIN, 3, value = 2)

        val selected = ChronicleRules.selectChronicleEvents(listOf(low, high), state.player.id)

        assertEquals(listOf(high), selected)
    }

    @Test
    fun orderingAndNarrativeVariationAreDeterministicWithoutGameplayRandomness() {
        val state = newGame()
        val first = state.community.adventurers[0]
        val second = state.community.adventurers[1]
        val events = listOf(
            event("stable-b", second.id, WorldEventType.DEFEAT, 3),
            event("stable-a", first.id, WorldEventType.DEFEAT, 3),
        )

        val selectedOnce = ChronicleRules.selectChronicleEvents(events, state.player.id)
        val selectedTwice = ChronicleRules.selectChronicleEvents(events.reversed(), state.player.id)
        val textOnce = selectedOnce.map { ChronicleNarrator.format(it, state) }
        val textTwice = selectedTwice.map { ChronicleNarrator.format(it, state) }

        assertEquals(selectedOnce, selectedTwice)
        assertEquals(textOnce, textTwice)
        assertNotEquals(textOnce[0], textOnce[1])
    }

    @Test
    fun dawnExcludesPrivateFactsButIncludesPublicAndDirectConsequences() {
        val state = newGame()
        val npc = state.community.adventurers.first()
        val privateProfit = event(
            "private",
            npc.id,
            WorldEventType.GREAT_SUCCESS,
            2,
            visibility = WorldEventVisibility.CHRONICLE_ONLY,
        )
        val publicDefeat = event(
            "public",
            npc.id,
            WorldEventType.DEFEAT,
            3,
            visibility = WorldEventVisibility.PUBLIC,
        )
        val directRank = event(
            "direct",
            state.player.id,
            WorldEventType.RANK_CHANGE,
            2,
            visibility = WorldEventVisibility.PLAYER_DIRECT,
            directlyAffectsPlayer = true,
        ).copy(previousRank = 3, newRank = 2)
        val record = ChronicleRecord(1, listOf(privateProfit, publicDefeat, directRank))

        val visible = DawnBriefRules.visibleEvents(record, state)

        assertFalse(privateProfit in visible)
        assertTrue(publicDefeat in visible)
        assertTrue(directRank in visible)
        assertTrue(DawnBriefRules.format(publicDefeat, state).contains("seen returning"))
    }

    @Test
    fun skipChangesOnlyPresentationStateNotWorldOutcome() {
        val start = newGame()
        val normalAdvance = WorldAdvancer.advanceWorldToNextDay(start, start.revision).applied()
        val directSkip = CommunityActions.completeDayTransition(
            normalAdvance,
            normalAdvance.revision,
        ).applied()
        val helperSkip = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
            start,
            start.revision,
        ).applied()

        assertEquals(directSkip, helperSkip)
        assertEquals(null, helperSkip.pendingDayTransition)
    }

    @Test
    fun saveRestoreAtChronicleAndDawnNeverRerunsTheDay() {
        val repository = GameSaveRepository(MemoryStorage())
        val chronicle = WorldAdvancer.advanceWorldToNextDay(newGame(), 0).applied()
        assertEquals(SaveWriteResult.Saved, repository.save(chronicle))
        val restoredChronicle = (repository.load() as SaveLoadResult.Loaded).gameState
        assertEquals(chronicle, restoredChronicle)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.PendingDayTransition),
            WorldAdvancer.advanceWorldToNextDay(restoredChronicle, restoredChronicle.revision),
        )

        val dawn = CommunityActions.advanceToDawnBrief(
            restoredChronicle,
            restoredChronicle.revision,
        ).applied()
        assertEquals(SaveWriteResult.Saved, repository.save(dawn))
        val restoredDawn = (repository.load() as SaveLoadResult.Loaded).gameState
        assertEquals(DayTransitionPhase.DAWN_BRIEF, restoredDawn.pendingDayTransition?.phase)
        assertEquals(dawn.community.chronicleHistory, restoredDawn.community.chronicleHistory)
    }

    @Test
    fun chronicleHistoryIsBoundedAndOlderRecordsDoNotMutate() {
        var state = newGame()
        state = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(state, state.revision).applied()
        val firstRecord = state.community.chronicleHistory.single()

        repeat(ChronicleRules.MAX_HISTORY_DAYS + 4) {
            state = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                state.copy(season = DragonSeasonState()),
                state.revision,
            ).applied().copy(season = DragonSeasonState())
        }

        assertEquals(ChronicleRules.MAX_HISTORY_DAYS, state.community.chronicleHistory.size)
        assertEquals(firstRecord, firstRecord.copy())
        assertTrue(state.community.chronicleHistory.none { it.day == firstRecord.day })
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
    }

    @Test
    fun fixedSeedProducesSameChronicleAcrossRepeatedDays() {
        var first = newGame()
        var second = newGame()

        repeat(12) {
            first = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(first, first.revision).applied()
            second = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(second, second.revision).applied()
        }

        assertEquals(first, second)
        assertEquals(first.community.chronicleHistory, second.community.chronicleHistory)
    }

    private fun event(
        id: String,
        characterId: String,
        type: WorldEventType,
        importance: Int,
        value: Int = 0,
        visibility: WorldEventVisibility = WorldEventVisibility.PUBLIC,
        directlyAffectsPlayer: Boolean = false,
    ) = WorldEvent(
        id = id,
        day = 1,
        type = type,
        characterId = characterId,
        value = value,
        importance = importance,
        visibility = visibility,
        location = WorldEventLocation.FOREST,
        directlyAffectsPlayer = directlyAffectsPlayer,
    )

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "player-001" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
        ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private class MemoryStorage : StringSaveStorage {
    private var value: String? = null

    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }

    override fun delete() {
        value = null
    }
}
