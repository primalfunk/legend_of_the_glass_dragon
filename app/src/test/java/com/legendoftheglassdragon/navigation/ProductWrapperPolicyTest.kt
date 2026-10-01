package com.legendoftheglassdragon.navigation

import com.legendoftheglassdragon.data.SaveFailure
import com.legendoftheglassdragon.data.SaveLoadResult
import com.legendoftheglassdragon.game.CombatMessage
import com.legendoftheglassdragon.game.CombatMessageKind
import com.legendoftheglassdragon.game.CombatState
import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.DayTransitionPhase
import com.legendoftheglassdragon.game.DayTransitionState
import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonAttemptRecord
import com.legendoftheglassdragon.game.DragonSeasonState
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.SeasonConclusionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductWrapperPolicyTest {
    @Test
    fun noSaveShowsNewGameWithoutDestructiveConfirmation() {
        val menu = ProductWrapperPolicy.mainMenuState(
            SaveLoadResult.Missing,
            SaveLoadResult.Missing,
        )

        assertEquals(CampaignSlotState.NONE, menu.campaign)
        assertFalse(menu.showContinue)
        assertFalse(menu.conclusionPending)
        assertFalse(menu.showSeasonRecord)
        assertFalse(menu.newGameNeedsConfirmation)
    }

    @Test
    fun activeSaveShowsContinueAndRequiresConfirmedReplacement() {
        val active = SaveLoadResult.Loaded(newGame())
        val menu = ProductWrapperPolicy.mainMenuState(active, SaveLoadResult.Missing)

        assertEquals(CampaignSlotState.ACTIVE, menu.campaign)
        assertTrue(menu.showContinue)
        assertFalse(menu.conclusionPending)
        assertTrue(menu.newGameNeedsConfirmation)
    }

    @Test
    fun completedSaveShowsRecordButCannotContinueIntoOrdinaryPlay() {
        val completed = completedGame()
        val menu = ProductWrapperPolicy.mainMenuState(
            SaveLoadResult.Loaded(completed),
            SaveLoadResult.Missing,
        )

        assertEquals(CampaignSlotState.COMPLETED, menu.campaign)
        assertFalse(menu.showContinue)
        assertFalse(menu.conclusionPending)
        assertTrue(menu.showSeasonRecord)
        assertFalse(menu.newGameNeedsConfirmation)
        assertEquals(AppRoute.SeasonRecord, ProductWrapperPolicy.resumeRoute(completed))
    }

    @Test
    fun interruptedResolvedSeasonContinuesItsConclusionWithoutEnteringTown() {
        val interrupted = completedGame().copy(
            season = completedGame().season.copy(
                conclusionPhase = SeasonConclusionPhase.FINAL_CHRONICLE,
            ),
        )
        val menu = ProductWrapperPolicy.mainMenuState(
            SaveLoadResult.Loaded(interrupted),
            SaveLoadResult.Loaded(interrupted),
        )

        assertTrue(menu.showContinue)
        assertTrue(menu.conclusionPending)
        assertTrue(menu.newGameNeedsConfirmation)
        assertEquals(AppRoute.FinalChronicle, ProductWrapperPolicy.resumeRoute(interrupted))
    }

    @Test
    fun archivedCompletedSeasonRemainsAvailableBesideFreshActiveSeason() {
        val active = newGame("New Hero")
        val archive = completedGame("Old Hero")

        val menu = ProductWrapperPolicy.mainMenuState(
            SaveLoadResult.Loaded(active),
            SaveLoadResult.Loaded(archive),
        )

        assertTrue(menu.showContinue)
        assertTrue(menu.showSeasonRecord)
        assertSame(
            archive,
            ProductWrapperPolicy.seasonRecordState(
                SaveLoadResult.Loaded(active),
                SaveLoadResult.Loaded(archive),
            ),
        )
    }

    @Test
    fun continueRestoresPersistedEncounterAndTransitionPhasesExactly() {
        val base = newGame()
        val combat = base.copy(
            activeCombat = CombatState(
                monsterId = "giant_rat",
                monsterCurrentHp = 5,
                status = CombatStatus.ACTIVE,
                fightConsumed = true,
                recentMessages = listOf(CombatMessage("A rat appears.", CombatMessageKind.ENCOUNTER)),
                escapeAvailable = true,
                rewardsGranted = false,
            ),
        )
        val chronicle = base.copy(
            pendingDayTransition = DayTransitionState(1, 2, DayTransitionPhase.CHRONICLE),
        )
        val dawn = base.copy(
            pendingDayTransition = DayTransitionState(1, 2, DayTransitionPhase.DAWN_BRIEF),
        )

        assertEquals(AppRoute.Combat, ProductWrapperPolicy.resumeRoute(combat))
        assertEquals(AppRoute.GlassChronicle, ProductWrapperPolicy.resumeRoute(chronicle))
        assertEquals(AppRoute.DawnBrief, ProductWrapperPolicy.resumeRoute(dawn))
        assertEquals(AppRoute.Town, ProductWrapperPolicy.resumeRoute(base))
    }

    @Test
    fun unreadableActiveSlotRequiresConfirmationAndNeverPretendsContinueWorks() {
        val invalid = SaveLoadResult.Invalid(SaveFailure.Corrupt("broken"))
        val menu = ProductWrapperPolicy.mainMenuState(invalid, SaveLoadResult.Missing)

        assertEquals(CampaignSlotState.INVALID, menu.campaign)
        assertFalse(menu.showContinue)
        assertTrue(menu.newGameNeedsConfirmation)
    }

    private fun newGame(name: String = "Arcturus"): GameState = (
        NewGameFactory(idProvider = { "player-id" }, seedProvider = { 41L }).create(name)
            as NewGameCreationResult.Created
        ).gameState

    private fun completedGame(name: String = "Arcturus"): GameState {
        val state = newGame(name)
        return state.copy(
            season = DragonSeasonState(
                stage = DragonStage.RESOLVED,
                pressure = 100,
                stageStartedDay = 1,
                attempts = listOf(
                    DragonAttemptRecord(
                        id = "dragon-victory-1",
                        day = 1,
                        challengerId = state.player.id,
                        outcome = DragonAttemptOutcome.VICTORY,
                        challengerPower = 140,
                        dragonPower = 125,
                    ),
                ),
                playerLastAttemptDay = 1,
                victorId = state.player.id,
                resolvedDay = 1,
                conclusionPhase = SeasonConclusionPhase.COMPLETE,
            ),
        )
    }
}
