package com.legendoftheglassdragon.game

data class GameState(
    val saveVersion: Int = CURRENT_SAVE_VERSION,
    val revision: Long = 0,
    val currentDay: Int,
    val player: PlayerCharacter,
    val playerDay: PlayerDayState,
    val world: WorldState,
    val community: CommunityState,
    val season: DragonSeasonState = DragonSeasonState(),
    val activeCombat: CombatState? = null,
    val activeChallenge: ChallengeState? = null,
    val activeDragonCombat: DragonCombatState? = null,
    val pendingDayTransition: DayTransitionState? = null,
    /** Campaign-local facts used by the player-layer trophy evaluator. */
    val trophyStats: TrophyCampaignStats = TrophyCampaignStats(),
    /** Persisted so the one-time new-campaign welcome is not shown after reload. */
    val welcomeShown: Boolean = false,
) {
    companion object {
        // The feature fields are optional additions to the established v8 schema.
        const val CURRENT_SAVE_VERSION = 8
    }
}

/** Small, authoritative campaign aggregates. Earned trophies themselves live outside this save. */
data class TrophyCampaignStats(
    val forestBattlesWon: Int = 0,
    val forestBattlesLost: Int = 0,
    val forestWinStreak: Int = 0,
    val bestForestWinStreak: Int = 0,
    val daysCompleted: Int = 0,
    val consecutiveDaysWithoutDefeat: Int = 0,
    val bestConsecutiveDaysWithoutDefeat: Int = 0,
    val trainingWins: Int = 0,
    val overqualifiedAdvancement: Boolean = false,
    val maxGoldHeld: Int = 0,
    val cumulativeGoldSpent: Int = 0,
    val largestPurchase: Int = 0,
    val weaponIdsOwned: List<String> = emptyList(),
    val armorIdsOwned: List<String> = emptyList(),
    val encounteredEnemyIds: List<String> = emptyList(),
    val defeatedEnemyIds: List<String> = emptyList(),
    val enemyIdsThatDefeatedPlayer: List<String> = emptyList(),
    val lowHpForestVictories: Int = 0,
    val dangerousBattlesWon: Int = 0,
    val finalFightVictories: Int = 0,
    val dangerousSequenceWins: Int = 0,
    val perfectDays: Int = 0,
    val meaningfulDefeats: Int = 0,
    val maxExperienceBeforeDefeat: Int = 0,
    val maxExperienceAfterDefeat: Int = 0,
    val giantKills: Int = 0,
    val defeatedAfterBeingDefeatedIds: List<String> = emptyList(),
    val bestRank: Int = Int.MAX_VALUE,
    val firstPlaceStreak: Int = 0,
    val bestFirstPlaceStreak: Int = 0,
    val rankAtCampaignStart: Int = Int.MAX_VALUE,
    val challengeWinNpcIds: List<String> = emptyList(),
    val challengeLossNpcIds: List<String> = emptyList(),
    val notableCommunityEvents: Int = 0,
    val rareEncounterIds: List<String> = emptyList(),
    val dangerousOptionalSuccess: Boolean = false,
    val rarePathFound: Boolean = false,
    val dragonVictoryInCampaign: Boolean = false,
    val dragonVictoryDay: Int = 0,
    val lowHpDragonVictory: Boolean = false,
    val dragonReady: Boolean = false,
    val bestExperience: Int = 0,
    val lastDefeatHighWaterExperience: Int = 0,
    val campaignStartGold: Int = 25,
) {
    val currentWeapons: Set<String> get() = weaponIdsOwned.toSet()
    val currentArmors: Set<String> get() = armorIdsOwned.toSet()
    val encounteredEnemies: Set<String> get() = encounteredEnemyIds.toSet()
    val defeatedEnemies: Set<String> get() = defeatedEnemyIds.toSet()
}

enum class DayTransitionPhase {
    CHRONICLE,
    DAWN_BRIEF,
}

/** Persisted presentation checkpoint for an already-resolved, never-to-be-replayed world day. */
data class DayTransitionState(
    val resolvedDay: Int,
    val nextDay: Int,
    val phase: DayTransitionPhase,
)

data class PlayerCharacter(
    val id: String,
    val name: String,
    val level: Int,
    val experience: Int,
    val currentHp: Int,
    val maximumHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val gold: Int,
    val bankedGold: Int,
    val weaponId: String,
    val armorId: String,
    val challengeWins: Int = 0,
    val challengeLosses: Int = 0,
) {
    val attack: Int get() = EquipmentCatalog.totalAttack(baseAttack, weaponId)
    val defense: Int get() = EquipmentCatalog.totalDefense(baseDefense, armorId)
    val weapon: String get() = EquipmentCatalog.requireWeapon(weaponId).displayName
    val armor: String get() = EquipmentCatalog.requireArmor(armorId).displayName
}

