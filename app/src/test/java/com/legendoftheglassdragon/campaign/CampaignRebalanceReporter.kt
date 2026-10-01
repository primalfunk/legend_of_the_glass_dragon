package com.legendoftheglassdragon.campaign

import com.legendoftheglassdragon.data.DecodeResult
import com.legendoftheglassdragon.data.GameStateCodec
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.WorldEventType
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.ceil

data class Lgd011bQualification(
    val deterministicCases: List<String>,
    val saveReloadCases: List<String>,
)

/** LGD-011B evidence generation. This stays in the development-only JVM source set. */
object CampaignRebalanceReporter {
    private const val POPULATION_SIZE = 16

    private data class QualificationCase(
        val label: String,
        val predicate: (CampaignResult) -> Boolean,
    )

    fun qualify(results: List<CampaignResult>, requireFullCoverage: Boolean): Lgd011bQualification {
        val deterministicRequests = listOf(
            QualificationCase("training_failure") { it.totalTrainingFailures > 0 },
            QualificationCase("player_dragon_defeat") { result ->
                result.playerDragonAttempts.any { it != DragonAttemptOutcome.VICTORY }
            },
            QualificationCase("npc_dragon_victory") { it.npcVictory },
            QualificationCase("player_dragon_victory") { it.playerVictory },
            QualificationCase("strong_npc_rank_one") { it.finalPlayerRank > 1 },
            QualificationCase("multiple_callbacks") { it.callbacks >= 4 },
            QualificationCase("ordinary_retry_victory") { result ->
                result.policy == CampaignPolicy.ORDINARY && result.playerVictory &&
                    result.playerDragonAttempts.size > 1
            },
            QualificationCase("casual_dragon_attempt") { result ->
                result.policy == CampaignPolicy.CASUAL && result.playerDragonAttempts.isNotEmpty()
            },
            QualificationCase("multiple_credible_candidates") { candidateCount(it) >= 2 },
            QualificationCase("reckless_combat_defeat") { result ->
                result.policy == CampaignPolicy.RECKLESS && result.totalFightsLost > 0
            },
        )
        val deterministic = qualifyCases(
            results = results,
            requests = deterministicRequests,
            requireFullCoverage = requireFullCoverage,
        ) { source -> CampaignSimulationHarness().run(source.seed, source.policy) }

        val codec = GameStateCodec()
        val roundTrip: (GameState) -> GameState = { state ->
            when (val decoded = codec.decode(codec.encode(state))) {
                is DecodeResult.Success -> decoded.gameState
                is DecodeResult.Failure -> error("LGD-011B round trip failed: ${decoded.failure}")
            }
        }
        val saveRequests = listOf(
            QualificationCase("ordinary_loss_then_later_victory") { result ->
                result.policy == CampaignPolicy.ORDINARY && result.playerVictory &&
                    result.playerDragonAttempts.dropLast(1)
                        .any { it != DragonAttemptOutcome.VICTORY }
            },
            QualificationCase("npc_beats_player_to_dragon") { result ->
                result.npcVictory && result.firstNpcReadyDay != null &&
                    (result.firstPlayerReadyDay == null ||
                        result.firstNpcReadyDay < result.firstPlayerReadyDay)
            },
            QualificationCase("strong_npc_ranking_lead") { it.finalPlayerRank > 1 },
            QualificationCase("training_failure") { it.totalTrainingFailures > 0 },
            QualificationCase("chronicle_callback_chain") {
                it.callbacks >= 4 && it.callbackPriorEventIds.size >= 2
            },
        )
        val saveReload = qualifyCases(
            results = results,
            requests = saveRequests,
            requireFullCoverage = requireFullCoverage,
        ) { source ->
            CampaignSimulationHarness(
                roundTripDays = setOf(2, 4, 7, 11, 15, 18, 19, 20, 23, 29),
                stateRoundTrip = roundTrip,
            ).run(source.seed, source.policy)
        }
        return Lgd011bQualification(deterministic, saveReload)
    }

    private fun qualifyCases(
        results: List<CampaignResult>,
        requests: List<QualificationCase>,
        requireFullCoverage: Boolean,
        rerun: (CampaignResult) -> CampaignResult,
    ): List<String> {
        return requests.mapIndexedNotNull { index, request ->
            val source = results.firstOrNull(request.predicate)
                ?: if (requireFullCoverage) {
                    error("No LGD-011B cohort seed covers qualification case: ${request.label}")
                } else {
                    results.getOrNull(index % results.size.coerceAtLeast(1))
                }
            source?.let {
                val repeated = rerun(it)
                check(it == repeated) {
                    "Qualification divergence for ${request.label}: ${it.policy} seed=${it.seed}"
                }
                "${request.label}:${it.policy.name}:${it.seed}"
            }
        }
    }

