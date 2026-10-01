package com.legendoftheglassdragon.campaign

import com.legendoftheglassdragon.game.ChallengeEngine
import com.legendoftheglassdragon.game.ChallengeStatus
import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonCombatEngine
import com.legendoftheglassdragon.game.DragonCombatStatus
import com.legendoftheglassdragon.game.DragonReadinessRules
import com.legendoftheglassdragon.game.DragonReadinessTier
import com.legendoftheglassdragon.game.DragonSeasonBalance
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.EconomyEngine
import com.legendoftheglassdragon.game.EconomyRules
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.GameActionResult
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameEngine
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import com.legendoftheglassdragon.game.MonsterCatalog
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.NpcAdventurer
import com.legendoftheglassdragon.game.NpcStatus
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.WorldAdvancer
import com.legendoftheglassdragon.game.WorldEvent
import com.legendoftheglassdragon.game.WorldEventType
import kotlin.math.max

/** Deterministic development policies. They are test fixtures, never production game AI. */
enum class CampaignPolicy(
    val label: String,
    val dailyFightLimit: (Int) -> Int,
    val healAtHpPercent: Int,
    val stopAtHpPercentWithoutHealing: Int,
    val runAtIncomingHits: Int,
    val challengeIntervalDays: Int,
    val challengeYieldHpPercent: Int,
    val maximumUpgradesPerVisit: Int,
    val carryGoldTarget: Int,
) {
    PASSIVE(
        label = "Passive",
        dailyFightLimit = { day -> if (day % 2 == 0) 2 else 0 },
        healAtHpPercent = 80,
        stopAtHpPercentWithoutHealing = 70,
        runAtIncomingHits = 3,
        challengeIntervalDays = 0,
        challengeYieldHpPercent = 60,
        maximumUpgradesPerVisit = 1,
        carryGoldTarget = 25,
    ),
    CASUAL(
        label = "Casual / Conservative",
        dailyFightLimit = { 8 },
        healAtHpPercent = 75,
        stopAtHpPercentWithoutHealing = 55,
        runAtIncomingHits = 3,
        challengeIntervalDays = 4,
        challengeYieldHpPercent = 45,
        maximumUpgradesPerVisit = 1,
        carryGoldTarget = 50,
    ),
    ORDINARY(
        label = "Ordinary",
        dailyFightLimit = { 9 },
        healAtHpPercent = 52,
        stopAtHpPercentWithoutHealing = 30,
        runAtIncomingHits = 2,
        challengeIntervalDays = 3,
        challengeYieldHpPercent = 25,
        maximumUpgradesPerVisit = 2,
        carryGoldTarget = 75,
    ),
    AGGRESSIVE(
        label = "Aggressive / Optimizing",
        dailyFightLimit = { GameBalance.FOREST_FIGHTS_PER_DAY },
        healAtHpPercent = 38,
        stopAtHpPercentWithoutHealing = 12,
        runAtIncomingHits = 1,
        challengeIntervalDays = 2,
        challengeYieldHpPercent = 0,
        maximumUpgradesPerVisit = 4,
        carryGoldTarget = 100,
    ),
    RECKLESS(
        label = "Reckless",
        dailyFightLimit = { GameBalance.FOREST_FIGHTS_PER_DAY },
        healAtHpPercent = 20,
        stopAtHpPercentWithoutHealing = 0,
        runAtIncomingHits = 0,
        challengeIntervalDays = 1,
        challengeYieldHpPercent = 0,
        maximumUpgradesPerVisit = 3,
        carryGoldTarget = Int.MAX_VALUE,
    ),
}

data class MonsterSeasonMetric(
    val encounters: Int = 0,
    val victories: Int = 0,
    val defeats: Int = 0,
    val escapes: Int = 0,
    val damageTaken: Int = 0,
    val experienceEarned: Int = 0,
    val goldEarned: Int = 0,
    val encounterDaySum: Int = 0,
    val playerLevelSum: Int = 0,
) {
    operator fun plus(other: MonsterSeasonMetric) = MonsterSeasonMetric(
        encounters + other.encounters,
        victories + other.victories,
        defeats + other.defeats,
        escapes + other.escapes,
        damageTaken + other.damageTaken,
        experienceEarned + other.experienceEarned,
        goldEarned + other.goldEarned,
        encounterDaySum + other.encounterDaySum,
        playerLevelSum + other.playerLevelSum,
    )
}

data class CampaignDayMetric(
    val day: Int,
    val playerLevel: Int,
    val playerExperience: Int,
    val playerRank: Int,
    val currentHpBeforeSleep: Int,
    val maximumHp: Int,
    val carriedGold: Int,
    val bankedGold: Int,
    val weaponId: String,
    val armorId: String,
    val fightsAvailable: Int,
    val fightsAttempted: Int,
    val fightsWon: Int,
    val fightsLost: Int,
    val escapes: Int,
    val fightsUnused: Int,
    val experienceEarned: Int,
    val combatGoldEarned: Int,
    val healingSpent: Int,
    val equipmentSpent: Int,
    val defeatGoldLost: Int,
    val trainingAttempts: Int,
    val trainingSuccesses: Int,
    val trainingFailures: Int,
    val challengeAttempts: Int,
    val challengeWins: Int,
    val challengeLosses: Int,
    val challengeYields: Int,
    val dragonAttempts: Int,
    val dragonAttemptOutcomes: List<DragonAttemptOutcome>,
    val dragonStage: DragonStage,
    val chronicleEntries: Int,
    val chronicleTypeCounts: Map<WorldEventType, Int>,
    val spotlightNpcIds: List<String>,
    val callbacks: Int,
    val relationshipChanges: Int,
    val rankPositionChanges: Int,
    val leaderId: String,
    val npcLevels: List<Int>,
    val readyNpcCandidates: Int,
    val newDragonIncidents: Int,
    val npcEquipmentUpgrades: Int,
)

