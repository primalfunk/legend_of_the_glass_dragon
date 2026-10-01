package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewGameFactoryTest {
    private val factory = NewGameFactory(
        idProvider = { "player-001" },
        seedProvider = { 8675309L },
    )

    @Test
    fun createsCanonicalLawfulDayOneCharacter() {
        val result = factory.create("  Arcturus  ") as NewGameCreationResult.Created
        val state = result.gameState
        val player = state.player

        assertEquals(GameState.CURRENT_SAVE_VERSION, state.saveVersion)
        assertEquals(StartingCharacterRules.DAY, state.currentDay)
        assertEquals("player-001", player.id)
        assertEquals("Arcturus", player.name)
        assertEquals(StartingCharacterRules.LEVEL, player.level)
        assertEquals(StartingCharacterRules.EXPERIENCE, player.experience)
        assertEquals(player.maximumHp, player.currentHp)
        assertEquals(StartingCharacterRules.ATTACK, player.attack)
        assertEquals(StartingCharacterRules.DEFENSE, player.defense)
        assertEquals(StartingCharacterRules.GOLD, player.gold)
        assertEquals(EquipmentCatalog.requireWeapon(StartingCharacterRules.WEAPON_ID).displayName, player.weapon)
        assertEquals(EquipmentCatalog.requireArmor(StartingCharacterRules.ARMOR_ID).displayName, player.armor)
        assertEquals(state.currentDay, state.playerDay.currentDay)
        assertEquals(StartingCharacterRules.FIGHTS_PER_DAY, state.playerDay.fightsRemaining)
        assertFalse(state.playerDay.hasEndedCurrentDay)
        assertEquals(8675309L, state.world.seed)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(state))
    }

    @Test
    fun rejectsInvalidNameBeforeCreatingIdentity() {
        val result = factory.create("   ")

        assertTrue(result is NewGameCreationResult.InvalidName)
        assertEquals(
            NameValidationError.Empty,
            (result as NewGameCreationResult.InvalidName).error,
        )
    }
}