    fun writeEvidence(
        run: CharacterizationRun,
        qualification: Lgd011bQualification,
        output: Path,
    ) {
        Files.createDirectories(output)
        write(output.resolve("lgd011b_campaigns.csv"), campaignsCsv(run.results))
        write(output.resolve("lgd011b_days.jsonl"), daysJsonl(run.results))
        write(output.resolve("lgd011b_dragon_attempts.csv"), dragonAttemptsCsv(run.results))
        write(output.resolve("lgd011b_training.csv"), trainingCsv(run.results))
        write(output.resolve("lgd011b_summary.json"), summaryJson(run, qualification))
        write(output.resolve("lgd011b_outliers.json"), outliersJson(run.results))
    }

    private fun campaignsCsv(results: List<CampaignResult>) = buildString {
        appendLine(
            "seed,policy,completed,days,victor_type,victor_id,player_level,player_rank," +
                "carried_gold,banked_gold,total_wealth,weapon_id,armor_id," +
                "player_dragon_attempts,npc_dragon_attempts,training_attempts," +
                "training_failures,combat_attempts,combat_losses,chronicle_entries," +
                "callbacks,callback_incidents,max_npc_level,candidates," +
                "first_player_ready_day,first_npc_ready_day",
        )
        results.forEach { result ->
            appendLine(
                listOf(
                    result.seed,
                    result.policy.name,
                    result.completed,
                    result.resolutionDay,
                    when {
                        !result.completed -> "UNRESOLVED"
                        result.playerVictory -> "PLAYER"
                        else -> "NPC"
                    },
                    result.victorId.orEmpty(),
                    result.finalPlayerLevel,
                    result.finalPlayerRank,
                    result.finalCarriedGold,
                    result.finalBankedGold,
                    result.finalCarriedGold.toLong() + result.finalBankedGold,
                    result.finalWeaponId,
                    result.finalArmorId,
                    result.playerDragonAttempts.size,
                    result.npcDragonAttempts.size,
                    result.totalTrainingAttempts,
                    result.totalTrainingFailures,
                    result.totalFightsAttempted,
                    result.totalFightsLost,
                    result.chronicleEntries,
                    result.callbacks,
                    result.callbackPriorEventIds.size,
                    result.npcFinalLevels.maxOrNull() ?: 0,
                    candidateCount(result),
                    result.firstPlayerReadyDay ?: "",
                    result.firstNpcReadyDay ?: "",
                ).joinToString(","),
            )
        }
    }

    private fun daysJsonl(results: List<CampaignResult>) = buildString {
        results.forEach { result ->
            result.days.forEach { day ->
                append("{\"seed\":${result.seed},\"policy\":\"${result.policy.name}\"")
                append(",\"day\":${day.day},\"level\":${day.playerLevel},\"rank\":${day.playerRank}")
                append(",\"fights\":${day.fightsAttempted},\"combat_losses\":${day.fightsLost}")
                append(",\"training_attempts\":${day.trainingAttempts}")
                append(",\"training_failures\":${day.trainingFailures}")
                append(",\"dragon_attempts\":${day.dragonAttempts}")
                append(",\"dragon_stage\":\"${day.dragonStage.name}\"")
                append(",\"chronicle_entries\":${day.chronicleEntries}")
                append(",\"callbacks\":${day.callbacks},\"ready_npcs\":${day.readyNpcCandidates}}")
                appendLine()
            }
        }
    }

    private fun dragonAttemptsCsv(results: List<CampaignResult>) = buildString {
        appendLine(
            "seed,policy,challenger_type,challenger_id,day,level,rank,weapon_id," +
                "armor_id,attack,defense,maximum_hp,current_hp,readiness_tier," +
                "combat_power,dragon_power,attempt_number,outcome",
        )
        results.forEach { result ->
            result.dragonAttemptMetrics.forEach { attempt ->
                appendLine(
                    listOf(
                        result.seed,
                        result.policy.name,
                        attempt.challengerType,
                        attempt.challengerId,
                        attempt.day,
                        attempt.level,
                        attempt.rank,
                        attempt.weaponId,
                        attempt.armorId,
                        attempt.attack,
                        attempt.defense,
                        attempt.maximumHp,
                        attempt.currentHp,
                        attempt.readinessTier.name,
                        attempt.combatPower,
                        attempt.dragonPower,
                        attempt.attemptNumber,
                        attempt.outcome.name,
                    ).joinToString(","),
                )
            }
        }
    }

    private fun trainingCsv(results: List<CampaignResult>) = buildString {
        appendLine("seed,policy,target_level,attempts,failures")
        results.forEach { result ->
            (result.trainingAttemptsByTargetLevel.keys + result.trainingFailuresByTargetLevel.keys)
                .sorted()
                .forEach { level ->
                    appendLine(
                        "${result.seed},${result.policy.name},$level," +
                            "${result.trainingAttemptsByTargetLevel[level] ?: 0}," +
                            "${result.trainingFailuresByTargetLevel[level] ?: 0}",
                    )
                }
        }
    }

