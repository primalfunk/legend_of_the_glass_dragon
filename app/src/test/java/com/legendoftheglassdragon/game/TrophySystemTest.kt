package com.legendoftheglassdragon.game

import com.legendoftheglassdragon.data.TrophyRepository
import com.legendoftheglassdragon.data.TrophyStoreState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrophySystemTest {
    @Test
    fun catalogHasFiftyUniqueStableEntriesAndARealSecretSet() {
        assertEquals(50, TrophyCatalog.all.size)
        assertEquals(50, TrophyCatalog.all.map { it.id }.toSet().size)
        assertTrue(TrophyCatalog.all.count { it.secret } in 8..12)
        assertEquals("dragon_slayer", TrophyCatalog.all[45].id)
        assertEquals("legend_of_the_glass_dragon", TrophyCatalog.all.last().id)
    }

    @Test
    fun representativeProgressionAndCombatConditionsAreDeterministic() {
        val base = newGame()
        val state = base.copy(
            trophyStats = base.trophyStats.copy(
                forestBattlesWon = 25,
                bestForestWinStreak = 10,
                lowHpForestVictories = 1,
                trainingWins = 9,
            ),
        )
        val ids = TrophyEngine.unlocked(state).map { it.trophy.id }.toSet()
        assertTrue("first_blood" in ids)
        assertTrue("untouchable" in ids)
        assertTrue("last_breath" in ids)
        assertTrue("student_becomes_master" in ids)
    }

    @Test
    fun dragonAndLowHealthDragonConditionsUseAuthoritativeState() {
        val base = newGame()
        val state = base.copy(
            currentDay = 8,
            trophyStats = base.trophyStats.copy(
                dragonVictoryInCampaign = true,
                dragonVictoryDay = 8,
                lowHpDragonVictory = true,
            ),
            season = base.season.copy(
                stage = DragonStage.RESOLVED,
                victorId = base.player.id,
                resolvedDay = 8,
                conclusionPhase = SeasonConclusionPhase.COMPLETE,
            ),
        )
        val ids = TrophyEngine.unlocked(state, TrophyMetagameSnapshot(dragonVictories = 3))
            .map { it.trophy.id }
            .toSet()
        assertTrue("dragon_slayer" in ids)
        assertTrue("dragonbane" in ids)
        assertTrue("by_a_thread" in ids)
    }

    @Test
    fun earnedTrophiesPersistAndCannotBeAwardedTwiceAcrossCampaigns() {
        val storage = FakeTrophyStorage()
        val repository = TrophyRepository(storage)
        val state = newGame().copy(trophyStats = newGame().trophyStats.copy(forestBattlesWon = 25))

        val first = repository.award(state, nowMillis = 10L)
        val second = repository.award(state, nowMillis = 11L)

        assertTrue(first.any { it.trophy.id == "first_blood" })
        assertTrue(second.none { it.trophy.id == "first_blood" })
        assertEquals(1, repository.load().earnedCount)

        val newCampaign = newGame().copy(trophyStats = newGame().trophyStats.copy(forestBattlesWon = 25))
        repository.award(newCampaign, nowMillis = 12L)
        assertEquals(1, repository.load().earnedCount)
    }

    @Test
    fun dragonVictoryCounterIsMetagameStateNotCampaignState() {
        val storage = FakeTrophyStorage()
        val repository = TrophyRepository(storage)
        val created = newGame()
        val victory = created.copy(
            trophyStats = created.trophyStats.copy(dragonVictoryInCampaign = true, dragonVictoryDay = 20),
            season = created.season.copy(
                stage = DragonStage.RESOLVED,
                victorId = created.player.id,
                resolvedDay = 20,
                conclusionPhase = SeasonConclusionPhase.COMPLETE,
            ),
            currentDay = 20,
            playerDay = created.playerDay.copy(currentDay = 20),
        )
        repository.award(victory)
        assertEquals(1, repository.load().dragonVictories)
        repository.award(victory)
        assertEquals(1, repository.load().dragonVictories)
    }

    private fun newGame(): GameState = when (val result = NewGameFactory(
        idProvider = { "test-player" },
        seedProvider = { 42L },
    ).create("Arcturus")) {
        is NewGameCreationResult.Created -> result.gameState
        is NewGameCreationResult.InvalidName -> error("fixture name should be valid")
    }

    private class FakeTrophyStorage : com.legendoftheglassdragon.data.TrophyStringStore {
        var value: String? = null
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
        override fun delete() { value = null }
    }
}
