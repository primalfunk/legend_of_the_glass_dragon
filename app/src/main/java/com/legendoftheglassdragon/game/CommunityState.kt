package com.legendoftheglassdragon.game

data class CommunityState(
    val adventurers: List<NpcAdventurer>,
    val recentEvents: List<WorldEvent>,
    val chronicleHistory: List<ChronicleRecord>,
    val lastSimulatedDay: Int,
)

enum class NpcStatus {
    ACTIVE,
    RECOVERING,
}

enum class NpcPersonality {
    AGGRESSIVE,
    CAUTIOUS,
    AMBITIOUS,
    GREEDY,
    SOCIAL,
    ERRATIC,
}

data class NpcAdventurer(
    val id: String,
    val name: String,
    val level: Int,
    val experience: Int,
    val maximumHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val gold: Int,
    val weaponId: String,
    val armorId: String,
    val status: NpcStatus,
    val recoveryDaysRemaining: Int,
    val personality: NpcPersonality,
    val familiarity: Int,
    val lastMetDay: Int,
    val lastAdvancedDay: Int,
    val rivalry: Int = 0,
    val playerWinsAgainst: Int = 0,
    val playerLossesAgainst: Int = 0,
    val challengeWins: Int = 0,
    val challengeLosses: Int = 0,
    val lastChallengeDay: Int = 0,
) {
    val attack: Int get() = EquipmentCatalog.totalAttack(baseAttack, weaponId)
    val defense: Int get() = EquipmentCatalog.totalDefense(baseDefense, armorId)
    val weapon: String get() = EquipmentCatalog.requireWeapon(weaponId).displayName
    val armor: String get() = EquipmentCatalog.requireArmor(armorId).displayName
}

enum class WorldEventType {
    LEVEL_GAIN,
    DEFEAT,
    RECOVERY,
    EQUIPMENT,
    GREAT_SUCCESS,
    SOCIAL,
    RANK_CHANGE,
    CHALLENGE,
    DRAGON_INCIDENT,
    DRAGON_INTENT,
    DRAGON_ATTEMPT,
    DRAGON_VICTORY,
    DRAGON_SUPPORT,
}

enum class WorldEventVisibility {
    /** The tablet player may see this in the Chronicle, but the adventurer does not know it. */
    CHRONICLE_ONLY,

    /** The event has become common knowledge and may appear in public news or a Dawn Brief. */
    PUBLIC,

    /** The event directly involved or was expressly delivered to the player character. */
    PLAYER_DIRECT,
}

enum class WorldEventLocation {
    FOREST,
    INN,
    TRAINING_HALL,
    MARKET,
    VILLAGE,
    UNKNOWN,
}

data class WorldEvent(
    val id: String,
    val day: Int,
    val type: WorldEventType,
    val characterId: String,
    val relatedCharacterId: String? = null,
    val value: Int = 0,
    val detail: String? = null,
    val previousRank: Int = 0,
    val newRank: Int = 0,
    val importance: Int,
    val visibility: WorldEventVisibility = WorldEventVisibility.PUBLIC,
    val location: WorldEventLocation = WorldEventLocation.UNKNOWN,
    val beforeValue: Int = 0,
    val afterValue: Int = 0,
    val relatedPriorEventId: String? = null,
    val relationshipDelta: Int = 0,
    val directlyAffectsPlayer: Boolean = false,
)

/** Immutable, structured facts selected for one completed realm day. */
data class ChronicleRecord(
    val day: Int,
    val events: List<WorldEvent>,
)