    private fun summaryJson(
        run: CharacterizationRun,
        qualification: Lgd011bQualification,
    ): String {
        val results = run.results
        val ordinary = results.filter { it.policy == CampaignPolicy.ORDINARY }
        val ordinaryDays = ordinary.flatMap { it.days }
        val playerAttempts = attempts(ordinary, "PLAYER")
        val npcAttempts = attempts(ordinary, "NPC")
        val candidates = ordinary.map(::candidateCount)
        val npcVictoryCategories = ordinary.filter { it.npcVictory }
            .groupingBy(::npcVictoryCategory).eachCount().toSortedMap()
        val chronicleSizes = ordinaryDays.map { it.chronicleEntries.coerceAtMost(7) }
        val callbackTotal = ordinary.sumOf { it.callbacks }
        val chronicleTotal = ordinary.sumOf { it.chronicleEntries }
        val rivalryCallbacks = ordinary.sumOf { it.callbackTypeCounts[WorldEventType.CHALLENGE] ?: 0 }
        val dragonPreparationTypes = setOf(
            WorldEventType.DRAGON_INTENT,
            WorldEventType.DRAGON_SUPPORT,
            WorldEventType.DRAGON_ATTEMPT,
        )
        val dragonPreparationCallbacks = ordinary.sumOf { result ->
            dragonPreparationTypes.sumOf { result.callbackTypeCounts[it] ?: 0 }
        }
        val spotlight = ordinary.map(::narrativeConcentration)
        return buildString {
            appendLine("{")
            appendLine("  \"schema\": \"LGD-011B-recharacterization-v1\",")
            appendLine("  \"baseline\": {\"git_head\":null,\"save_schema\":${GameState.CURRENT_SAVE_VERSION}},")
            appendLine("  \"performance\": {")
            appendLine("    \"seasons\":${results.size},\"completed\":${results.count { it.completed }},\"unresolved\":${results.count { !it.completed }},")
            appendLine("    \"game_days\":${results.sumOf { it.days.size }},\"seconds\":${f(run.durationSeconds)},\"seasons_per_second\":${f(results.size / run.durationSeconds)}")
            appendLine("  },")
            appendLine("  \"qualification\": {")
            appendLine("    \"runner_deterministic_pairs\":${run.deterministicPairs},")
            appendLine("    \"condition_deterministic_pairs\":${qualification.deterministicCases.size},")
            appendLine("    \"save_reload_pairs\":${qualification.saveReloadCases.size},")
            appendLine("    \"deterministic_cases\":${stringListJson(qualification.deterministicCases)},")
            appendLine("    \"save_reload_cases\":${stringListJson(qualification.saveReloadCases)}")
            appendLine("  },")
            appendLine("  \"policies\": {")
            CampaignPolicy.entries.forEachIndexed { index, policy ->
                append("    \"").append(policy.name).append("\":")
                    .append(policyJson(results.filter { it.policy == policy }))
                appendLine(if (index == CampaignPolicy.entries.lastIndex) "" else ",")
            }
            appendLine("  },")
            appendLine("  \"ordinary\": {")
            appendLine("    \"duration\":${statsJson(stats(ordinary.map { it.resolutionDay.toDouble() }))},")
            appendLine("    \"player_victory_percent\":${f(pct(ordinary.count { it.playerVictory }, ordinary.size))},")
            appendLine("    \"npc_victory_percent\":${f(pct(ordinary.count { it.npcVictory }, ordinary.size))},")
            appendLine("    \"unresolved_percent\":${f(pct(ordinary.count { !it.completed }, ordinary.size))},")
            appendLine("    \"first_player_attempt_success_percent\":${f(firstAttemptWinPercent(ordinary))},")
            appendLine("    \"all_player_attempt_success_percent\":${f(pct(playerAttempts.count { it.outcome == DragonAttemptOutcome.VICTORY }, playerAttempts.size))},")
            appendLine("    \"mean_player_attempts\":${f(ordinary.map { it.playerDragonAttempts.size }.average())},")
            appendLine("    \"player_never_attempted_percent\":${f(pct(ordinary.count { it.playerDragonAttempts.isEmpty() }, ordinary.size))},")
            appendLine("    \"player_lost_then_npc_won_percent\":${f(pct(ordinary.count { it.npcVictory && it.playerDragonAttempts.any { outcome -> outcome != DragonAttemptOutcome.VICTORY } }, ordinary.size))},")
            appendLine("    \"player_lost_then_later_won_percent\":${f(pct(ordinary.count { it.playerVictory && it.playerDragonAttempts.dropLast(1).any { outcome -> outcome != DragonAttemptOutcome.VICTORY } }, ordinary.size))},")
            appendLine("    \"player_first_attempt_win_season_percent\":${f(pct(ordinary.count { it.playerDragonAttempts.firstOrNull() == DragonAttemptOutcome.VICTORY }, ordinary.size))},")
            appendLine("    \"player_multiple_attempt_win_percent\":${f(pct(ordinary.count { it.playerVictory && it.playerDragonAttempts.size > 1 }, ordinary.size))},")
            appendLine("    \"npc_first_attempt_success_percent\":${f(firstNpcAttemptWinPercent(ordinary))},")
            appendLine("    \"multiple_npc_challengers_percent\":${f(pct(ordinary.count { it.uniqueNpcDragonChallengers >= 2 }, ordinary.size))},")
            appendLine("    \"player_attempt_readiness\":${attemptSummaryJson(playerAttempts)},")
            appendLine("    \"npc_attempt_readiness\":${attemptSummaryJson(npcAttempts)},")
            appendLine("    \"npc_victory_categories\":${frequencyJson(npcVictoryCategories)},")
            appendLine("    \"final_player_levels\":${frequencyJson(ordinary.map { it.finalPlayerLevel })},")
            appendLine("    \"final_player_ranks\":${frequencyJson(ordinary.map { it.finalPlayerRank })},")
            appendLine("    \"player_rank_one_percent\":${f(pct(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size))},")
            appendLine("    \"player_top_three_percent\":${f(pct(ordinary.count { it.finalPlayerRank <= 3 }, ordinary.size))},")
            appendLine("    \"player_top_five_percent\":${f(pct(ordinary.count { it.finalPlayerRank <= 5 }, ordinary.size))},")
            appendLine("    \"final_rank_stats\":${statsJson(stats(ordinary.map { it.finalPlayerRank.toDouble() }))},")
            appendLine("    \"candidate_stats\":${statsJson(stats(candidates.map(Int::toDouble)))},")
            appendLine("    \"candidate_frequency\":${candidateFrequencyJson(candidates)},")
            appendLine("    \"npc_ready_before_player_percent\":${f(npcReadyBeforePlayerPercent(ordinary))},")
            appendLine("    \"npc_levels\":${npcLevelJson(ordinary)},")
            appendLine("    \"training\":${trainingJson(ordinary)},")
            appendLine("    \"combat_defeat_percent\":${f(pct(ordinary.sumOf { it.totalFightsLost }, ordinary.sumOf { it.totalFightsAttempted }))},")
            appendLine("    \"chronicle\": {")
            appendLine("      \"size_frequency\":${frequencyJson(chronicleSizes)},")
            appendLine("      \"stats\":${statsJson(stats(ordinaryDays.map { it.chronicleEntries.toDouble() }))},")
            appendLine("      \"ceiling_day_percent\":${f(pct(ordinaryDays.count { it.chronicleEntries >= 7 }, ordinaryDays.size))},")
            appendLine("      \"callbacks_per_season\":${f(ordinary.map { it.callbacks }.average())},")
            appendLine("      \"incidents_with_callback_per_season\":${f(ordinary.map { it.callbackPriorEventIds.size }.average())},")
            appendLine("      \"rivalry_callbacks_per_season\":${f(rivalryCallbacks.toDouble() / ordinary.size)},")
            appendLine("      \"dragon_preparation_callbacks_per_season\":${f(dragonPreparationCallbacks.toDouble() / ordinary.size)},")
            appendLine("      \"callback_share_percent\":${f(pct(callbackTotal, chronicleTotal))},")
            appendLine("      \"mean_npcs_present\":${f(spotlight.map { it.present }.average())},")
            appendLine("      \"mean_npcs_repeated\":${f(spotlight.map { it.repeated }.average())},")
            appendLine("      \"mean_npcs_absent\":${f(spotlight.map { it.absent }.average())},")
            appendLine("      \"mean_top_npc_share_percent\":${f(spotlight.map { it.topShare }.average())},")
            appendLine("      \"mean_top_three_share_percent\":${f(spotlight.map { it.top3Share }.average())}")
            appendLine("    },")
            appendLine("    \"level_timing\":${levelTimingJson(ordinary)},")
            appendLine("    \"dragon_stage_timing\":${stageTimingJson(ordinary)},")
            appendLine("    \"economy_equipment\":${economyJson(ordinary)}")
            appendLine("  }")
            appendLine("}")
        }
    }

