package com.legendoftheglassdragon.balance

import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.GameActionRejection
import com.legendoftheglassdragon.game.GameActionResult
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameEngine
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import com.legendoftheglassdragon.game.MonsterCatalog
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.WorldAdvancer
import kotlin.math.ceil
import kotlin.math.max

enum class PlayPolicy(
    val healAtHpPercent: Int,
    val stopAtHpPercentWithoutHealing: Int,
    val runAtIncomingHits: Int,
) {
    CAUTIOUS(
        healAtHpPercent = 75,
        stopAtHpPercentWithoutHealing = 55,
        runAtIncomingHits = 2,
    ),
    ORDINARY(
        healAtHpPercent = 48,
        stopAtHpPercentWithoutHealing = 30,
        runAtIncomingHits = 1,
    ),
    AGGRESSIVE(
        healAtHpPercent = 40,
        stopAtHpPercentWithoutHealing = 0,
        runAtIncomingHits = 0,
    ),
}

data class BalanceDay(
    val day: Int,
    val startingLevel: Int,
    val endingLevel: Int,
    val endingExperience: Int,
    val endingGold: Int,
    val endingHp: Int,
    val fightsAttempted: Int,
    val fightsWon: Int,
    val escapes: Int,
    val defeated: Boolean,
    val fightNumberAtDefeat: Int?,
    val trainingEligibleAtEnd: Boolean,
    val trainingAttempts: Int,
    val promotions: Int,
)

data class CampaignTrace(
    val seed: Long,
    val policy: PlayPolicy,
    val days: List<BalanceDay>,
    val levelReachedDays: Map<Int, Int>,
    val totalFightsAttempted: Int,
    val totalFightsWon: Int,
    val totalDefeats: Int,
    val totalTrainingAttempts: Int,
    val totalPromotions: Int,
    val maximumConsecutiveDefeatedDays: Int,
    val finalState: GameState,
)

data class CohortSummary(
    val policy: PlayPolicy,
    val seedCount: Int,
    val realmDays: Int,
    val dayOneMedianFightsWon: Int,
    val dayOneMedianExperience: Int,
    val dayOneLevelTwoRatePercent: Double,
    val levelTwoMedianDay: Int,
    val levelTwoP90Day: Int,
    val medianDayByLevel: Map<Int, Int>,
    val p90DayByLevel: Map<Int, Int>,
    val dayTwelveLevelDistribution: Map<Int, Int>,
    val finalLevelDistribution: Map<Int, Int>,
    val defeatDayRatePercent: Double,
    val medianFightsBeforeDefeat: Int?,
    val finalGoldMedian: Int,
    val dayTwelveGoldMedian: Int,
    val maximumConsecutiveDefeatedDays: Int,
) {
    override fun toString(): String = buildString {
        append(policy.name)
        append(" seeds=").append(seedCount)
        append(" days=").append(realmDays)
        append(" day1WinsMedian=").append(dayOneMedianFightsWon)
        append(" day1XpMedian=").append(dayOneMedianExperience)
        append(" day1L2=").append("%.1f".format(dayOneLevelTwoRatePercent)).append('%')
        append(" l2MedianDay=").append(levelTwoMedianDay)
        append(" l2P90Day=").append(levelTwoP90Day)
        append(" levelMedianDays=").append(medianDayByLevel)
        append(" levelP90Days=").append(p90DayByLevel)
        append(" defeatDays=").append("%.1f".format(defeatDayRatePercent)).append('%')
        append(" medianFightsBeforeDefeat=").append(medianFightsBeforeDefeat ?: "none")
        append(" day12Levels=").append(dayTwelveLevelDistribution)
        append(" finalLevels=").append(finalLevelDistribution)
        append(" goldMedianDay12/final=").append(dayTwelveGoldMedian).append('/').append(finalGoldMedian)
        append(" maxConsecutiveDefeats=").append(maximumConsecutiveDefeatedDays)
    }
}

