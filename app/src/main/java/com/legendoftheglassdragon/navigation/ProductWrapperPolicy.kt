package com.legendoftheglassdragon.navigation

import com.legendoftheglassdragon.data.SaveLoadResult
import com.legendoftheglassdragon.game.DayTransitionPhase
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.SeasonConclusionPhase

enum class CampaignSlotState {
    NONE,
    ACTIVE,
    COMPLETED,
    INVALID,
}

data class MainMenuState(
    val campaign: CampaignSlotState,
    val showContinue: Boolean,
    val conclusionPending: Boolean,
    val showSeasonRecord: Boolean,
    val newGameNeedsConfirmation: Boolean,
)

/** Pure product-wrapper decisions shared by UI and JVM qualification. */
object ProductWrapperPolicy {
    fun mainMenuState(
        activeSave: SaveLoadResult,
        completedSeason: SaveLoadResult,
    ): MainMenuState {
        val campaign = when (activeSave) {
            SaveLoadResult.Missing -> CampaignSlotState.NONE
            is SaveLoadResult.Invalid -> CampaignSlotState.INVALID
            is SaveLoadResult.Loaded -> if (
                activeSave.gameState.season.stage == DragonStage.RESOLVED
            ) {
                CampaignSlotState.COMPLETED
            } else {
                CampaignSlotState.ACTIVE
            }
        }
        val activeCompleted = campaign == CampaignSlotState.COMPLETED
        val activeState = (activeSave as? SaveLoadResult.Loaded)?.gameState
        val conclusionPending = activeState?.season?.let { season ->
            season.stage == DragonStage.RESOLVED &&
                season.conclusionPhase != SeasonConclusionPhase.COMPLETE
        } == true
        val archiveCompleted = (completedSeason as? SaveLoadResult.Loaded)
            ?.gameState
            ?.season
            ?.stage == DragonStage.RESOLVED
        return MainMenuState(
            campaign = campaign,
            showContinue = campaign == CampaignSlotState.ACTIVE || conclusionPending,
            conclusionPending = conclusionPending,
            showSeasonRecord = activeCompleted || archiveCompleted,
            newGameNeedsConfirmation = campaign == CampaignSlotState.ACTIVE || conclusionPending ||
                campaign == CampaignSlotState.INVALID,
        )
    }

    fun resumeRoute(state: GameState): AppRoute = when {
        state.activeCombat != null -> AppRoute.Combat
        state.activeChallenge != null -> AppRoute.Challenge
        state.activeDragonCombat != null -> AppRoute.DragonCombat
        state.pendingDayTransition?.phase == DayTransitionPhase.CHRONICLE ->
            AppRoute.GlassChronicle
        state.pendingDayTransition?.phase == DayTransitionPhase.DAWN_BRIEF ->
            AppRoute.DawnBrief
        state.season.stage == DragonStage.RESOLVED -> conclusionRoute(state)
        else -> AppRoute.Town
    }

    fun seasonRecordState(
        activeSave: SaveLoadResult,
        completedSeason: SaveLoadResult,
    ): GameState? {
        val active = (activeSave as? SaveLoadResult.Loaded)?.gameState
        if (active?.season?.stage == DragonStage.RESOLVED) return active
        return (completedSeason as? SaveLoadResult.Loaded)
            ?.gameState
            ?.takeIf { it.season.stage == DragonStage.RESOLVED }
    }

    private fun conclusionRoute(state: GameState): AppRoute = when (state.season.conclusionPhase) {
        SeasonConclusionPhase.VICTORY_NOTICE -> AppRoute.DragonVictoryNotice
        SeasonConclusionPhase.FINAL_CHRONICLE -> AppRoute.FinalChronicle
        SeasonConclusionPhase.SEASON_RECORD,
        SeasonConclusionPhase.COMPLETE,
        null,
        -> AppRoute.SeasonRecord
    }
}