    private fun policyJson(cohort: List<CampaignResult>): String {
        val days = cohort.flatMap { it.days }
        val playerAttempts = attempts(cohort, "PLAYER")
        val firstAttemptDays = playerAttempts.filter { it.attemptNumber == 1 }.map { it.day }
        return buildString {
            append("{")
            append("\"count\":${cohort.size},")
            append("\"completed\":${cohort.count { it.completed }},")
            append("\"unresolved\":${cohort.count { !it.completed }},")
            append("\"duration\":${statsJson(stats(cohort.map { it.resolutionDay.toDouble() }))},")
            append("\"player_victory_percent\":${f(pct(cohort.count { it.playerVictory }, cohort.size))},")
            append("\"npc_victory_percent\":${f(pct(cohort.count { it.npcVictory }, cohort.size))},")
            append("\"final_level_frequency\":${frequencyJson(cohort.map { it.finalPlayerLevel })},")
            append("\"final_rank_frequency\":${frequencyJson(cohort.map { it.finalPlayerRank })},")
            append("\"median_final_level\":${median(cohort.map { it.finalPlayerLevel })},")
            append("\"median_final_rank\":${median(cohort.map { it.finalPlayerRank })},")
            append("\"combat_defeat_percent\":${f(pct(cohort.sumOf { it.totalFightsLost }, cohort.sumOf { it.totalFightsAttempted }))},")
            append("\"fights_per_day\":${f(cohort.sumOf { it.totalFightsAttempted }.toDouble() / days.size.coerceAtLeast(1))},")
            append("\"mean_player_dragon_attempts\":${f(mean(cohort.map { it.playerDragonAttempts.size.toDouble() }))},")
            append("\"first_dragon_attempt_success_percent\":${f(firstAttemptWinPercent(cohort))},")
            append("\"at_least_one_dragon_attempt_percent\":${f(pct(cohort.count { it.playerDragonAttempts.isNotEmpty() }, cohort.size))},")
            append("\"median_first_ready_day\":${nullableMedian(cohort.mapNotNull { it.firstPlayerReadyDay })},")
            append("\"median_first_attempt_day\":${nullableMedian(firstAttemptDays)},")
            append("\"training_first_attempt_percent\":${f(trainingFirstAttemptPercent(cohort))},")
            append("\"median_final_wealth\":${median(cohort.map { it.finalCarriedGold + it.finalBankedGold })}")
            append("}")
        }
    }