object BalanceHarness {
    fun runCampaign(
        seed: Long,
        policy: PlayPolicy,
        realmDays: Int,
    ): CampaignTrace {
        require(realmDays >= 1)
        val engine = GameEngine()
        var state = (
            NewGameFactory(
                idProvider = { "balance-player-$seed" },
                seedProvider = { seed },
            ).create("Tester") as NewGameCreationResult.Created
            ).gameState
        val records = mutableListOf<BalanceDay>()
        val levelReachedDays = linkedMapOf(1 to 1)
        var totalTrainingAttempts = 0
        var totalPromotions = 0
        var consecutiveDefeatedDays = 0
        var maximumConsecutiveDefeatedDays = 0

        repeat(realmDays) { dayIndex ->
            val day = state.currentDay
            val startingLevel = state.player.level
            var fightsAttempted = 0
            var fightsWon = 0
            var escapes = 0
            var defeated = false
            var defeatFightNumber: Int? = null
            var trainingAttempts = 0
            var promotions = 0

            fun trainWhileEligible() {
                if (ProgressionRules.isTrainingEligible(state.player) &&
                    !state.playerDay.trainingAttemptedToday
                ) {
                    trainingAttempts++
                    totalTrainingAttempts++
                    val beforeLevel = state.player.level
                    state = engine.train(state, state.revision).applied()
                    if (state.player.level == beforeLevel + 1) {
                    promotions += 1
                    totalPromotions += 1
                    levelReachedDays.putIfAbsent(state.player.level, day)
                    } else {
                        check(state.player.level == beforeLevel)
                    }
                }
            }

            trainWhileEligible()

            while (state.playerDay.fightsRemaining > 0 && !defeated) {
                val hpPercent = state.player.currentHp * 100 / state.player.maximumHp
                if (hpPercent <= policy.healAtHpPercent && state.player.currentHp < state.player.maximumHp) {
                    when (val healed = engine.healFully(state, state.revision)) {
                        is GameActionResult.Applied -> state = healed.gameState
                        is GameActionResult.Rejected -> {
                            check(healed.reason is GameActionRejection.NotEnoughGold)
                            if (hpPercent <= policy.stopAtHpPercentWithoutHealing) break
                        }
                    }
                }

                state = engine.startForestEncounter(state, state.revision).applied()
                fightsAttempted += 1

                while (state.activeCombat?.status == CombatStatus.ACTIVE) {
                    val combat = requireNotNull(state.activeCombat)
                    val monster = MonsterCatalog.require(combat.monsterId)
                    val maximumIncomingDamage = max(
                        GameBalance.MINIMUM_DAMAGE,
                        monster.attack + GameBalance.DAMAGE_RANDOM_VALUES - 1 - state.player.defense,
                    )
                    val shouldRun = policy.runAtIncomingHits > 0 &&
                        state.player.currentHp <= maximumIncomingDamage * policy.runAtIncomingHits
                    state = if (shouldRun) {
                        engine.runAway(state, state.revision).applied()
                    } else {
                        engine.attack(state, state.revision).applied()
                    }
                }

                when (requireNotNull(state.activeCombat).status) {
                    CombatStatus.VICTORY -> fightsWon += 1
                    CombatStatus.ESCAPED -> escapes += 1
                    CombatStatus.DEFEAT -> {
                        defeated = true
                        defeatFightNumber = fightsAttempted
                    }
                    CombatStatus.ACTIVE -> error("Combat loop ended while active")
                }
                state = engine.leaveCompletedCombat(state, state.revision).applied()
                if (!defeated) trainWhileEligible()
            }

            if (defeated) {
                consecutiveDefeatedDays += 1
                maximumConsecutiveDefeatedDays = max(
                    maximumConsecutiveDefeatedDays,
                    consecutiveDefeatedDays,
                )
            } else {
                consecutiveDefeatedDays = 0
            }

            records += BalanceDay(
                day = day,
                startingLevel = startingLevel,
                endingLevel = state.player.level,
                endingExperience = state.player.experience,
                endingGold = state.player.gold,
                endingHp = state.player.currentHp,
                fightsAttempted = fightsAttempted,
                fightsWon = fightsWon,
                escapes = escapes,
                defeated = defeated,
                fightNumberAtDefeat = defeatFightNumber,
                trainingEligibleAtEnd = ProgressionRules.isTrainingEligible(state.player),
                trainingAttempts = trainingAttempts,
                promotions = promotions,
            )
            check(GameStateValidator.validate(state) == GameStateValidationResult.Valid)

            if (dayIndex < realmDays - 1) {
                state = WorldAdvancer.advanceWorldToNextDayAndSkipPresentation(
                    state.copy(season = com.legendoftheglassdragon.game.DragonSeasonState()),
                    state.revision,
                ).applied().copy(season = com.legendoftheglassdragon.game.DragonSeasonState())
                check(state.player.currentHp == state.player.maximumHp)
                check(GameStateValidator.validate(state) == GameStateValidationResult.Valid)
            }
        }

        return CampaignTrace(
            seed = seed,
            policy = policy,
            days = records,
            levelReachedDays = levelReachedDays,
            totalFightsAttempted = records.sumOf { it.fightsAttempted },
            totalFightsWon = records.sumOf { it.fightsWon },
            totalDefeats = records.count { it.defeated },
            totalTrainingAttempts = totalTrainingAttempts,
            totalPromotions = totalPromotions,
            maximumConsecutiveDefeatedDays = maximumConsecutiveDefeatedDays,
            finalState = state,
        )
    }

