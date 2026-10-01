package com.legendoftheglassdragon

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.legendoftheglassdragon.data.AppPreferences
import com.legendoftheglassdragon.data.GameSaveRepository
import com.legendoftheglassdragon.data.SaveLoadResult
import com.legendoftheglassdragon.data.SaveWriteResult
import com.legendoftheglassdragon.game.CommunityActions
import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonCombatEngine
import com.legendoftheglassdragon.game.DragonCombatStatus
import com.legendoftheglassdragon.game.DragonReadinessRules
import com.legendoftheglassdragon.game.DragonSeasonActions
import com.legendoftheglassdragon.game.DragonSeasonBalance
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.GameActionRejection
import com.legendoftheglassdragon.game.GameActionResult
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameEngine
import com.legendoftheglassdragon.game.GameRandom
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.RandomRoll
import com.legendoftheglassdragon.game.SeasonConclusionPhase
import com.legendoftheglassdragon.game.StartingCharacterRules
import com.legendoftheglassdragon.game.WorldAdvancer
import com.legendoftheglassdragon.game.WorldState
import com.legendoftheglassdragon.navigation.AppRoute
import com.legendoftheglassdragon.navigation.ProductWrapperPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Android-runtime release qualification using production repositories and domain transitions. */
@RunWith(AndroidJUnit4::class)
class ReleaseStateMachineInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val activeSaves
        get() = GameSaveRepository.from(context)

    private val completedSeasons
        get() = GameSaveRepository.completedSeasonFrom(context)

    @Before
    fun clearQualificationSlots() {
        assertTrue(activeSaves.delete())
        assertTrue(completedSeasons.delete())
    }

    @Test
    fun persistedCombatChronicleAndDawnResumeFromAuthoritativeState() {
        val base = newGame(seed = 42L)
        val combat = GameEngine(FixedGameRandom(0))
            .startForestEncounter(base, base.revision)
            .applied()
        saveAndReload(combat).also { restored ->
            assertEquals(AppRoute.Combat, ProductWrapperPolicy.resumeRoute(restored))
            assertEquals(combat.activeCombat, restored.activeCombat)
            assertEquals(9, restored.playerDay.fightsRemaining)
        }

        val chronicle = WorldAdvancer.advanceWorldToNextDay(base, base.revision).applied()
        saveAndReload(chronicle).also { restored ->
            assertEquals(AppRoute.GlassChronicle, ProductWrapperPolicy.resumeRoute(restored))
            assertEquals(2, restored.currentDay)
            assertEquals(
                GameActionResult.Rejected(GameActionRejection.PendingDayTransition),
                WorldAdvancer.advanceWorldToNextDay(restored, restored.revision),
            )
        }

        val dawn = CommunityActions.advanceToDawnBrief(
            chronicle,
            chronicle.revision,
        ).applied()
        saveAndReload(dawn).also { restored ->
            assertEquals(AppRoute.DawnBrief, ProductWrapperPolicy.resumeRoute(restored))
            assertEquals(chronicle.community.chronicleHistory, restored.community.chronicleHistory)
        }
    }

    @Test
    fun playerDragonVictoryAndDefeatEachPersistExactlyOnce() {
        val winningEngine = DragonCombatEngine(FixedGameRandom(2))
        var winning = dragonReadyPlayer(newGame(seed = 9L))
        winning = winningEngine.begin(winning, winning.revision).applied()
        assertEquals(AppRoute.DragonCombat, ProductWrapperPolicy.resumeRoute(saveAndReload(winning)))
        repeat(24) {
            if (winning.activeDragonCombat?.status == DragonCombatStatus.ACTIVE) {
                winning = winningEngine.attack(winning, winning.revision).applied()
                winning = saveAndReload(winning)
            }
        }
        assertEquals(DragonCombatStatus.PLAYER_VICTORY, winning.activeDragonCombat?.status)
        assertEquals(DragonStage.RESOLVED, winning.season.stage)
        assertEquals(winning.player.id, winning.season.victorId)
        assertEquals(1, winning.season.attempts.count { it.outcome == DragonAttemptOutcome.VICTORY })
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.DragonCombatAlreadyComplete),
            winningEngine.attack(winning, winning.revision),
        )

        val losingEngine = DragonCombatEngine(FixedGameRandom(0))
        val ready = dragonReadyPlayer(newGame(seed = 10L))
        var losing = losingEngine.begin(ready, ready.revision).applied()
        losing = losing.copy(player = losing.player.copy(currentHp = 1, gold = 100))
        losing = losingEngine.attack(losing, losing.revision).applied()
        val restoredLoss = saveAndReload(losing)
        assertEquals(DragonCombatStatus.PLAYER_DEFEAT, restoredLoss.activeDragonCombat?.status)
        assertEquals(1, restoredLoss.season.attempts.size)
        assertEquals(75, restoredLoss.player.gold)
        assertEquals(0, restoredLoss.playerDay.fightsRemaining)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.DragonCombatAlreadyComplete),
            losingEngine.attack(restoredLoss, restoredLoss.revision),
        )
    }

    @Test
    fun completeAutonomousSeasonArchivesAndSecondSeasonResetsWithoutResettingSettings() {
        val preferences = AppPreferences.from(context)
        preferences.setMusicEnabled(false)
        preferences.setClickSoundEnabled(false)

        var season = newGame(seed = 7_919L, name = "Release Fixture")
        assertEquals(SaveWriteResult.Saved, activeSaves.save(season))
        repeat(60) {
            if (season.season.stage != DragonStage.RESOLVED) {
                season = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                    season,
                    season.revision,
                ).applied()
                assertEquals(SaveWriteResult.Saved, activeSaves.save(season))
            }
        }

        assertEquals(DragonStage.RESOLVED, season.season.stage)
        assertNotEquals(season.player.id, season.season.victorId)
        assertTrue(season.currentDay <= DragonSeasonBalance.GUARANTEED_RESOLUTION_DAY + 1)
        while (season.season.conclusionPhase != SeasonConclusionPhase.COMPLETE) {
            season = DragonSeasonActions.advanceConclusion(season, season.revision).applied()
            assertEquals(SaveWriteResult.Saved, activeSaves.save(season))
        }
        assertEquals(SaveWriteResult.Saved, completedSeasons.save(season))
        assertEquals(season, load(activeSaves))
        assertEquals(season, load(completedSeasons))

        val secondSeason = newGame(seed = 88L, name = "Second Season")
        assertEquals(SaveWriteResult.Saved, activeSaves.save(secondSeason))
        assertEquals(1, load(activeSaves).currentDay)
        assertEquals(DragonStage.WHISPERS, load(activeSaves).season.stage)
        assertEquals(1, load(activeSaves).player.level)
        assertEquals(StartingCharacterRules.GOLD, load(activeSaves).player.gold)
        assertEquals(season, load(completedSeasons))
        assertEquals(false, preferences.load().musicEnabled)
        assertEquals(false, preferences.load().clickSoundEnabled)

        // Leave an authentic completed-season fixture for explicit release-APK UI qualification.
        assertEquals(SaveWriteResult.Saved, activeSaves.save(season))
        preferences.setMusicEnabled(true)
        preferences.setClickSoundEnabled(true)
    }

    private fun newGame(seed: Long, name: String = "Release Hero"): GameState =
        (NewGameFactory(
            idProvider = { "release-player-$seed" },
            seedProvider = { seed },
        ).create(name) as NewGameCreationResult.Created).gameState

    private fun dragonReadyPlayer(state: GameState): GameState {
        val targetLevel = 8
        val maximumHp = StartingCharacterRules.MAXIMUM_HP +
            (targetLevel - 1) * GameBalance.LEVEL_MAX_HP_GAIN
        val weapon = EquipmentCatalog.weapons.last()
        val armor = EquipmentCatalog.armors.last()
        val ready = state.copy(
            player = state.player.copy(
                level = targetLevel,
                experience = ProgressionRules.experienceRequiredForLevel(targetLevel),
                currentHp = maximumHp,
                maximumHp = maximumHp,
                baseAttack = StartingCharacterRules.ATTACK +
                    (targetLevel - 1) * GameBalance.LEVEL_ATTACK_GAIN,
                baseDefense = StartingCharacterRules.DEFENSE +
                    (targetLevel - 1) * GameBalance.LEVEL_DEFENSE_GAIN,
                gold = 500,
                weaponId = weapon.id,
                armorId = armor.id,
            ),
            season = state.season.copy(
                stage = DragonStage.CHALLENGE_WINDOW,
                pressure = DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE,
                stageStartedDay = state.currentDay,
            ),
        )
        assertTrue(DragonReadinessRules.player(ready).isReady)
        return ready
    }

    private fun saveAndReload(state: GameState): GameState {
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
        assertEquals(SaveWriteResult.Saved, activeSaves.save(state))
        return load(activeSaves).also { restored -> assertEquals(state, restored) }
    }

    private fun load(repository: GameSaveRepository): GameState =
        (repository.load() as SaveLoadResult.Loaded).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}

private class FixedGameRandom(private val value: Int) : GameRandom {
    override fun nextInt(world: WorldState, bound: Int): RandomRoll = RandomRoll(
        value = value.coerceIn(0, bound - 1),
        world = world.copy(randomStep = world.randomStep + 1),
    )
}