data class DragonAttemptMetric(
    val challengerType: String,
    val challengerId: String,
    val day: Int,
    val level: Int,
    val rank: Int,
    val weaponId: String,
    val armorId: String,
    val attack: Int,
    val defense: Int,
    val maximumHp: Int,
    val currentHp: Int,
    val readinessTier: DragonReadinessTier,
    val combatPower: Int,
    val dragonPower: Int,
    val attemptNumber: Int,
    val outcome: DragonAttemptOutcome,
)

data class CampaignResult(
    val seed: Long,
    val policy: CampaignPolicy,
    val completed: Boolean,
    val resolutionDay: Int,
    val victorId: String?,
    val playerVictory: Boolean,
    val npcVictory: Boolean,
    val finalPlayerLevel: Int,
    val finalPlayerExperience: Int,
    val finalPlayerRank: Int,
    val finalCarriedGold: Int,
    val finalBankedGold: Int,
    val finalWeaponId: String,
    val finalArmorId: String,
    val levelReachedDays: Map<Int, Int>,
    val equipmentAcquiredDays: Map<String, Int>,
    val stageEnteredDays: Map<DragonStage, Int>,
    val firstPlayerReadyDay: Int?,
    val firstNpcReadyDay: Int?,
    val challengeWindowReadyCandidates: Int,
    val finalReadyCandidates: Int,
    val playerDragonAttempts: List<DragonAttemptOutcome>,
    val npcDragonAttempts: List<DragonAttemptOutcome>,
    val uniqueNpcDragonChallengers: Int,
    val repeatedNpcDragonAttempts: Int,
    val dragonAttemptMetrics: List<DragonAttemptMetric>,
    val totalFightsAttempted: Int,
    val totalFightsWon: Int,
    val totalFightsLost: Int,
    val totalEscapes: Int,
    val totalUnusedFights: Int,
    val totalCombatDamageTaken: Int,
    val victoryHpPercentages: List<Int>,
    val totalCombatExperience: Int,
    val totalCombatGold: Int,
    val totalHealingSpent: Int,
    val totalEquipmentSpent: Int,
    val totalDefeatGoldLost: Int,
    val totalTrainingAttempts: Int,
    val totalTrainingSuccesses: Int,
    val totalTrainingFailures: Int,
    val trainingAttemptsByTargetLevel: Map<Int, Int>,
    val trainingFailuresByTargetLevel: Map<Int, Int>,
    val attemptsPerSuccessfulPromotion: List<Int>,
    val maximumTrainingFailureStreak: Int,
    val totalChallengeAttempts: Int,
    val totalChallengeWins: Int,
    val totalChallengeLosses: Int,
    val totalChallengeYields: Int,
    val relationshipChanges: Int,
    val rivalryFormedCount: Int,
    val rankPositionChanges: Int,
    val leadershipChanges: Int,
    val uniqueLeaderIds: List<String>,
    val playerEverRankOne: Boolean,
    val chronicleEntries: Int,
    val chronicleDays: Int,
    val chronicleTypeCounts: Map<WorldEventType, Int>,
    val chronicleNpcAppearances: Map<String, Int>,
    val incidentsCreated: Int,
    val callbacks: Int,
    val callbackPriorEventIds: Set<String>,
    val callbackTypeCounts: Map<WorldEventType, Int>,
    val npcFinalLevels: List<Int>,
    val npcFinalGold: List<Int>,
    val npcFinalWeaponTiers: List<Int>,
    val npcFinalArmorTiers: List<Int>,
    val days: List<CampaignDayMetric>,
    val monsterMetrics: Map<String, MonsterSeasonMetric>,
    val finalState: GameState,
)

