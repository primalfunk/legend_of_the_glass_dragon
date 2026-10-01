package com.legendoftheglassdragon.data

import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrophySaveRoundTripTest {
    @Test
    fun optionalPlayerLayerFieldsRoundTripWithoutChangingEstablishedSaveVersion() {
        val storage = MemoryStore()
        val repository = GameSaveRepository(storage)
        val original = when (val result = NewGameFactory(
            idProvider = { "round-trip-player" },
            seedProvider = { 7L },
        ).create("RoundTrip")) {
            is NewGameCreationResult.Created -> result.gameState.copy(
                welcomeShown = false,
                trophyStats = result.gameState.trophyStats.copy(
                    forestBattlesWon = 73,
                    maxGoldHeld = 1_250,
                    encounteredEnemyIds = listOf("giant_rat", "goblin"),
                ),
            )
            is NewGameCreationResult.InvalidName -> error("fixture name should be valid")
        }

        assertEquals(SaveWriteResult.Saved, repository.save(original))
        val loaded = (repository.load() as SaveLoadResult.Loaded).gameState

        assertEquals(8, loaded.saveVersion)
        assertFalse(loaded.welcomeShown)
        assertEquals(73, loaded.trophyStats.forestBattlesWon)
        assertEquals(1_250, loaded.trophyStats.maxGoldHeld)
        assertEquals(listOf("giant_rat", "goblin"), loaded.trophyStats.encounteredEnemyIds)
    }

    private class MemoryStore : StringSaveStorage {
        var value: String? = null
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
        override fun delete() { value = null }
    }
}
