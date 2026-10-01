package com.legendoftheglassdragon.game

import com.legendoftheglassdragon.data.GameSaveRepository
import com.legendoftheglassdragon.data.SaveLoadResult
import com.legendoftheglassdragon.data.SaveWriteResult
import com.legendoftheglassdragon.data.StringSaveStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragonSeasonTest {
    @Test
    fun stageProgressionIsDeterministicAndNeverRegresses() {
        var first = newGame(101L)
        var second = newGame(101L)
        val stages = mutableListOf<DragonStage>()

        repeat(18) {
            if (first.season.stage != DragonStage.RESOLVED) {
                first = advance(first)
                second = advance(second)
                stages += first.season.stage
                assertEquals(first, second)
            }
        }

        assertTrue(stages.zipWithNext().all { (a, b) -> b.ordinal >= a.ordinal })
        assertTrue(stages.any { it >= DragonStage.FEAR })
        assertTrue(first.season.incidents.all { incidentAllowedAtStage(it) })
        assertTrue(first.community.chronicleHistory.flatMap { it.events }.any {
            it.type == WorldEventType.DRAGON_INCIDENT
        })
    }

    @Test
    fun readinessUsesActualLevelEquipmentHealthAndCampaignStage() {
        val fresh = newGame(7L)
        assertFalse(DragonReadinessRules.player(fresh).isReady)
        assertTrue(DragonReadinessRules.player(fresh).playerGuidance().isNotEmpty())

        val ready = dragonReadyPlayer(fresh)
        assertTrue(DragonReadinessRules.player(ready).isReady)
        val strongest = DragonReadinessRules.potentialCandidates(ready).firstOrNull()
        strongest?.let {
            assertEquals(
                DragonReadinessRules.npc(it, ready.season.stage).combatPower,
                DragonReadinessRules.potentialCandidates(ready).first().let { candidate ->
                    DragonReadinessRules.npc(candidate, ready.season.stage).combatPower
                },
            )
        }
    }

    @Test
    fun unqualifiedPlayerCannotBeginAndQualifiedAttemptBeginsOnce() {
        val engine = DragonCombatEngine()
        val unready = newGame(8L).copy(
            season = DragonSeasonState(
                stage = DragonStage.CHALLENGE_WINDOW,
                pressure = DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE,
                stageStartedDay = 1,
            ),
        )
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.DragonNotReady),
            engine.begin(unready, unready.revision),
        )

        val ready = dragonReadyPlayer(unready)
        val begun = engine.begin(ready, ready.revision).applied()
        assertNotNull(begun.activeDragonCombat)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.ActiveDragonCombat),
            engine.begin(begun, begun.revision),
        )
    }

    @Test
    fun unqualifiedNpcCannotAttemptAndSupportUsesRealEconomyOnce() {
        val base = newGame(81L)
        val weak = base.community.adventurers.first()
        val window = base.copy(
            currentDay = 20,
            playerDay = base.playerDay.copy(currentDay = 20),
            community = base.community.copy(
                adventurers = base.community.adventurers.map { it.copy(lastAdvancedDay = 19) },
                lastSimulatedDay = 19,
            ),
            season = DragonSeasonState(
                stage = DragonStage.CHALLENGE_WINDOW,
                pressure = DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE,
                stageStartedDay = 18,
                declaredChallengerIds = listOf(weak.id),
            ),
            player = base.player.copy(gold = 100),
        )
        val simulation = DragonSeasonSimulator(ConstantGameRandom(50)).advance(
            state = window,
            community = window.community,
            worldState = window.world,
            resolvedDay = 20,
        )
        assertTrue(simulation.season.attempts.isEmpty())

        val supported = DragonSeasonActions.supportCandidate(window, window.revision, weak.id).applied()
        val supportedNpc = supported.community.adventurers.first { it.id == weak.id }
        assertEquals(50, supported.player.gold)
        assertEquals(weak.gold + DragonSeasonBalance.SUPPORT_GOLD, supportedNpc.gold)
        assertEquals(3, supportedNpc.familiarity)
        assertTrue(supported.community.recentEvents.any { it.type == WorldEventType.DRAGON_SUPPORT })
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.StaleState),
            DragonSeasonActions.supportCandidate(supported, window.revision, weak.id),
        )
    }

    @Test
    fun playerVictoryIsAtomicPersistsAndStartsConclusion() {
        val engine = DragonCombatEngine(ConstantGameRandom(2))
        var state = dragonReadyPlayer(newGame(9L))
        state = engine.begin(state, state.revision).applied()
        repeat(20) {
            if (state.activeDragonCombat?.status == DragonCombatStatus.ACTIVE) {
                state = engine.attack(state, state.revision).applied()
            }
        }

        assertEquals(DragonCombatStatus.PLAYER_VICTORY, state.activeDragonCombat?.status)
        assertEquals(DragonStage.RESOLVED, state.season.stage)
        assertEquals(state.player.id, state.season.victorId)
        assertEquals(1, state.season.attempts.count { it.outcome == DragonAttemptOutcome.VICTORY })
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.DragonCombatAlreadyComplete),
            engine.attack(state, state.revision),
        )
        val repository = GameSaveRepository(DragonMemoryStorage())
        assertEquals(SaveWriteResult.Saved, repository.save(state))
        val restored = (repository.load() as SaveLoadResult.Loaded).gameState
        assertEquals(state, restored)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(restored))
    }

    @Test
    fun failedPlayerAttemptAppliesConsequencesOnceAndRequiresAnotherDay() {
        val engine = DragonCombatEngine(ConstantGameRandom(0))
        val ready = dragonReadyPlayer(newGame(10L)).let { state ->
            state.copy(
                player = state.player.copy(
                    currentHp = 1,
                    maximumHp = state.player.maximumHp,
                    gold = 100,
                ),
            )
        }
        // Full health is a readiness requirement; begin lawfully, then model a marginal arrival.
        var state = engine.begin(ready.copy(player = ready.player.copy(currentHp = ready.player.maximumHp)), 0)
            .applied()
            .copy(player = ready.player)
        state = engine.attack(state, state.revision).applied()

        assertEquals(DragonCombatStatus.PLAYER_DEFEAT, state.activeDragonCombat?.status)
        assertEquals(75, state.player.gold)
        assertEquals(0, state.playerDay.fightsRemaining)
        assertTrue(state.playerDay.wasDefeatedToday)
        assertEquals(state.currentDay, state.season.playerLastAttemptDay)
        assertEquals(1, state.season.attempts.size)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.DragonCombatAlreadyComplete),
            engine.attack(state, state.revision),
        )
    }

    @Test
    fun privateDragonSignAppearsInChronicleButNotDawn() {
        val state = newGame(11L)
        val incident = DragonIncidentRecord(
            id = "secret-sign",
            day = 1,
            type = DragonIncidentType.DISTANT_CHIMES,
            stage = DragonStage.WHISPERS,
            visibility = WorldEventVisibility.CHRONICLE_ONLY,
        )
        val event = WorldEvent(
            id = incident.id,
            day = 1,
            type = WorldEventType.DRAGON_INCIDENT,
            characterId = state.player.id,
            detail = incident.id,
            importance = 2,
            visibility = incident.visibility,
        )
        val withIncident = state.copy(season = state.season.copy(incidents = listOf(incident)))
        val record = ChronicleRecord(1, listOf(event))

        assertTrue(ChronicleNarrator.format(event, withIncident).contains("glass"))
        assertTrue(DawnBriefRules.visibleEvents(record, withIncident).isEmpty())
    }

    @Test
    fun resolvedSeasonCannotAdvanceAndConclusionPhasesPersist() {
        val state = resolvedNpcSeason(newGame(12L))
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.SeasonResolved),
            WorldAdvancer.advanceWorldToNextDay(state, state.revision),
        )
        val finalChronicle = DragonSeasonActions.advanceConclusion(state, state.revision).applied()
        val record = DragonSeasonActions.advanceConclusion(
            finalChronicle,
            finalChronicle.revision,
        ).applied()
        val complete = DragonSeasonActions.advanceConclusion(record, record.revision).applied()

        assertEquals(SeasonConclusionPhase.FINAL_CHRONICLE, finalChronicle.season.conclusionPhase)
        assertEquals(SeasonConclusionPhase.SEASON_RECORD, record.season.conclusionPhase)
        assertEquals(SeasonConclusionPhase.COMPLETE, complete.season.conclusionPhase)
        assertEquals(state.season.victorId, complete.season.victorId)
    }

    @Test
    fun twentyFivePassiveSeasonsAllEndWithNpcAgencyAndVariedHistories() {
        val results = (1L..25L).map { seed -> passiveSeason(seed * 7_919L) }

        assertTrue(results.all { it.season.stage == DragonStage.RESOLVED })
        assertTrue(results.all { it.season.victorId != it.player.id })
        assertTrue(results.all { it.season.resolvedDay <= DragonSeasonBalance.GUARANTEED_RESOLUTION_DAY + 1 })
        assertTrue(results.all { it.season.attempts.isNotEmpty() })
        assertTrue(results.flatMap { it.season.attempts }.all {
            it.challengerPower >= DragonSeasonBalance.NPC_MINIMUM_COMBAT_POWER
        })
        assertTrue(results.map { it.season.resolvedDay }.distinct().size >= 3)
        assertTrue(results.mapNotNull { it.season.victorId }.distinct().size >= 3)

        val days = results.map { it.season.resolvedDay }
        val failed = results.sumOf { state ->
            state.season.attempts.count { it.outcome != DragonAttemptOutcome.VICTORY }
        }
        val declared = results.map { it.season.declaredChallengerIds.size }
        val strongestLevels = results.map { state ->
            state.community.adventurers.maxOf { it.level }
        }
        println(
            "DRAGON_COHORT seasons=25 minDay=" + days.min() +
                " medianDay=" + days.sorted()[days.size / 2] +
                " maxDay=" + days.max() +
                " distinctVictors=" + results.mapNotNull { it.season.victorId }.distinct().size +
                " failedAttempts=" + failed +
                " declaredRange=" + declared.min() + ".." + declared.max() +
                " strongestLevelRange=" + strongestLevels.min() + ".." + strongestLevels.max(),
        )
        println("FINAL_CHRONICLE_SAMPLE " + SeasonNarrator.finalChronicle(results.first()).joinToString(" | "))
    }

    private fun passiveSeason(seed: Long): GameState {
        var state = newGame(seed)
        repeat(60) {
            if (state.season.stage == DragonStage.RESOLVED) return state
            state = advance(state)
        }
        return state
    }

    private fun advance(state: GameState): GameState =
        WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(state, state.revision).applied()

    private fun dragonReadyPlayer(state: GameState): GameState {
        val maximumHp = StartingCharacterRules.MAXIMUM_HP +
            (DragonSeasonBalance.PLAYER_MINIMUM_LEVEL - 1) * GameBalance.LEVEL_MAX_HP_GAIN
        return state.copy(
            player = state.player.copy(
                level = DragonSeasonBalance.PLAYER_MINIMUM_LEVEL,
                experience = ProgressionRules.experienceRequiredForLevel(
                    DragonSeasonBalance.PLAYER_MINIMUM_LEVEL,
                ),
                currentHp = maximumHp,
                maximumHp = maximumHp,
                baseAttack = StartingCharacterRules.ATTACK +
                    (DragonSeasonBalance.PLAYER_MINIMUM_LEVEL - 1) * GameBalance.LEVEL_ATTACK_GAIN,
                baseDefense = StartingCharacterRules.DEFENSE +
                    (DragonSeasonBalance.PLAYER_MINIMUM_LEVEL - 1) * GameBalance.LEVEL_DEFENSE_GAIN,
                gold = 500,
                weaponId = "battle_axe",
                armorId = "mithril_mail",
            ),
            season = state.season.copy(
                stage = DragonStage.CHALLENGE_WINDOW,
                pressure = DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE,
                stageStartedDay = state.currentDay,
            ),
        )
    }

    private fun resolvedNpcSeason(state: GameState): GameState {
        val npc = state.community.adventurers.first()
        val attempt = DragonAttemptRecord(
            id = "npc-victory",
            day = 1,
            challengerId = npc.id,
            outcome = DragonAttemptOutcome.VICTORY,
            challengerPower = 140,
            dragonPower = 132,
        )
        return state.copy(
            season = DragonSeasonState(
                stage = DragonStage.RESOLVED,
                pressure = DragonSeasonBalance.MAX_PRESSURE,
                stageStartedDay = 1,
                attempts = listOf(attempt),
                victorId = npc.id,
                resolvedDay = 1,
                conclusionPhase = SeasonConclusionPhase.VICTORY_NOTICE,
            ),
        )
    }

    private fun incidentAllowedAtStage(incident: DragonIncidentRecord): Boolean = when (incident.type) {
        DragonIncidentType.DISTANT_CHIMES -> incident.stage >= DragonStage.WHISPERS
        DragonIncidentType.GLASS_FRAGMENTS,
        DragonIncidentType.MISSING_TRAVELERS,
        -> incident.stage >= DragonStage.SIGNS
        DragonIncidentType.BURNED_WOODLAND,
        DragonIncidentType.WOUNDED_SCOUT,
        -> incident.stage >= DragonStage.FEAR
        DragonIncidentType.SHATTERED_HOMESTEAD,
        DragonIncidentType.DRAGON_SIGHTING,
        -> incident.stage >= DragonStage.CRISIS
        DragonIncidentType.CHAMPION_CALL -> incident.stage >= DragonStage.CHALLENGE_WINDOW
    }

    private fun newGame(seed: Long): GameState = (
        NewGameFactory(
            idProvider = { "player-" + seed },
            seedProvider = { seed },
        ).create("Arcturus") as NewGameCreationResult.Created
        ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private class ConstantGameRandom(private val value: Int) : GameRandom {
    override fun nextInt(world: WorldState, bound: Int): RandomRoll = RandomRoll(
        value = value.coerceIn(0, bound - 1),
        world = world.copy(randomStep = world.randomStep + 1),
    )
}

private class DragonMemoryStorage : StringSaveStorage {
    private var value: String? = null
    override fun read(): String? = value
    override fun write(value: String) {
        this.value = value
    }
    override fun delete() {
        value = null
    }
}