class CampaignSimulationHarness(
    private val maximumDays: Int = 100,
    private val roundTripDays: Set<Int> = emptySet(),
    private val stateRoundTrip: ((GameState) -> GameState)? = null,
) {
    private val game = GameEngine()
    private val challenge = ChallengeEngine()
    private val dragon = DragonCombatEngine()

    fun run(seed: Long, policy: CampaignPolicy): CampaignResult {
        var state = (NewGameFactory(
            idProvider = { "sim-player-$seed" },
            seedProvider = { seed },
        ).create("Simulated Adventurer") as NewGameCreationResult.Created).gameState
        val days = mutableListOf<CampaignDayMetric>()
        val levels = linkedMapOf(1 to 1)
        val equipmentDays = linkedMapOf(
            EquipmentCatalog.STARTER_WEAPON_ID to 1,
            EquipmentCatalog.STARTER_ARMOR_ID to 1,
        )
        val stages = linkedMapOf(DragonStage.WHISPERS to 1)
        val monsterMetrics = linkedMapOf<String, MonsterSeasonMetric>()
        val playerDragonAttempts = mutableListOf<DragonAttemptOutcome>()
        val npcDragonAttempts = mutableListOf<DragonAttemptOutcome>()
        val npcDragonChallengers = mutableSetOf<String>()
        val dragonAttemptMetrics = mutableListOf<DragonAttemptMetric>()
        var repeatedNpcAttempts = 0
        var firstPlayerReadyDay: Int? = null
        var firstNpcReadyDay: Int? = null
        var challengeWindowReadyCandidates = -1
        var latestReadyCandidateCount = 0
        var totalDamageTaken = 0
        val victoryHpPercentages = mutableListOf<Int>()
        var relationshipChanges = 0
        var rankPositionChanges = 0
        var leadershipChanges = 0
        val leaders = linkedSetOf<String>()
        var priorLeader = CommunityRules.rankings(state).first().characterId
        leaders += priorLeader
        var playerEverRankOne = priorLeader == state.player.id
        val chronicleTypes = linkedMapOf<WorldEventType, Int>()
        val npcAppearances = linkedMapOf<String, Int>()
        var incidentsCreated = 0
        var callbacks = 0
        val callbackPriorEventIds = linkedSetOf<String>()
        val callbackTypeCounts = linkedMapOf<WorldEventType, Int>()
        val priorNpcAttemptCounts = mutableMapOf<String, Int>()
        val trainingAttemptsByTargetLevel = linkedMapOf<Int, Int>()
        val trainingFailuresByTargetLevel = linkedMapOf<Int, Int>()
        val attemptsPerSuccessfulPromotion = mutableListOf<Int>()
        val currentPromotionAttempts = mutableMapOf<Int, Int>()
        var currentTrainingFailureStreak = 0
        var maximumTrainingFailureStreak = 0
        var casualFirstReadyDay: Int? = null

        while (state.season.stage != DragonStage.RESOLVED && state.currentDay <= maximumDays) {
            val day = state.currentDay
            val dayStartNpc = state.community.adventurers.associateBy { it.id }
            val dayStartRankMap = rankMap(state)
            val startXp = state.player.experience
            var fightsAttempted = 0
            var fightsWon = 0
            var fightsLost = 0
            var escapes = 0
            var combatXp = 0
            var combatGold = 0
            var healingSpent = 0
            var equipmentSpent = 0
            var defeatGoldLost = 0
            var trainingAttempts = 0
            var trainingSuccesses = 0
            var trainingFailures = 0
            var challengeAttempts = 0
            var challengeWins = 0
            var challengeLosses = 0
            var challengeYields = 0
            val dayDragonOutcomes = mutableListOf<DragonAttemptOutcome>()

            fun trainWhileEligible() {
                if (!ProgressionRules.isTrainingEligible(state.player) ||
                    state.playerDay.trainingAttemptedToday
                ) return
                trainingAttempts += 1
                val beforeLevel = state.player.level
                val targetLevel = beforeLevel + 1
                trainingAttemptsByTargetLevel[targetLevel] =
                    (trainingAttemptsByTargetLevel[targetLevel] ?: 0) + 1
                currentPromotionAttempts[targetLevel] = (currentPromotionAttempts[targetLevel] ?: 0) + 1
                val result = game.train(state, state.revision)
                check(result is GameActionResult.Applied) {
                    "Lawful training was rejected for seed $seed: $result"
                }
                state = result.gameState
                if (state.player.level == beforeLevel + 1) {
                    trainingSuccesses += 1
                    levels.putIfAbsent(state.player.level, day)
                    attemptsPerSuccessfulPromotion += currentPromotionAttempts.remove(targetLevel) ?: 1
                    currentTrainingFailureStreak = 0
                } else {
                    check(state.player.level == beforeLevel)
                    trainingFailures += 1
                    trainingFailuresByTargetLevel[targetLevel] =
                        (trainingFailuresByTargetLevel[targetLevel] ?: 0) + 1
                    currentTrainingFailureStreak += 1
                    maximumTrainingFailureStreak = max(
                        maximumTrainingFailureStreak,
                        currentTrainingFailureStreak,
                    )
                }
            }

            fun withdrawFor(amountNeeded: Int): Boolean {
                if (amountNeeded <= state.player.gold) return true
                val shortfall = amountNeeded - state.player.gold
                if (shortfall > state.player.bankedGold) return false
                state = EconomyEngine.withdraw(state, state.revision, shortfall).applied()
                return true
            }

            fun healIfPolicyRequests(): Boolean {
                if (state.player.currentHp >= state.player.maximumHp) return true
                val hpPercent = state.player.currentHp * 100 / state.player.maximumHp
                if (hpPercent > policy.healAtHpPercent) return true
                val cost = state.player.maximumHp - state.player.currentHp
                if (!withdrawFor(cost)) return hpPercent > policy.stopAtHpPercentWithoutHealing
                val before = state.player.gold + state.player.bankedGold
                val result = game.healFully(state, state.revision)
                if (result is GameActionResult.Applied) {
                    state = result.gameState
                    healingSpent += before - state.player.gold - state.player.bankedGold
                    return true
                }
                return hpPercent > policy.stopAtHpPercentWithoutHealing
            }

            fun buyAvailableUpgrades() {
                if (policy == CampaignPolicy.PASSIVE && day % 5 != 0) return
                repeat(policy.maximumUpgradesPerVisit) {
                    val weapon = EquipmentCatalog.nextWeapon(state.player.weaponId)
                        ?.takeIf { state.player.level >= it.minimumLevel }
                    val armor = EquipmentCatalog.nextArmor(state.player.armorId)
                        ?.takeIf { state.player.level >= it.minimumLevel }
                    data class UpgradeCandidate(val id: String, val cost: Int, val weapon: Boolean)
                    val weaponCandidate = weapon?.let {
                        UpgradeCandidate(
                            id = it.id,
                            cost = EconomyRules.weaponUpgradeCost(state.player, it),
                            weapon = true,
                        )
                    }
                    val armorCandidate = armor?.let {
                        UpgradeCandidate(
                            id = it.id,
                            cost = EconomyRules.armorUpgradeCost(state.player, it),
                            weapon = false,
                        )
                    }
                    val ordered = when (policy) {
                        CampaignPolicy.AGGRESSIVE,
                        CampaignPolicy.RECKLESS,
                        -> listOfNotNull(weaponCandidate, armorCandidate)
                        else -> if (
                            EquipmentCatalog.requireWeapon(state.player.weaponId).tier <=
                            EquipmentCatalog.requireArmor(state.player.armorId).tier
                        ) {
                            listOfNotNull(weaponCandidate, armorCandidate)
                        } else {
                            listOfNotNull(armorCandidate, weaponCandidate)
                        }
                    }
                    var purchased = false
                    for (item in ordered) {
                        if (!withdrawFor(item.cost)) continue
                        val beforeWealth = state.player.gold + state.player.bankedGold
                        val result = if (item.weapon) {
                            EconomyEngine.buyWeapon(state, state.revision, item.id)
                        } else {
                            EconomyEngine.buyArmor(state, state.revision, item.id)
                        }
                        if (result is GameActionResult.Applied) {
                            state = result.gameState
                            equipmentSpent += beforeWealth - state.player.gold - state.player.bankedGold
                            equipmentDays.putIfAbsent(item.id, day)
                            purchased = true
                            break
                        }
                    }
                    if (!purchased) return
                }
            }

            fun bankSurplus() {
                if (policy.carryGoldTarget == Int.MAX_VALUE) return
                val surplus = state.player.gold - policy.carryGoldTarget
                if (surplus > 0) {
                    state = EconomyEngine.deposit(state, state.revision, surplus).applied()
                }
            }

            fun updateReadiness() {
                val playerReady = DragonReadinessRules.player(state).isReady
                if (playerReady && firstPlayerReadyDay == null) firstPlayerReadyDay = day
                if (policy == CampaignPolicy.CASUAL && playerReady && casualFirstReadyDay == null) {
                    casualFirstReadyDay = day
                }
                val readyNpcs = readyNpcCount(state)
                if (readyNpcs > 0 && firstNpcReadyDay == null) firstNpcReadyDay = day
                if (state.season.stage == DragonStage.CHALLENGE_WINDOW &&
                    challengeWindowReadyCandidates < 0
                ) {
                    challengeWindowReadyCandidates = readyNpcs + if (playerReady) 1 else 0
                }
                if (state.season.stage == DragonStage.CHALLENGE_WINDOW) {
                    latestReadyCandidateCount = readyNpcs + if (playerReady) 1 else 0
                }
            }

            trainWhileEligible()
            buyAvailableUpgrades()
            healIfPolicyRequests()
            updateReadiness()

            if (shouldAttemptDragon(state, policy, casualFirstReadyDay)) {
                val attemptReadiness = DragonReadinessRules.player(state)
                val attemptPlayer = state.player
                val attemptRank = CommunityRules.rankOf(state, state.player.id)
                val attemptNumber = state.season.attempts.count { it.challengerId == state.player.id } + 1
                state = dragon.begin(state, state.revision).applied()
                while (state.activeDragonCombat?.status == DragonCombatStatus.ACTIVE) {
                    val hpPercent = state.player.currentHp * 100 / state.player.maximumHp
                    val retreatAt = when (policy) {
                        CampaignPolicy.CASUAL -> 42
                        CampaignPolicy.ORDINARY -> 10
                        else -> 0
                    }
                    val dragonCombat = requireNotNull(state.activeDragonCombat)
                    val dragonDefense = if (dragonCombat.crystalHideBroken) {
                        DragonSeasonBalance.DRAGON_FRACTURED_DEFENSE
                    } else {
                        DragonSeasonBalance.DRAGON_CRYSTAL_DEFENSE
                    }
                    val maximumPlayerDamage = max(
                        GameBalance.MINIMUM_DAMAGE,
                        state.player.attack + GameBalance.DAMAGE_RANDOM_VALUES - 1 - dragonDefense,
                    )
                    val canFinishNow = dragonCombat.dragonCurrentHp <= maximumPlayerDamage
                    state = if (retreatAt > 0 && hpPercent <= retreatAt && !canFinishNow) {
                        dragon.retreat(state, state.revision).applied()
                    } else {
                        dragon.attack(state, state.revision).applied()
                    }
                }
                val outcome = state.season.attempts.last { it.challengerId == state.player.id }.outcome
                val attemptRecord = state.season.attempts.last { it.challengerId == state.player.id }
                dragonAttemptMetrics += DragonAttemptMetric(
                    challengerType = "PLAYER",
                    challengerId = attemptPlayer.id,
                    day = day,
                    level = attemptPlayer.level,
                    rank = attemptRank,
                    weaponId = attemptPlayer.weaponId,
                    armorId = attemptPlayer.armorId,
                    attack = attemptPlayer.attack,
                    defense = attemptPlayer.defense,
                    maximumHp = attemptPlayer.maximumHp,
                    currentHp = attemptPlayer.currentHp,
                    readinessTier = attemptReadiness.tier,
                    combatPower = attemptReadiness.combatPower,
                    dragonPower = attemptRecord.dragonPower,
                    attemptNumber = attemptNumber,
                    outcome = outcome,
                )
                dayDragonOutcomes += outcome
                playerDragonAttempts += outcome
                state = dragon.leave(state, state.revision).applied()
            }

            if (state.season.stage != DragonStage.RESOLVED && !state.playerDay.wasDefeatedToday) {
                val fightLimit = policy.dailyFightLimit(day)
                    .coerceIn(0, GameBalance.FOREST_FIGHTS_PER_DAY)
                while (fightsAttempted < fightLimit && state.playerDay.fightsRemaining > 0) {
                    trainWhileEligible()
                    if (!healIfPolicyRequests()) break
                    val hpAtEncounterStart = state.player.currentHp
                    state = game.startForestEncounter(state, state.revision).applied()
                    fightsAttempted += 1
                    val monsterId = requireNotNull(state.activeCombat).monsterId
                    var damageTaken = 0
                    while (state.activeCombat?.status == CombatStatus.ACTIVE) {
                        val combat = requireNotNull(state.activeCombat)
                        val monster = MonsterCatalog.require(combat.monsterId)
                        val maximumIncoming = max(
                            GameBalance.MINIMUM_DAMAGE,
                            monster.attack + GameBalance.DAMAGE_RANDOM_VALUES - 1 - state.player.defense,
                        )
                        val run = policy.runAtIncomingHits > 0 &&
                            state.player.currentHp <= maximumIncoming * policy.runAtIncomingHits
                        val beforeHp = state.player.currentHp
                        state = if (run) {
                            game.runAway(state, state.revision).applied()
                        } else {
                            game.attack(state, state.revision).applied()
                        }
                        damageTaken += if (state.activeCombat?.status == CombatStatus.DEFEAT) {
                            beforeHp
                        } else {
                            (beforeHp - state.player.currentHp).coerceAtLeast(0)
                        }
                    }
                    totalDamageTaken += damageTaken
                    val completed = requireNotNull(state.activeCombat)
                    var metric = MonsterSeasonMetric(
                        encounters = 1,
                        damageTaken = damageTaken,
                        encounterDaySum = day,
                        playerLevelSum = state.player.level,
                    )
                    when (completed.status) {
                        CombatStatus.VICTORY -> {
                            fightsWon += 1
                            combatXp += completed.experienceReward
                            combatGold += completed.goldReward
                            val hpPercent = state.player.currentHp * 100 / state.player.maximumHp
                            victoryHpPercentages += hpPercent
                            metric = metric.copy(
                                victories = 1,
                                experienceEarned = completed.experienceReward,
                                goldEarned = completed.goldReward,
                            )
                        }
                        CombatStatus.DEFEAT -> {
                            fightsLost += 1
                            defeatGoldLost += completed.goldLost
                            metric = metric.copy(defeats = 1)
                        }
                        CombatStatus.ESCAPED -> {
                            escapes += 1
                            metric = metric.copy(escapes = 1)
                        }
                        CombatStatus.ACTIVE -> error("Combat did not terminate")
                    }
                    monsterMetrics[monsterId] = (monsterMetrics[monsterId] ?: MonsterSeasonMetric()) + metric
                    state = game.leaveCompletedCombat(state, state.revision).applied()
                    if (fightsLost > 0) break
                    check(state.player.currentHp <= state.player.maximumHp)
                    check(hpAtEncounterStart > 0)
                }
            }

            trainWhileEligible()
            buyAvailableUpgrades()

            if (shouldChallenge(state, policy)) {
                val opponent = chooseChallengeOpponent(state, policy)
                if (opponent != null) {
                    val started = challenge.begin(state, state.revision, opponent.id)
                    if (started is GameActionResult.Applied) {
                        state = started.gameState
                        if (state.activeChallenge?.opportunityConsumed == true) challengeAttempts += 1
                        while (state.activeChallenge?.status == ChallengeStatus.ACTIVE) {
                            val hpPercent = state.player.currentHp * 100 / state.player.maximumHp
                            state = if (
                                policy.challengeYieldHpPercent > 0 &&
                                hpPercent <= policy.challengeYieldHpPercent
                            ) {
                                challenge.yield(state, state.revision).applied()
                            } else {
                                challenge.attack(state, state.revision).applied()
                            }
                        }
                        when (state.activeChallenge?.status) {
                            ChallengeStatus.PLAYER_VICTORY -> challengeWins += 1
                            ChallengeStatus.PLAYER_DEFEAT -> challengeLosses += 1
                            ChallengeStatus.YIELDED -> challengeYields += 1
                            else -> Unit
                        }
                        state = challenge.leave(state, state.revision).applied()
                    }
                }
            }

            buyAvailableUpgrades()
            bankSurplus()
            updateReadiness()

            val hpBeforeSleep = state.player.currentHp
            val fightsUnused = state.playerDay.fightsRemaining
            val beforeWorld = state
            val previousNpc = beforeWorld.community.adventurers.associateBy { it.id }
            val previousIncidents = beforeWorld.season.incidents.size
            val previousAttempts = beforeWorld.season.attempts.map { it.id }.toSet()

            if (state.season.stage != DragonStage.RESOLVED) {
                state = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                    state,
                    state.revision,
                ).applied()
                if (state.currentDay in roundTripDays) {
                    state = requireNotNull(stateRoundTrip) { "No state round-trip adapter supplied" }(state)
                }
            }

            val resolvedDay = if (state.season.stage == DragonStage.RESOLVED) {
                state.season.resolvedDay
            } else {
                day
            }
            val record = state.community.chronicleHistory.firstOrNull { it.day == resolvedDay }
            val events = record?.events.orEmpty()
            events.forEach { event ->
                chronicleTypes[event.type] = (chronicleTypes[event.type] ?: 0) + 1
                event.relatedPriorEventId?.let { priorId ->
                    callbacks += 1
                    callbackPriorEventIds += priorId
                    callbackTypeCounts[event.type] = (callbackTypeCounts[event.type] ?: 0) + 1
                }
                sequenceOf(event.characterId, event.relatedCharacterId)
                    .filterNotNull()
                    .filter { it != state.player.id }
                    .forEach { id -> npcAppearances[id] = (npcAppearances[id] ?: 0) + 1 }
            }

            val newAttempts = state.season.attempts.filter { it.id !in previousAttempts }
            if (newAttempts.isNotEmpty()) {
                val firstAttemptDay = newAttempts.minOf { it.day }
                stages.putIfAbsent(DragonStage.CHALLENGE_WINDOW, firstAttemptDay)
                challengeWindowReadyCandidates = max(challengeWindowReadyCandidates, 1)
                latestReadyCandidateCount = max(latestReadyCandidateCount, 1)
            }
            newAttempts.filter { it.challengerId != state.player.id }.forEach { attempt ->
                val npcBeforeAttempt = state.community.adventurers.first { it.id == attempt.challengerId }
                val npcTier = when {
                    attempt.challengerPower >= DragonSeasonBalance.NPC_MINIMUM_COMBAT_POWER + 15 ->
                        DragonReadinessTier.WELL_PREPARED
                    attempt.challengerPower >= DragonSeasonBalance.NPC_MINIMUM_COMBAT_POWER + 8 ->
                        DragonReadinessTier.READY
                    else -> DragonReadinessTier.DANGEROUS
                }
                dragonAttemptMetrics += DragonAttemptMetric(
                    challengerType = "NPC",
                    challengerId = attempt.challengerId,
                    day = attempt.day,
                    level = npcBeforeAttempt.level,
                    rank = CommunityRules.rankOf(beforeWorld, attempt.challengerId),
                    weaponId = npcBeforeAttempt.weaponId,
                    armorId = npcBeforeAttempt.armorId,
                    attack = npcBeforeAttempt.attack,
                    defense = npcBeforeAttempt.defense,
                    maximumHp = npcBeforeAttempt.maximumHp,
                    currentHp = npcBeforeAttempt.maximumHp,
                    readinessTier = npcTier,
                    combatPower = attempt.challengerPower,
                    dragonPower = attempt.dragonPower,
                    attemptNumber = (priorNpcAttemptCounts[attempt.challengerId] ?: 0) + 1,
                    outcome = attempt.outcome,
                )
                if (firstNpcReadyDay == null) firstNpcReadyDay = attempt.day
                val prior = priorNpcAttemptCounts[attempt.challengerId] ?: 0
                if (prior > 0) repeatedNpcAttempts += 1
                priorNpcAttemptCounts[attempt.challengerId] = prior + 1
                npcDragonChallengers += attempt.challengerId
                npcDragonAttempts += attempt.outcome
            }

            val afterRankMap = rankMap(state)
            val dayRankChanges = dayStartRankMap.keys.count { dayStartRankMap[it] != afterRankMap[it] }
            rankPositionChanges += dayRankChanges
            val leader = CommunityRules.rankings(state).first().characterId
            if (leader != priorLeader) leadershipChanges += 1
            priorLeader = leader
            leaders += leader
            playerEverRankOne = playerEverRankOne || leader == state.player.id

            val relationshipDeltaCount = state.community.adventurers.count { npc ->
                val previous = dayStartNpc[npc.id]
                previous != null && (
                    previous.familiarity != npc.familiarity || previous.rivalry != npc.rivalry
                    )
            }
            relationshipChanges += relationshipDeltaCount
            val npcEquipmentUpgrades = state.community.adventurers.count { npc ->
                val previous = previousNpc[npc.id]
                previous != null && (
                    previous.weaponId != npc.weaponId || previous.armorId != npc.armorId
                    )
            }
            val newIncidents = (state.season.incidents.size - previousIncidents).coerceAtLeast(0)
            incidentsCreated += newIncidents

            if (state.season.stage !in stages) {
                stages[state.season.stage] = if (state.season.stage == DragonStage.RESOLVED) {
                    state.season.resolvedDay
                } else {
                    state.season.stageStartedDay
                }
            }
            updateReadiness()
            val readyNpcs = readyNpcCount(state)
            val playerRank = CommunityRules.rankOf(state, state.player.id)
            val spotlight = events.flatMap { listOfNotNull(it.characterId, it.relatedCharacterId) }
                .filter { it != state.player.id }
                .distinct()
            val typeCounts = events.groupingBy { it.type }.eachCount()

            days += CampaignDayMetric(
                day = day,
                playerLevel = state.player.level,
                playerExperience = state.player.experience,
                playerRank = playerRank,
                currentHpBeforeSleep = hpBeforeSleep,
                maximumHp = state.player.maximumHp,
                carriedGold = state.player.gold,
                bankedGold = state.player.bankedGold,
                weaponId = state.player.weaponId,
                armorId = state.player.armorId,
                fightsAvailable = GameBalance.FOREST_FIGHTS_PER_DAY,
                fightsAttempted = fightsAttempted,
                fightsWon = fightsWon,
                fightsLost = fightsLost,
                escapes = escapes,
                fightsUnused = fightsUnused,
                experienceEarned = state.player.experience - startXp,
                combatGoldEarned = combatGold,
                healingSpent = healingSpent,
                equipmentSpent = equipmentSpent,
                defeatGoldLost = defeatGoldLost,
                trainingAttempts = trainingAttempts,
                trainingSuccesses = trainingSuccesses,
                trainingFailures = trainingFailures,
                challengeAttempts = challengeAttempts,
                challengeWins = challengeWins,
                challengeLosses = challengeLosses,
                challengeYields = challengeYields,
                dragonAttempts = dayDragonOutcomes.size,
                dragonAttemptOutcomes = dayDragonOutcomes,
                dragonStage = state.season.stage,
                chronicleEntries = events.size,
                chronicleTypeCounts = typeCounts,
                spotlightNpcIds = spotlight,
                callbacks = events.count { it.relatedPriorEventId != null },
                relationshipChanges = relationshipDeltaCount,
                rankPositionChanges = dayRankChanges,
                leaderId = leader,
                npcLevels = state.community.adventurers.map { it.level },
                readyNpcCandidates = readyNpcs,
                newDragonIncidents = newIncidents,
                npcEquipmentUpgrades = npcEquipmentUpgrades,
            )
            check(GameStateValidator.validate(state) == GameStateValidationResult.Valid) {
                "Invalid state after seed=$seed policy=$policy day=$day"
            }
        }

        val completed = state.season.stage == DragonStage.RESOLVED
        val resolutionDay = if (completed) state.season.resolvedDay else days.lastOrNull()?.day ?: 0
        val finalRank = CommunityRules.rankOf(state, state.player.id)
        val finalReady = max(
            latestReadyCandidateCount,
            if (completed && state.season.attempts.isNotEmpty()) 1 else 0,
        )
        return CampaignResult(
            seed = seed,
            policy = policy,
            completed = completed,
            resolutionDay = resolutionDay,
            victorId = state.season.victorId,
            playerVictory = state.season.victorId == state.player.id,
            npcVictory = completed && state.season.victorId != state.player.id,
            finalPlayerLevel = state.player.level,
            finalPlayerExperience = state.player.experience,
            finalPlayerRank = finalRank,
            finalCarriedGold = state.player.gold,
            finalBankedGold = state.player.bankedGold,
            finalWeaponId = state.player.weaponId,
            finalArmorId = state.player.armorId,
            levelReachedDays = levels,
            equipmentAcquiredDays = equipmentDays,
            stageEnteredDays = stages,
            firstPlayerReadyDay = firstPlayerReadyDay,
            firstNpcReadyDay = firstNpcReadyDay,
            challengeWindowReadyCandidates = max(
                challengeWindowReadyCandidates,
                if (completed && state.season.attempts.isNotEmpty()) 1 else 0,
            ),
            finalReadyCandidates = finalReady,
            playerDragonAttempts = playerDragonAttempts,
            npcDragonAttempts = npcDragonAttempts,
            uniqueNpcDragonChallengers = npcDragonChallengers.size,
            repeatedNpcDragonAttempts = repeatedNpcAttempts,
            dragonAttemptMetrics = dragonAttemptMetrics,
            totalFightsAttempted = days.sumOf { it.fightsAttempted },
            totalFightsWon = days.sumOf { it.fightsWon },
            totalFightsLost = days.sumOf { it.fightsLost },
            totalEscapes = days.sumOf { it.escapes },
            totalUnusedFights = days.sumOf { it.fightsUnused },
            totalCombatDamageTaken = totalDamageTaken,
            victoryHpPercentages = victoryHpPercentages,
            totalCombatExperience = monsterMetrics.values.sumOf { it.experienceEarned },
            totalCombatGold = days.sumOf { it.combatGoldEarned },
            totalHealingSpent = days.sumOf { it.healingSpent },
            totalEquipmentSpent = days.sumOf { it.equipmentSpent },
            totalDefeatGoldLost = days.sumOf { it.defeatGoldLost },
            totalTrainingAttempts = days.sumOf { it.trainingAttempts },
            totalTrainingSuccesses = days.sumOf { it.trainingSuccesses },
            totalTrainingFailures = days.sumOf { it.trainingFailures },
            trainingAttemptsByTargetLevel = trainingAttemptsByTargetLevel,
            trainingFailuresByTargetLevel = trainingFailuresByTargetLevel,
            attemptsPerSuccessfulPromotion = attemptsPerSuccessfulPromotion,
            maximumTrainingFailureStreak = maximumTrainingFailureStreak,
            totalChallengeAttempts = days.sumOf { it.challengeAttempts },
            totalChallengeWins = days.sumOf { it.challengeWins },
            totalChallengeLosses = days.sumOf { it.challengeLosses },
            totalChallengeYields = days.sumOf { it.challengeYields },
            relationshipChanges = relationshipChanges,
            rivalryFormedCount = state.community.adventurers.count { it.rivalry >= 3 },
            rankPositionChanges = rankPositionChanges,
            leadershipChanges = leadershipChanges,
            uniqueLeaderIds = leaders.toList(),
            playerEverRankOne = playerEverRankOne,
            chronicleEntries = days.sumOf { it.chronicleEntries },
            chronicleDays = days.count { it.chronicleEntries > 0 },
            chronicleTypeCounts = chronicleTypes,
            chronicleNpcAppearances = npcAppearances,
            incidentsCreated = incidentsCreated,
            callbacks = callbacks,
            callbackPriorEventIds = callbackPriorEventIds,
            callbackTypeCounts = callbackTypeCounts,
            npcFinalLevels = state.community.adventurers.map { it.level },
            npcFinalGold = state.community.adventurers.map { it.gold },
            npcFinalWeaponTiers = state.community.adventurers.map {
                EquipmentCatalog.requireWeapon(it.weaponId).tier
            },
            npcFinalArmorTiers = state.community.adventurers.map {
                EquipmentCatalog.requireArmor(it.armorId).tier
            },
            days = days,
            monsterMetrics = monsterMetrics,
            finalState = state,
        )
    }

    private fun shouldAttemptDragon(
        state: GameState,
        policy: CampaignPolicy,
        casualFirstReadyDay: Int?,
    ): Boolean {
        val readiness = DragonReadinessRules.player(state)
        if (!readiness.isReady) return false
        return when (policy) {
            CampaignPolicy.PASSIVE -> false
            CampaignPolicy.CASUAL ->
                readiness.tier == DragonReadinessTier.WELL_PREPARED ||
                    (casualFirstReadyDay != null && state.currentDay >= casualFirstReadyDay + 2)
            CampaignPolicy.ORDINARY -> readiness.combatPower >= 130
            CampaignPolicy.AGGRESSIVE,
            CampaignPolicy.RECKLESS,
            -> true
        }
    }

    private fun shouldChallenge(state: GameState, policy: CampaignPolicy): Boolean =
        policy.challengeIntervalDays > 0 &&
            state.currentDay % policy.challengeIntervalDays == 0 &&
            state.playerDay.challengesRemaining > 0 &&
            !state.playerDay.wasDefeatedToday &&
            state.season.stage != DragonStage.RESOLVED &&
            state.player.currentHp > 0

    private fun chooseChallengeOpponent(
        state: GameState,
        policy: CampaignPolicy,
    ): NpcAdventurer? {
        val active = CommunityRules.innVisitors(state).filter { it.status == NpcStatus.ACTIVE }
        return when (policy) {
            CampaignPolicy.PASSIVE -> null
            CampaignPolicy.CASUAL -> active.filter { it.level <= state.player.level }.minByOrNull { it.level }
            CampaignPolicy.ORDINARY -> active.filter { it.level <= state.player.level + 1 }
                .minByOrNull { kotlin.math.abs(it.level - state.player.level) }
            CampaignPolicy.AGGRESSIVE,
            CampaignPolicy.RECKLESS,
            -> active.maxWithOrNull(compareBy<NpcAdventurer> { it.level }.thenBy { it.attack + it.defense })
        }
    }

    private fun readyNpcCount(state: GameState): Int = state.community.adventurers.count {
        DragonReadinessRules.npc(it, state.season.stage).isReady
    }

    private fun rankMap(state: GameState): Map<String, Int> =
        CommunityRules.rankings(state).mapIndexed { index, entry -> entry.characterId to index + 1 }.toMap()

    private fun GameActionResult.applied(): GameState =
        (this as? GameActionResult.Applied)?.gameState
            ?: error("Expected applied game action, got $this")
}