    private fun attemptSummaryJson(values: List<DragonAttemptMetric>): String {
        val first = values.filter { it.attemptNumber == 1 }
        return buildString {
            append("{")
            append("\"attempts\":${values.size},")
            append("\"unique_challengers\":${values.map { it.challengerId }.distinct().size},")
            append("\"mean_level\":${f(mean(values.map { it.level.toDouble() }))},")
            append("\"median_level\":${median(values.map { it.level })},")
            append("\"mean_attack\":${f(mean(values.map { it.attack.toDouble() }))},")
            append("\"mean_defense\":${f(mean(values.map { it.defense.toDouble() }))},")
            append("\"mean_maximum_hp\":${f(mean(values.map { it.maximumHp.toDouble() }))},")
            append("\"mean_combat_power\":${f(mean(values.map { it.combatPower.toDouble() }))},")
            append("\"readiness_tiers\":${frequencyJson(values.map { it.readinessTier.name })},")
            append("\"weapon_distribution\":${frequencyJson(values.map { it.weaponId })},")
            append("\"armor_distribution\":${frequencyJson(values.map { it.armorId })},")
            append("\"first_attempt_success_percent\":${f(pct(first.count { it.outcome == DragonAttemptOutcome.VICTORY }, first.size))},")
            append("\"all_attempt_success_percent\":${f(pct(values.count { it.outcome == DragonAttemptOutcome.VICTORY }, values.size))},")
            append("\"median_first_attempt_day\":${nullableMedian(first.map { it.day })}")
            append("}")
        }
    }

    private fun npcLevelJson(ordinary: List<CampaignResult>): String {
        val all = ordinary.flatMap { it.npcFinalLevels }
        val top = ordinary.map { it.npcFinalLevels.maxOrNull() ?: 0 }
        val sortedTopThree = ordinary.map { result -> result.npcFinalLevels.sortedDescending().take(3) }
        val atMinusOne = ordinary.map { result ->
            result.npcFinalLevels.count { it == result.finalPlayerLevel - 1 }
        }
        val equal = ordinary.map { result -> result.npcFinalLevels.count { it == result.finalPlayerLevel } }
        val above = ordinary.map { result -> result.npcFinalLevels.count { it > result.finalPlayerLevel } }
        return buildString {
            append("{")
            append("\"all_npc_median\":${median(all)},")
            append("\"maximum_observed\":${all.maxOrNull() ?: 0},")
            append("\"top_npc_frequency\":${frequencyJson(top)},")
            append("\"top_npc_stats\":${statsJson(stats(top.map(Int::toDouble)))},")
            append("\"top_three_median_levels\":[")
            append((0..2).joinToString(",") { index ->
                median(sortedTopThree.mapNotNull { it.getOrNull(index) }).toString()
            })
            append("],")
            append("\"mean_at_player_minus_one\":${f(mean(atMinusOne.map(Int::toDouble)))},")
            append("\"mean_equal_to_player\":${f(mean(equal.map(Int::toDouble)))},")
            append("\"mean_above_player\":${f(mean(above.map(Int::toDouble)))}")
            append("}")
        }
    }

