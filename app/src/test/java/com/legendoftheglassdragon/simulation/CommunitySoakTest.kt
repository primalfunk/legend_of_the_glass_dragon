package com.legendoftheglassdragon.simulation

import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.GameActionResult
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.NpcStatus
import com.legendoftheglassdragon.game.WorldAdvancer
import com.legendoftheglassdragon.game.WorldEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunitySoakTest {
    @Test
    fun deterministicOneThousandDayWorldRemainsStructurallyLawful() {
        var state = (
            NewGameFactory(
                idProvider = { "player-001" },
                seedProvider = { 8675309L },
            ).create("Arcturus") as NewGameCreationResult.Created
            ).gameState
        val originalIdentities = state.community.adventurers.associate { it.id to it.name }

        repeat(1_000) {
            state = (
                WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                    state.copy(season = com.legendoftheglassdragon.game.DragonSeasonState()),
                    state.revision,
                )
                    as GameActionResult.Applied
                ).gameState.copy(season = com.legendoftheglassdragon.game.DragonSeasonState())
            assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
            assertTrue(state.community.recentEvents.size <= CommunityRules.MAX_RETAINED_EVENTS)
            assertTrue(state.community.adventurers.all { it.gold >= 0 && it.level >= 1 })
            assertTrue(state.community.adventurers.all {
                EquipmentCatalog.weapon(it.weaponId) != null &&
                    EquipmentCatalog.armor(it.armorId) != null &&
                    it.challengeWins >= 0 && it.challengeLosses >= 0
            })
            assertTrue(state.community.adventurers.all {
                it.status == NpcStatus.ACTIVE || it.recoveryDaysRemaining > 0
            })
        }

        assertEquals(1_001, state.currentDay)
        assertEquals(originalIdentities, state.community.adventurers.associate { it.id to it.name })
        assertEquals(
            state.community.adventurers.size + 1,
            CommunityRules.rankings(state).map { it.characterId }.distinct().size,
        )
        assertTrue(state.community.recentEvents.all {
            it.day >= state.currentDay - CommunityRules.NEWS_RETENTION_DAYS
        })
        assertTrue(state.community.adventurers.sumOf { it.challengeWins } > 0)
        assertEquals(
            state.community.adventurers.sumOf { it.challengeWins },
            state.community.adventurers.sumOf { it.challengeLosses },
        )
        assertTrue(state.community.adventurers.any {
            it.weaponId != EquipmentCatalog.STARTER_WEAPON_ID ||
                it.armorId != EquipmentCatalog.STARTER_ARMOR_ID
        })
        assertTrue(state.community.recentEvents.any { it.type == WorldEventType.CHALLENGE })
    }

    @Test
    fun firstHundredDaysProduceNonUniformProgressDefeatsAndEquipment() {
        var state = (
            NewGameFactory(
                idProvider = { "player-001" },
                seedProvider = { 42L },
            ).create("Arcturus") as NewGameCreationResult.Created
            ).gameState
        var sawDefeat = false

        repeat(100) {
            state = (
                WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                    state.copy(season = com.legendoftheglassdragon.game.DragonSeasonState()),
                    state.revision,
                )
                    as GameActionResult.Applied
                ).gameState.copy(season = com.legendoftheglassdragon.game.DragonSeasonState())
            sawDefeat = sawDefeat || state.community.adventurers.any {
                it.status == NpcStatus.RECOVERING
            }
        }

        assertTrue(state.community.adventurers.map { it.experience }.distinct().size > 3)
        assertTrue(state.community.adventurers.map { it.level }.distinct().size > 1)
        assertTrue(state.community.adventurers.any {
            it.weapon != "Rusty Sword" || it.armor != "Worn Leather"
        })
        assertTrue(sawDefeat)
    }
}