    fun summarize(traces: List<CampaignTrace>): CohortSummary {
        require(traces.isNotEmpty())
        val policy = traces.first().policy
        val realmDays = traces.first().days.size
        require(traces.all { it.policy == policy && it.days.size == realmDays })
        val levelTwoDays = traces.map { it.levelReachedDays[2] ?: realmDays + 1 }
        val trackedLevels = 2..10
        val defeatFightNumbers = traces.flatMap { trace ->
            trace.days.mapNotNull { it.fightNumberAtDefeat }
        }
        return CohortSummary(
            policy = policy,
            seedCount = traces.size,
            realmDays = realmDays,
            dayOneMedianFightsWon = percentile(traces.map { it.days.first().fightsWon }, 0.5),
            dayOneMedianExperience = percentile(traces.map { it.days.first().endingExperience }, 0.5),
            dayOneLevelTwoRatePercent = traces.count { (it.levelReachedDays[2] ?: Int.MAX_VALUE) <= 1 } *
                100.0 / traces.size,
            levelTwoMedianDay = percentile(levelTwoDays, 0.5),
            levelTwoP90Day = percentile(levelTwoDays, 0.9),
            medianDayByLevel = trackedLevels.associateWith { level ->
                percentile(traces.map { it.levelReachedDays[level] ?: realmDays + 1 }, 0.5)
            },
            p90DayByLevel = trackedLevels.associateWith { level ->
                percentile(traces.map { it.levelReachedDays[level] ?: realmDays + 1 }, 0.9)
            },
            dayTwelveLevelDistribution = traces.groupingBy {
                it.days[minOf(11, realmDays - 1)].endingLevel
            }.eachCount().toSortedMap(),
            finalLevelDistribution = traces.groupingBy { it.finalState.player.level }
                .eachCount()
                .toSortedMap(),
            defeatDayRatePercent = traces.sumOf { it.totalDefeats } * 100.0 /
                (traces.size * realmDays),
            medianFightsBeforeDefeat = defeatFightNumbers.takeIf { it.isNotEmpty() }
                ?.let { percentile(it, 0.5) },
            finalGoldMedian = percentile(traces.map { it.finalState.player.gold }, 0.5),
            dayTwelveGoldMedian = percentile(
                traces.map { it.days[minOf(11, realmDays - 1)].endingGold },
                0.5,
            ),
            maximumConsecutiveDefeatedDays = traces.maxOf { it.maximumConsecutiveDefeatedDays },
        )
    }

    private fun percentile(values: List<Int>, fraction: Double): Int {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val index = (ceil(fraction * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index]
    }

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}
