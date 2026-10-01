package com.legendoftheglassdragon.data

import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.GameActionResult
import com.legendoftheglassdragon.game.GameEngine
import com.legendoftheglassdragon.game.GameRandom
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import com.legendoftheglassdragon.game.RandomRoll
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.WorldAdvancer
import com.legendoftheglassdragon.game.EquipmentCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSaveRepositoryTest {
    @Test
    fun gameStateRoundTripPreservesUnicodeStatsEquipmentAndDay() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        val original = newGame("Éowyn 龙")

        assertEquals(SaveWriteResult.Saved, repository.save(original))

        val loaded = repository.load() as SaveLoadResult.Loaded
        assertEquals(original, loaded.gameState)
        assertEquals("Éowyn 龙", loaded.gameState.player.name)
        assertEquals(original.player.attack, loaded.gameState.player.attack)
        assertEquals(original.player.weapon, loaded.gameState.player.weapon)
        assertEquals(original.currentDay, loaded.gameState.currentDay)
    }

    @Test
    fun replacementWritesTheNewAdventureAtomicallyAsOneRecord() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        repository.save(newGame("First"))

        repository.save(newGame("Second"))

        val loaded = repository.load() as SaveLoadResult.Loaded
        assertEquals("Second", loaded.gameState.player.name)
    }

    @Test
    fun deletingSaveRemovesContinueAvailability() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        repository.save(newGame("Arcturus"))
        assertTrue(repository.hasStoredSave())

        assertTrue(repository.delete())

        assertFalse(repository.hasStoredSave())
        assertEquals(SaveLoadResult.Missing, repository.load())
    }

    @Test
    fun corruptSaveReturnsFailureInsteadOfThrowing() {
        val storage = InMemoryStringSaveStorage("save.version=1\nplayer.name=\\uZZZZ")
        val result = GameSaveRepository(storage).load()

        assertTrue(result is SaveLoadResult.Invalid)
        assertTrue((result as SaveLoadResult.Invalid).failure is SaveFailure.Corrupt)
        assertTrue(storage.value != null)
    }

    @Test
    fun unsupportedVersionIsReportedDeliberatelyAndRetained() {
        val storage = InMemoryStringSaveStorage("save.version=99\n")
        val result = GameSaveRepository(storage).load()

        assertEquals(
            SaveFailure.UnsupportedVersion(99),
            (result as SaveLoadResult.Invalid).failure,
        )
        assertTrue(storage.value != null)
    }

    @Test
    fun invariantViolationIsRejectedOnLoad() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        repository.save(newGame("Arcturus"))
        storage.value = requireNotNull(storage.value).replace("player.currentHp=20", "player.currentHp=999")

        val result = repository.load()

        assertTrue(result is SaveLoadResult.Invalid)
        assertTrue((result as SaveLoadResult.Invalid).failure is SaveFailure.InvalidState)
    }

    @Test
    fun activeEncounterRoundTripPreservesExactCombatAndRandomPosition() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        val zeroRandom = GameRandom { world, _ ->
            RandomRoll(0, world.copy(randomStep = world.randomStep + 1))
        }
        val original = newGame("Arcturus")
        val encounter = (
            GameEngine(zeroRandom).startForestEncounter(original, original.revision)
                as GameActionResult.Applied
            ).gameState

        assertEquals(SaveWriteResult.Saved, repository.save(encounter))
        val loaded = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(encounter, loaded)
        assertEquals(CombatStatus.ACTIVE, loaded.activeCombat?.status)
        assertEquals(9, loaded.playerDay.fightsRemaining)
        assertEquals(encounter.world.randomStep, loaded.world.randomStep)
    }

    @Test
    fun defeatAndNextDayRecoveryEachSurviveExactRoundTrip() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        val zeroRandom = GameRandom { world, _ ->
            RandomRoll(0, world.copy(randomStep = world.randomStep + 1))
        }
        val engine = GameEngine(zeroRandom)
        val original = newGame("Arcturus").let { state ->
            state.copy(player = state.player.copy(currentHp = 1, gold = 100, experience = 49))
        }
        val encounter = (
            engine.startForestEncounter(original, original.revision) as GameActionResult.Applied
            ).gameState
        val defeated = (engine.attack(encounter, encounter.revision) as GameActionResult.Applied).gameState

        assertEquals(SaveWriteResult.Saved, repository.save(defeated))
        var loaded = (repository.load() as SaveLoadResult.Loaded).gameState
        assertEquals(defeated, loaded)
        assertTrue(loaded.playerDay.wasDefeatedToday)
        assertEquals(75, loaded.player.gold)
        assertEquals(49, loaded.player.experience)

        loaded = (engine.leaveCompletedCombat(loaded, loaded.revision) as GameActionResult.Applied).gameState
        val tomorrow = (
            WorldAdvancer.advanceWorldToNextDay(loaded, loaded.revision) as GameActionResult.Applied
            ).gameState
        assertEquals(SaveWriteResult.Saved, repository.save(tomorrow))
        val recovered = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(tomorrow, recovered)
        assertFalse(recovered.playerDay.wasDefeatedToday)
        assertEquals(recovered.player.maximumHp, recovered.player.currentHp)
        assertEquals(49, recovered.player.experience)
        assertEquals(75, recovered.player.gold)
    }

    @Test
    fun trainingEligibilityAndPromotionEachSurviveRoundTrip() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        val engine = GameEngine()
        val eligible = newGame("Arcturus").let { state ->
            state.copy(player = state.player.copy(experience = 50))
        }

        repository.save(eligible)
        val loadedEligible = (repository.load() as SaveLoadResult.Loaded).gameState
        assertTrue(ProgressionRules.isTrainingEligible(loadedEligible.player))

        val promoted = (
            engine.train(loadedEligible, loadedEligible.revision) as GameActionResult.Applied
            ).gameState
        repository.save(promoted)
        val loadedPromoted = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(promoted, loadedPromoted)
        assertEquals(2, loadedPromoted.player.level)
        assertEquals(6, loadedPromoted.player.attack)
        assertEquals(3, loadedPromoted.player.defense)
        assertFalse(ProgressionRules.isTrainingEligible(loadedPromoted.player))
    }

    @Test
    fun legacyV1SaveLoadsAsLawfulV2StateWithoutLosingCharacter() {
        val legacy = """
            save.version=1
            game.currentDay=3
            world.seed=42
            player.id=legacy-id
            player.name=Arcturus
            player.level=1
            player.experience=17
            player.currentHp=13
            player.maximumHp=20
            player.attack=4
            player.defense=2
            player.gold=31
            player.weapon=Rusty Sword
            player.armor=Worn Leather
            day.currentDay=3
            day.fightsRemaining=4
            day.ended=false
        """.trimIndent()

        val migrated = (
            GameSaveRepository(InMemoryStringSaveStorage(legacy)).load()
                as SaveLoadResult.Loaded
            ).gameState

        assertEquals(GameState.CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertEquals("Arcturus", migrated.player.name)
        assertEquals(3, migrated.currentDay)
        assertEquals(4, migrated.playerDay.fightsRemaining)
        assertEquals(0L, migrated.revision)
        assertEquals(0L, migrated.world.randomStep)
        assertEquals(null, migrated.activeCombat)
    }

    @Test
    fun repeatedDayTransitionSaveLoadCyclesPreserveExactState() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        var state = newGame("Longrunner")

        var transitions = 0
        while (state.season.stage != com.legendoftheglassdragon.game.DragonStage.RESOLVED &&
            transitions < 60
        ) {
            state = (
                WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(state, state.revision)
                    as GameActionResult.Applied
                ).gameState
            assertEquals(SaveWriteResult.Saved, repository.save(state))
            state = (repository.load() as SaveLoadResult.Loaded).gameState
            transitions++
        }

        assertTrue(state.season.stage == com.legendoftheglassdragon.game.DragonStage.RESOLVED)
        assertTrue(transitions in 1..60)
        assertEquals(transitions * 2L, state.revision)
        assertEquals("Longrunner", state.player.name)
    }

    @Test
    fun communityIdentitiesEventsAndFamiliaritySurviveRoundTrip() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        val original = newGame("Arcturus")
        val visitorId = original.community.adventurers.first().id
        val changed = original.copy(
            community = original.community.copy(
                adventurers = original.community.adventurers.map { npc ->
                    if (npc.id == visitorId) npc.copy(familiarity = 3) else npc
                },
            ),
        )

        assertEquals(SaveWriteResult.Saved, repository.save(changed))
        val loaded = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(changed.community, loaded.community)
        assertEquals(
            changed.community.adventurers.map { it.id },
            loaded.community.adventurers.map { it.id },
        )
    }

    @Test
    fun economyEquipmentAndChallengeHistorySurviveV5RoundTrip() {
        val repository = GameSaveRepository(InMemoryStringSaveStorage())
        val original = newGame("Arcturus")
        val rivalId = original.community.adventurers.first().id
        val changed = original.copy(
            player = original.player.copy(
                baseAttack = 6,
                baseDefense = 4,
                gold = 73,
                bankedGold = 412,
                weaponId = "steel_blade",
                armorId = "chain_mail",
                challengeWins = 3,
                challengeLosses = 2,
            ),
            playerDay = original.playerDay.copy(challengesRemaining = 0),
            community = original.community.copy(
                adventurers = original.community.adventurers.map { npc ->
                    if (npc.id == rivalId) {
                        npc.copy(
                            rivalry = 11,
                            playerWinsAgainst = 3,
                            playerLossesAgainst = 2,
                            challengeWins = 4,
                            challengeLosses = 5,
                            lastChallengeDay = 1,
                        )
                    } else {
                        npc
                    }
                },
            ),
        )

        assertEquals(SaveWriteResult.Saved, repository.save(changed))
        val loaded = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(changed, loaded)
        assertEquals(412, loaded.player.bankedGold)
        assertEquals("Steel Blade", loaded.player.weapon)
        assertEquals(9, loaded.player.attack)
        assertEquals(7, loaded.player.defense)
        assertEquals(11, loaded.community.adventurers.first().rivalry)
    }

    @Test
    fun schemaV4TotalStatsAndDisplayEquipmentMigrateWithoutDoubleCounting() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        val original = newGame("Arcturus").let { state ->
            state.copy(
                player = state.player.copy(
                    level = 3,
                    experience = 250,
                    baseAttack = 6,
                    baseDefense = 4,
                    weaponId = "steel_blade",
                    armorId = "chain_mail",
                ),
            )
        }
        repository.save(original)
        storage.value = requireNotNull(storage.value)
            .lineSequence()
            .joinToString("\n") { line ->
                when {
                    line == "save.version=8" -> "save.version=4"
                    line == "player.baseAttack=6" -> "player.attack=9"
                    line == "player.baseDefense=4" -> "player.defense=7"
                    line == "player.weaponId=steel_blade" -> "player.weapon=Steel Blade"
                    line == "player.armorId=chain_mail" -> "player.armor=Chain Mail"
                    else -> downgradeNpcEconomySchemaLine(line)
                }
            }

        val migrated = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(GameState.CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertEquals(6, migrated.player.baseAttack)
        assertEquals(4, migrated.player.baseDefense)
        assertEquals(9, migrated.player.attack)
        assertEquals(7, migrated.player.defense)
        assertEquals("steel_blade", migrated.player.weaponId)
        assertEquals("chain_mail", migrated.player.armorId)
        assertEquals(0, migrated.player.bankedGold)
        assertEquals(EquipmentCatalog.requireWeapon("steel_blade").displayName, migrated.player.weapon)
    }

    @Test
    fun schemaV2SaveMigratesDeterministicallyIntoPersistentCommunity() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        repository.save(newGame("Arcturus"))
        storage.value = requireNotNull(storage.value)
            .lineSequence()
            .filterNot { it.startsWith("community.") || it.startsWith("day.wasDefeated") }
            .joinToString("\n") { line -> downgradeEconomySchemaLine(line, version = 2) }

        val firstLoad = (repository.load() as SaveLoadResult.Loaded).gameState
        val secondLoad = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(GameState.CURRENT_SAVE_VERSION, firstLoad.saveVersion)
        assertEquals(16, firstLoad.community.adventurers.size)
        assertEquals(firstLoad.community, secondLoad.community)
        assertEquals(firstLoad.currentDay - 1, firstLoad.community.lastSimulatedDay)
    }

    @Test
    fun schemaV3SaveMigratesLeveledCharactersWithoutInventingDefeatState() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        val original = newGame("Arcturus")
        val leveledNpc = original.community.adventurers.first().copy(
            level = 3,
            experience = 250,
            maximumHp = 29,
            baseAttack = 8,
            baseDefense = 4,
        )
        val currentState = original.copy(
            player = original.player.copy(
                level = 3,
                experience = 250,
                currentHp = 23,
                maximumHp = 30,
                baseAttack = 8,
                baseDefense = 4,
            ),
            community = original.community.copy(
                adventurers = listOf(leveledNpc) + original.community.adventurers.drop(1),
            ),
        )
        assertEquals(SaveWriteResult.Saved, repository.save(currentState))
        storage.value = requireNotNull(storage.value)
            .lineSequence()
            .filterNot { it.startsWith("day.wasDefeated") }
            .joinToString("\n") { line ->
                when (val downgraded = downgradeEconomySchemaLine(line, version = 3)) {
                    "player.experience=250" -> "player.experience=120"
                    "player.attack=8" -> "player.attack=6"
                    "community.npcs.0.experience=250" -> "community.npcs.0.experience=120"
                    "community.npcs.0.attack=8" -> "community.npcs.0.attack=6"
                    else -> downgraded
                }
            }

        val migrated = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(GameState.CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertFalse(migrated.playerDay.wasDefeatedToday)
        assertEquals("Arcturus", migrated.player.name)
        assertEquals(3, migrated.player.level)
        assertEquals(250, migrated.player.experience)
        assertEquals(23, migrated.player.currentHp)
        assertEquals(8, migrated.player.attack)
        assertEquals(16, migrated.community.adventurers.size)
        assertEquals(leveledNpc.id, migrated.community.adventurers.first().id)
        assertEquals(3, migrated.community.adventurers.first().level)
        assertEquals(250, migrated.community.adventurers.first().experience)
        assertEquals(8, migrated.community.adventurers.first().attack)
    }

    @Test
    fun schemaV6ChronicleSaveReceivesSensibleDragonSeasonInitialization() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        val original = newGame("Archivist")
        assertEquals(SaveWriteResult.Saved, repository.save(original))
        storage.value = requireNotNull(storage.value)
            .lineSequence()
            .filterNot { it.startsWith("season.") || it.startsWith("dragonCombat.") }
            .joinToString("\n") { line ->
                if (line == "save.version=8") "save.version=6" else line
            }

        val migrated = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(GameState.CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertEquals(com.legendoftheglassdragon.game.DragonStage.WHISPERS, migrated.season.stage)
        assertEquals(0, migrated.season.pressure)
        assertEquals(null, migrated.activeDragonCombat)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(migrated))
    }

    @Test
    fun schemaV7DragonSaveMigratesWithFreshTrainingOpportunity() {
        val storage = InMemoryStringSaveStorage()
        val repository = GameSaveRepository(storage)
        assertEquals(SaveWriteResult.Saved, repository.save(newGame("Veteran")))
        storage.value = requireNotNull(storage.value)
            .lineSequence()
            .filterNot { it.startsWith("day.training") }
            .joinToString("\n") { line ->
                if (line == "save.version=8") "save.version=7" else line
            }

        val migrated = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(GameState.CURRENT_SAVE_VERSION, migrated.saveVersion)
        assertFalse(migrated.playerDay.trainingAttemptedToday)
        assertEquals(null, migrated.playerDay.lastTrainingSucceeded)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(migrated))
    }

    private fun newGame(name: String) = (
        NewGameFactory(
            idProvider = { "stable-id-$name" },
            seedProvider = { 42L },
        ).create(name) as NewGameCreationResult.Created
    ).gameState

    private fun downgradeEconomySchemaLine(line: String, version: Int): String = line
        .replace("save.version=8", "save.version=$version")
        .replace("player.baseAttack=", "player.attack=")
        .replace("player.baseDefense=", "player.defense=")
        .replace("player.weaponId=rusty_sword", "player.weapon=Rusty Sword")
        .replace("player.armorId=worn_leather", "player.armor=Worn Leather")
        .replace(".baseAttack=", ".attack=")
        .replace(".baseDefense=", ".defense=")
        .replace(".weaponId=rusty_sword", ".weapon=Rusty Sword")
        .replace(".armorId=worn_leather", ".armor=Worn Leather")

    private fun downgradeNpcEconomySchemaLine(line: String): String = line
        .replace(".baseAttack=", ".attack=")
        .replace(".baseDefense=", ".defense=")
        .replace(".weaponId=rusty_sword", ".weapon=Rusty Sword")
        .replace(".armorId=worn_leather", ".armor=Worn Leather")
}

private class InMemoryStringSaveStorage(
    var value: String? = null,
) : StringSaveStorage {
    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }

    override fun delete() {
        value = null
    }
}
