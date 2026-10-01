package com.legendoftheglassdragon.game

import java.security.SecureRandom
import java.util.UUID

object StartingCharacterRules {
    const val DAY = 1
    const val LEVEL = 1
    const val EXPERIENCE = 0
    const val MAXIMUM_HP = 20
    const val ATTACK = 4
    const val DEFENSE = 2
    const val GOLD = 25
    const val FIGHTS_PER_DAY = GameBalance.FOREST_FIGHTS_PER_DAY
    const val WEAPON_ID = EquipmentCatalog.STARTER_WEAPON_ID
    const val ARMOR_ID = EquipmentCatalog.STARTER_ARMOR_ID
}

sealed interface NewGameCreationResult {
    data class Created(val gameState: GameState) : NewGameCreationResult
    data class InvalidName(val error: NameValidationError) : NewGameCreationResult
}

class NewGameFactory(
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val seedProvider: () -> Long = { SecureRandom().nextLong() },
) {
    fun create(characterName: String): NewGameCreationResult {
        val validName = when (val result = CharacterNameValidator.validate(characterName)) {
            is NameValidationResult.Invalid -> return NewGameCreationResult.InvalidName(result.error)
            is NameValidationResult.Valid -> result.name
        }

        val player = PlayerCharacter(
            id = idProvider(),
            name = validName,
            level = StartingCharacterRules.LEVEL,
            experience = StartingCharacterRules.EXPERIENCE,
            currentHp = StartingCharacterRules.MAXIMUM_HP,
            maximumHp = StartingCharacterRules.MAXIMUM_HP,
            baseAttack = StartingCharacterRules.ATTACK,
            baseDefense = StartingCharacterRules.DEFENSE,
            gold = StartingCharacterRules.GOLD,
            bankedGold = 0,
            weaponId = StartingCharacterRules.WEAPON_ID,
            armorId = StartingCharacterRules.ARMOR_ID,
        )
        val worldSeed = seedProvider()

        return NewGameCreationResult.Created(
            GameState(
                currentDay = StartingCharacterRules.DAY,
                player = player,
                playerDay = PlayerDayState(
                    currentDay = StartingCharacterRules.DAY,
                    fightsRemaining = StartingCharacterRules.FIGHTS_PER_DAY,
                    hasEndedCurrentDay = false,
                ),
                world = WorldState(seed = worldSeed, randomStep = 0),
                community = CommunityFactory.create(worldSeed = worldSeed),
                trophyStats = TrophyCampaignStats(
                    maxGoldHeld = StartingCharacterRules.GOLD,
                    bestExperience = StartingCharacterRules.EXPERIENCE,
                    weaponIdsOwned = listOf(StartingCharacterRules.WEAPON_ID),
                    armorIdsOwned = listOf(StartingCharacterRules.ARMOR_ID),
                    campaignStartGold = StartingCharacterRules.GOLD,
                ),
            ),
        )
    }
}
