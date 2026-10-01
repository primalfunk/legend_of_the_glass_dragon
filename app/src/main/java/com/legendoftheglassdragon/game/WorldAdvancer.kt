package com.legendoftheglassdragon.game

import com.legendoftheglassdragon.simulation.CommunitySimulator

/** Future world systems extend this single atomic day-transition seam. */
object WorldAdvancer {
    /** Deterministic simulation/test seam equivalent to choosing "Skip Transition to Village". */
    fun advanceWorldToNextDayAndSkipPresentation(
        state: GameState,
        expectedRevision: Long,
        random: GameRandom = PersistedGameRandom,
    ): GameActionResult = when (
        val advanced = advanceWorldToNextDay(state, expectedRevision, random)
    ) {
        is GameActionResult.Rejected -> advanced
        is GameActionResult.Applied -> CommunityActions.completeDayTransition(
            advanced.gameState,
            advanced.gameState.revision,
        )
    }

    fun advanceWorldToNextDay(
        state: GameState,
        expectedRevision: Long,
        random: GameRandom = PersistedGameRandom,
    ): GameActionResult {
        if (state.revision != expectedRevision) {
            return GameActionResult.Rejected(GameActionRejection.StaleState)
        }
        if (state.activeCombat != null) {
            return GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        }
        if (state.activeChallenge != null) {
            return GameActionResult.Rejected(GameActionRejection.ActiveChallenge)
        }
        if (state.activeDragonCombat != null) {
            return GameActionResult.Rejected(GameActionRejection.ActiveDragonCombat)
        }
        if (state.season.stage == DragonStage.RESOLVED) {
            return GameActionResult.Rejected(GameActionRejection.SeasonResolved)
        }
        if (state.pendingDayTransition != null) {
            return GameActionResult.Rejected(GameActionRejection.PendingDayTransition)
        }
        val completedDayPlayerEvents = state.community.recentEvents
            .filter { it.day == state.currentDay }
        val simulation = CommunitySimulator(random).advance(
            community = state.community,
            player = state.player,
            worldState = state.world,
            resolvedDay = state.currentDay,
        )
        val dragonSimulation = DragonSeasonSimulator(random).advance(
            state = state,
            community = simulation.community,
            worldState = simulation.world,
            resolvedDay = state.currentDay,
        )
        val nextDay = state.currentDay + 1
        val transitionFacts = (
            completedDayPlayerEvents + simulation.transitionEvents + dragonSimulation.events
            )
            .distinctBy { it.id }
        val chronicleRecord = ChronicleRecord(
            day = state.currentDay,
            events = ChronicleRules.selectChronicleEvents(transitionFacts, state.player.id),
        )
        return GameActionResult.Applied(
            state.copy(
                revision = state.revision + 1,
                currentDay = nextDay,
                world = dragonSimulation.world,
                community = dragonSimulation.community.copy(
                    recentEvents = retainPublicNews(
                        previous = state.community.recentEvents,
                        events = transitionFacts,
                        resolvedDay = state.currentDay,
                        playerId = state.player.id,
                    ),
                    chronicleHistory = ChronicleRules.appendRecord(
                        state.community.chronicleHistory,
                        chronicleRecord,
                    ),
                ),
                season = dragonSimulation.season,
                player = state.player.copy(currentHp = state.player.maximumHp),
                playerDay = PlayerDayState(
                    currentDay = nextDay,
                    fightsRemaining = GameBalance.FOREST_FIGHTS_PER_DAY,
                    hasEndedCurrentDay = false,
                    wasDefeatedToday = false,
                    challengesRemaining = GameBalance.CHALLENGES_PER_DAY,
                    trainingAttemptedToday = false,
                    lastTrainingSucceeded = null,
                ),
                activeCombat = null,
                activeChallenge = null,
                activeDragonCombat = null,
                pendingDayTransition = DayTransitionState(
                    resolvedDay = state.currentDay,
                    nextDay = nextDay,
                    phase = DayTransitionPhase.CHRONICLE,
                ),
            ),
        )
    }

    private fun retainPublicNews(
        previous: List<WorldEvent>,
        events: List<WorldEvent>,
        resolvedDay: Int,
        playerId: String,
    ): List<WorldEvent> {
        val earliestDay = (resolvedDay - CommunityRules.NEWS_RETENTION_DAYS + 1).coerceAtLeast(1)
        val today = ChronicleRules.selectPublicNews(events, playerId)
        return (previous.filter { it.day != resolvedDay } + today)
            .filter { it.day >= earliestDay }
            .takeLast(CommunityRules.MAX_RETAINED_EVENTS)
    }
}