data class PlayerDayState(
    val currentDay: Int,
    val fightsRemaining: Int,
    val hasEndedCurrentDay: Boolean,
    val wasDefeatedToday: Boolean = false,
    val challengesRemaining: Int = GameBalance.CHALLENGES_PER_DAY,
    /** A Physical Training trial is intentionally limited to one attempt per realm day. */
    val trainingAttemptedToday: Boolean = false,
    /** Drives the persisted result screen after process recreation. */
    val lastTrainingSucceeded: Boolean? = null,
)

/** The deliberately small, extensible world boundary for the first save schema. */
data class WorldState(
    val seed: Long,
    val randomStep: Long = 0,
)

enum class CombatStatus {
    ACTIVE,
    VICTORY,
    ESCAPED,
    DEFEAT,
}

enum class CombatMessageKind {
    ENCOUNTER,
    PLAYER_ACTION,
    MONSTER_ACTION,
    VICTORY,
    ESCAPE,
    DEFEAT,
    INFO,
}

data class CombatMessage(
    val text: String,
    val kind: CombatMessageKind,
)

data class CombatState(
    val monsterId: String,
    val monsterCurrentHp: Int,
    val status: CombatStatus,
    val fightConsumed: Boolean,
    val recentMessages: List<CombatMessage>,
    val escapeAvailable: Boolean,
    val rewardsGranted: Boolean,
    val experienceReward: Int = 0,
    val goldReward: Int = 0,
    val goldLost: Int = 0,
)

enum class ChallengeStatus {
    ACTIVE,
    PLAYER_VICTORY,
    PLAYER_DEFEAT,
    YIELDED,
    REFUSED,
}

data class ChallengeState(
    val opponentId: String,
    val opponentCurrentHp: Int,
    val status: ChallengeStatus,
    val recentMessages: List<CombatMessage>,
    val opportunityConsumed: Boolean,
    val rewardsGranted: Boolean,
    val experienceReward: Int = 0,
    val goldTransferred: Int = 0,
)

enum class DragonStage {
    WHISPERS,
    SIGNS,
    FEAR,
    CRISIS,
    CHALLENGE_WINDOW,
    RESOLVED,
}

enum class DragonIncidentType {
    DISTANT_CHIMES,
    GLASS_FRAGMENTS,
    MISSING_TRAVELERS,
    BURNED_WOODLAND,
    WOUNDED_SCOUT,
    SHATTERED_HOMESTEAD,
    DRAGON_SIGHTING,
    CHAMPION_CALL,
}

enum class DragonAttemptOutcome {
    DECISIVE_DEFEAT,
    NARROW_DEFEAT,
    RETREAT,
    VICTORY,
}

enum class SeasonConclusionPhase {
    VICTORY_NOTICE,
    FINAL_CHRONICLE,
    SEASON_RECORD,
    COMPLETE,
}

data class DragonIncidentRecord(
    val id: String,
    val day: Int,
    val type: DragonIncidentType,
    val stage: DragonStage,
    val visibility: WorldEventVisibility,
    val residentId: String? = null,
)

data class DragonAttemptRecord(
    val id: String,
    val day: Int,
    val challengerId: String,
    val outcome: DragonAttemptOutcome,
    val challengerPower: Int,
    val dragonPower: Int,
)

data class DragonSeasonState(
    val stage: DragonStage = DragonStage.WHISPERS,
    val pressure: Int = 0,
    val stageStartedDay: Int = 1,
    val lastIncidentDay: Int = 0,
    val incidents: List<DragonIncidentRecord> = emptyList(),
    val declaredChallengerIds: List<String> = emptyList(),
    val attempts: List<DragonAttemptRecord> = emptyList(),
    val playerLastAttemptDay: Int = 0,
    val victorId: String? = null,
    val resolvedDay: Int = 0,
    val conclusionPhase: SeasonConclusionPhase? = null,
)

enum class DragonCombatStatus {
    ACTIVE,
    PLAYER_VICTORY,
    PLAYER_DEFEAT,
    RETREATED,
}

data class DragonCombatState(
    val dragonCurrentHp: Int,
    val status: DragonCombatStatus,
    val recentMessages: List<CombatMessage>,
    val turnNumber: Int,
    val crystalHideBroken: Boolean,
    val attemptRecorded: Boolean,
    val goldLost: Int = 0,
)