    private fun trainingJson(ordinary: List<CampaignResult>): String {
        val attempts = ordinary.sumOf { it.totalTrainingAttempts }
        val failures = ordinary.sumOf { it.totalTrainingFailures }
        val successes = ordinary.sumOf { it.totalTrainingSuccesses }
        val promotionAttempts = ordinary.flatMap { it.attemptsPerSuccessfulPromotion }
        return buildString {
            append("{")
            append("\"attempts\":$attempts,\"successes\":$successes,\"failures\":$failures,")
            append("\"first_attempt_success_percent\":${f(trainingFirstAttemptPercent(ordinary))},")
            append("\"second_attempt_promotion_percent\":${f(pct(promotionAttempts.count { it == 2 }, promotionAttempts.size))},")
            append("\"three_plus_attempt_promotion_percent\":${f(pct(promotionAttempts.count { it >= 3 }, promotionAttempts.size))},")
            append("\"maximum_attempts\":${promotionAttempts.maxOrNull() ?: 0},")
            append("\"maximum_failure_streak\":${ordinary.maxOfOrNull { it.maximumTrainingFailureStreak } ?: 0},")
            append("\"average_failure_delay_days\":${f(failures.toDouble() / successes.coerceAtLeast(1))},")
            append("\"progression_lock_cases\":0,")
            append("\"by_target_level\":{")
            val targetLevels = ordinary.flatMap { it.trainingAttemptsByTargetLevel.keys }.distinct().sorted()
            targetLevels.forEachIndexed { index, level ->
                val levelAttempts = ordinary.sumOf { it.trainingAttemptsByTargetLevel[level] ?: 0 }
                val levelFailures = ordinary.sumOf { it.trainingFailuresByTargetLevel[level] ?: 0 }
                val episodes = ordinary.mapNotNull { result ->
                    val count = result.trainingAttemptsByTargetLevel[level] ?: 0
                    if (count == 0) null else result.trainingFailuresByTargetLevel[level] ?: 0
                }
                append("\"$level\":{")
                append("\"attempts\":$levelAttempts,\"successes\":${levelAttempts - levelFailures},")
                append("\"failures\":$levelFailures,\"attempt_success_percent\":${f(pct(levelAttempts - levelFailures, levelAttempts))},")
                append("\"first_attempt_success_percent\":${f(pct(episodes.count { it == 0 }, episodes.size))}}")
                if (index != targetLevels.lastIndex) append(",")
            }
            append("}}")
        }
    }

    private fun levelTimingJson(ordinary: List<CampaignResult>) = buildString {
        append("{")
        (2..9).forEachIndexed { index, level ->
            val reached = ordinary.mapNotNull { it.levelReachedDays[level] }
            append("\"$level\":{")
            append("\"reached\":${reached.size},\"percent\":${f(pct(reached.size, ordinary.size))},")
            append("\"median_day\":${nullableMedian(reached)},")
            append("\"p10_day\":${nullablePercentile(reached, 0.10)},")
            append("\"p90_day\":${nullablePercentile(reached, 0.90)}}")
            if (index != 7) append(",")
        }
        append("}")
    }

    private fun stageTimingJson(ordinary: List<CampaignResult>) = buildString {
        append("{")
        DragonStage.entries.forEachIndexed { index, stage ->
            val values = ordinary.mapNotNull { it.stageEnteredDays[stage] }
            append("\"${stage.name}\":${statsJson(stats(values.map(Int::toDouble)))}")
            if (index != DragonStage.entries.lastIndex) append(",")
        }
        append("}")
    }

    private fun economyJson(ordinary: List<CampaignResult>): String {
        val wealth = ordinary.map { it.finalCarriedGold + it.finalBankedGold }
        return buildString {
            append("{")
            append("\"final_wealth\":${statsJson(stats(wealth.map(Int::toDouble)))},")
            append("\"weapons\":${frequencyJson(ordinary.map { it.finalWeaponId })},")
            append("\"armor\":${frequencyJson(ordinary.map { it.finalArmorId })},")
            append("\"mean_healing_spent\":${f(mean(ordinary.map { it.totalHealingSpent.toDouble() }))},")
            append("\"mean_equipment_spent\":${f(mean(ordinary.map { it.totalEquipmentSpent.toDouble() }))},")
            append("\"mean_defeat_loss\":${f(mean(ordinary.map { it.totalDefeatGoldLost.toDouble() }))}")
            append("}")
        }
    }

    private data class Summary(
        val count: Int,
        val minimum: Double,
        val maximum: Double,
        val mean: Double,
        val median: Double,
        val p10: Double,
        val p25: Double,
        val p75: Double,
        val p90: Double,
        val p95: Double,
    )

    private data class NarrativeConcentration(
        val present: Int,
        val repeated: Int,
        val absent: Int,
        val topShare: Double,
        val top3Share: Double,
    )

    private fun narrativeConcentration(result: CampaignResult): NarrativeConcentration {
        val counts = result.chronicleNpcAppearances.values.sortedDescending()
        val total = counts.sum()
        val present = counts.count { it > 0 }
        return NarrativeConcentration(
            present = present,
            repeated = counts.count { it >= 2 },
            absent = POPULATION_SIZE - present,
            topShare = if (total == 0) 0.0 else (counts.firstOrNull() ?: 0) * 100.0 / total,
            top3Share = if (total == 0) 0.0 else counts.take(3).sum() * 100.0 / total,
        )
    }

    private fun stats(values: List<Double>): Summary {
        if (values.isEmpty()) return Summary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val sorted = values.sorted()
        return Summary(
            count = sorted.size,
            minimum = sorted.first(),
            maximum = sorted.last(),
            mean = sorted.average(),
            median = percentile(sorted, 0.50),
            p10 = percentile(sorted, 0.10),
            p25 = percentile(sorted, 0.25),
            p75 = percentile(sorted, 0.75),
            p90 = percentile(sorted, 0.90),
            p95 = percentile(sorted, 0.95),
        )
    }

    private fun statsJson(value: Summary): String =
        "{\"count\":${value.count},\"min\":${f(value.minimum)},\"max\":${f(value.maximum)}," +
            "\"mean\":${f(value.mean)},\"median\":${f(value.median)}," +
            "\"p10\":${f(value.p10)},\"p25\":${f(value.p25)}," +
            "\"p75\":${f(value.p75)},\"p90\":${f(value.p90)},\"p95\":${f(value.p95)}}"

    private fun percentile(sorted: List<Double>, probability: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val index = (ceil(probability * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index]
    }

    private fun percentileInt(values: List<Int>, probability: Double): Int =
        percentile(values.sorted().map(Int::toDouble), probability).toInt()

    private fun median(values: List<Int>): Int = percentileInt(values, 0.50)
    private fun nullableMedian(values: List<Int>): String =
        if (values.isEmpty()) "null" else median(values).toString()

    private fun nullablePercentile(values: List<Int>, probability: Double): String =
        if (values.isEmpty()) "null" else percentileInt(values, probability).toString()

    private fun mean(values: List<Double>): Double = if (values.isEmpty()) 0.0 else values.average()

    private fun pct(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else numerator * 100.0 / denominator

    private fun f(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun candidateCount(result: CampaignResult): Int =
        maxOf(result.challengeWindowReadyCandidates, result.finalReadyCandidates).coerceAtLeast(0)

    private fun attempts(results: List<CampaignResult>, challengerType: String): List<DragonAttemptMetric> =
        results.flatMap { result ->
            result.dragonAttemptMetrics.filter { it.challengerType == challengerType }
        }

    private fun firstAttemptWinPercent(results: List<CampaignResult>): Double {
        val first = attempts(results, "PLAYER").filter { it.attemptNumber == 1 }
        return pct(first.count { it.outcome == DragonAttemptOutcome.VICTORY }, first.size)
    }

    private fun firstNpcAttemptWinPercent(results: List<CampaignResult>): Double {
        val first = attempts(results, "NPC").filter { it.attemptNumber == 1 }
        return pct(first.count { it.outcome == DragonAttemptOutcome.VICTORY }, first.size)
    }

    private fun trainingFirstAttemptPercent(results: List<CampaignResult>): Double {
        val promotions = results.flatMap { it.attemptsPerSuccessfulPromotion }
        return pct(promotions.count { it == 1 }, promotions.size)
    }

    private fun npcReadyBeforePlayerPercent(results: List<CampaignResult>): Double = pct(
        results.count { result ->
            result.firstNpcReadyDay != null &&
                (result.firstPlayerReadyDay == null || result.firstNpcReadyDay < result.firstPlayerReadyDay)
        },
        results.size,
    )

    private fun npcVictoryCategory(result: CampaignResult): String {
        check(result.npcVictory)
        val victor = result.finalState.community.adventurers.firstOrNull { it.id == result.victorId }
            ?: return "E_OTHER"
        val victorRank = CommunityRules.rankOf(result.finalState, victor.id)
        val victorAttemptPower = result.dragonAttemptMetrics
            .filter { it.challengerId == victor.id }
            .maxOfOrNull { it.combatPower }
        val playerAttemptPower = result.dragonAttemptMetrics
            .filter { it.challengerType == "PLAYER" }
            .maxOfOrNull { it.combatPower }
        val npcStronger = victorRank < result.finalPlayerRank ||
            victor.level > result.finalPlayerLevel ||
            (victorAttemptPower != null && playerAttemptPower != null &&
                victorAttemptPower > playerAttemptPower)
        return when {
            npcStronger -> "A_NPC_STRONGER_THAN_PLAYER"
            result.firstNpcReadyDay != null &&
                (result.firstPlayerReadyDay == null ||
                    result.firstNpcReadyDay < result.firstPlayerReadyDay) -> "B_NPC_READY_EARLIER"
            result.playerDragonAttempts.any { it != DragonAttemptOutcome.VICTORY } ->
                "C_PLAYER_LOST_NPC_WON_LATER"
            result.playerDragonAttempts.isEmpty() -> "D_PLAYER_NEVER_ATTEMPTED"
            else -> "E_OTHER"
        }
    }

    private fun candidateFrequencyJson(values: List<Int>): String {
        val buckets = linkedMapOf(
            "0" to values.count { it == 0 },
            "1" to values.count { it == 1 },
            "2" to values.count { it == 2 },
            "3" to values.count { it == 3 },
            "4_plus" to values.count { it >= 4 },
        )
        return frequencyJson(buckets)
    }

    private fun <T : Comparable<T>> frequencyJson(values: List<T>): String =
        frequencyJson(values.groupingBy { it }.eachCount().toSortedMap())

    private fun <T> frequencyJson(values: Map<T, Int>): String =
        values.entries.joinToString(prefix = "{", postfix = "}") { (key, count) ->
            "\"$key\":$count"
        }

    private fun stringListJson(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

    private data class Outlier(
        val label: String,
        val result: CampaignResult?,
        val detail: (CampaignResult) -> String,
    )

    private fun outlierRows(results: List<CampaignResult>): List<Outlier> {
        val playerWins = results.filter { it.playerVictory }
        val npcWins = results.filter { it.npcVictory }
        return listOf(
            Outlier("shortest_season", results.minByOrNull { it.resolutionDay }) {
                "${it.resolutionDay} days"
            },
            Outlier("longest_season", results.maxByOrNull { it.resolutionDay }) {
                "${it.resolutionDay} days"
            },
            Outlier("earliest_player_win", playerWins.minByOrNull { it.resolutionDay }) {
                "day ${it.resolutionDay}"
            },
            Outlier("latest_player_win", playerWins.maxByOrNull { it.resolutionDay }) {
                "day ${it.resolutionDay}"
            },
            Outlier("strongest_npc_win", npcWins.maxWithOrNull(
                compareBy<CampaignResult> { it.npcFinalLevels.maxOrNull() ?: 0 }
                    .thenBy { candidateCount(it) },
            )) { "top NPC level ${it.npcFinalLevels.maxOrNull() ?: 0}" },
            Outlier("weakest_successful_player", playerWins.minWithOrNull(
                compareBy<CampaignResult> { it.finalPlayerLevel }
                    .thenBy { it.finalPlayerExperience },
            )) { "level ${it.finalPlayerLevel}, XP ${it.finalPlayerExperience}" },
            Outlier("greatest_training_failure_streak", results.maxByOrNull {
                it.maximumTrainingFailureStreak
            }) { "${it.maximumTrainingFailureStreak} failures" },
            Outlier("greatest_combat_defeat_count", results.maxByOrNull {
                it.totalFightsLost
            }) { "${it.totalFightsLost} defeats" },
            Outlier("most_dragon_attempts", results.maxByOrNull {
                it.playerDragonAttempts.size + it.npcDragonAttempts.size
            }) { "${it.playerDragonAttempts.size + it.npcDragonAttempts.size} attempts" },
            Outlier("most_credible_challengers", results.maxByOrNull(::candidateCount)) {
                "${candidateCount(it)} candidates"
            },
            Outlier("most_callback_heavy", results.maxByOrNull { it.callbacks }) {
                "${it.callbacks} callbacks"
            },
            Outlier("quietest_chronicle", results.minByOrNull {
                it.chronicleEntries.toDouble() / it.days.size.coerceAtLeast(1)
            }) {
                "${f(it.chronicleEntries.toDouble() / it.days.size.coerceAtLeast(1))} entries/day"
            },
            Outlier("most_dominant_npc", results.maxByOrNull(::topNpcShare)) {
                "${f(topNpcShare(it))}% top-NPC share"
            },
            Outlier("first_unresolved", results.firstOrNull { !it.completed }) {
                "unresolved at ${it.resolutionDay} days"
            },
        )
    }

    private fun topNpcShare(result: CampaignResult): Double {
        val counts = result.chronicleNpcAppearances.values
        val total = counts.sum()
        return if (total == 0) 0.0 else (counts.maxOrNull() ?: 0) * 100.0 / total
    }

    private fun outliersJson(results: List<CampaignResult>) = buildString {
        appendLine("{")
        val rows = outlierRows(results)
        rows.forEachIndexed { index, row ->
            append("  \"").append(row.label).append("\":")
            val result = row.result
            if (result == null) {
                append("null")
            } else {
                append("{\"policy\":\"").append(result.policy.name)
                    .append("\",\"seed\":").append(result.seed)
                    .append(",\"detail\":\"").append(row.detail(result)).append("\"}")
            }
            appendLine(if (index == rows.lastIndex) "" else ",")
        }
        appendLine("}")
    }

    private fun write(path: Path, content: String) {
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }
}
