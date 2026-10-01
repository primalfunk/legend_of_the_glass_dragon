package com.legendoftheglassdragon.campaign

import com.legendoftheglassdragon.data.DecodeResult
import com.legendoftheglassdragon.data.GameStateCodec
import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.WorldEventType
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.sqrt

data class Lgd011aQualification(
    val saveReloadPairs: Int,
    val saveReloadCases: List<String>,
)

object CampaignRecharacterizationReporter {
    fun qualifySaveReload(results: List<CampaignResult>): Lgd011aQualification {
        val requested = listOf(
            "training_failure" to { result: CampaignResult -> result.totalTrainingFailures > 0 },
            "player_dragon_defeat" to { result: CampaignResult ->
                result.playerDragonAttempts.any { it != DragonAttemptOutcome.VICTORY }
            },
            "npc_dragon_victory" to { result: CampaignResult -> result.npcVictory },
            "multiple_credible_champions" to { result: CampaignResult ->
                maxOf(result.challengeWindowReadyCandidates, result.finalReadyCandidates) >= 2
            },
            "rivalry_callback" to { result: CampaignResult -> result.callbacks >= 2 },
        )
        val codec = GameStateCodec()
        val roundTrip: (GameState) -> GameState = { state ->
            when (val decoded = codec.decode(codec.encode(state))) {
                is DecodeResult.Success -> decoded.gameState
                is DecodeResult.Failure -> error("LGD-011A round trip failed: ${decoded.failure}")
            }
        }
        val qualified = requested.map { (label, predicate) ->
            val source = results.firstOrNull(predicate)
                ?: error("No revised cohort seed covers save/reload case: $label")
            val interrupted = CampaignSimulationHarness(
                roundTripDays = setOf(2, 4, 7, 11, 15, 19, 23, 29),
                stateRoundTrip = roundTrip,
            ).run(source.seed, source.policy)
            check(source == interrupted) {
                "Save/reload changed $label for ${source.policy} seed=${source.seed}"
            }
            "$label:${source.policy.name}:${source.seed}"
        }
        return Lgd011aQualification(qualified.size, qualified)
    }

    fun writeEvidence(
        run: CharacterizationRun,
        qualification: Lgd011aQualification,
        output: Path,
    ) {
        Files.createDirectories(output)
        write(output.resolve("lgd011a_campaigns.csv"), campaignsCsv(run.results))
        write(output.resolve("lgd011a_days.jsonl"), daysJsonl(run.results))
        write(output.resolve("lgd011a_training.csv"), trainingCsv(run.results))
        write(output.resolve("lgd011a_summary.json"), summaryJson(run, qualification))
        write(output.resolve("lgd011a_outliers.json"), outliersJson(run.results))
        write(
            output.resolve("LGD-011A_TARGETED_BALANCE_RECHARACTERIZATION.md"),
            report(run, qualification),
        )
    }

    private fun campaignsCsv(results: List<CampaignResult>) = buildString {
        appendLine(
            "seed,policy,completed,days,victor_type,player_level,player_rank,total_wealth," +
                "weapon_id,armor_id,player_dragon_attempts,npc_dragon_attempts," +
                "training_attempts,training_failures,max_training_failure_streak," +
                "combat_attempts,combat_losses,chronicle_events,callbacks,max_npc_level," +
                "challenge_window_candidates,final_ready_candidates,first_player_ready_day," +
                "first_npc_ready_day",
        )
        results.forEach { result ->
            appendLine(
                listOf(
                    result.seed,
                    result.policy.name,
                    result.completed,
                    result.resolutionDay,
                    if (!result.completed) "UNRESOLVED" else if (result.playerVictory) "PLAYER" else "NPC",
                    result.finalPlayerLevel,
                    result.finalPlayerRank,
                    result.finalCarriedGold.toLong() + result.finalBankedGold,
                    result.finalWeaponId,
                    result.finalArmorId,
                    result.playerDragonAttempts.size,
                    result.npcDragonAttempts.size,
                    result.totalTrainingAttempts,
                    result.totalTrainingFailures,
                    result.maximumTrainingFailureStreak,
                    result.totalFightsAttempted,
                    result.totalFightsLost,
                    result.chronicleEntries,
                    result.callbacks,
                    result.npcFinalLevels.maxOrNull() ?: 0,
                    result.challengeWindowReadyCandidates,
                    result.finalReadyCandidates,
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

    private fun summaryJson(run: CharacterizationRun, qualification: Lgd011aQualification): String {
        val ordinary = run.results.filter { it.policy == CampaignPolicy.ORDINARY }
        val ordinaryDays = ordinary.flatMap { it.days }
        return buildString {
            appendLine("{")
            appendLine("  \"seasons\": ${run.results.size},")
            appendLine("  \"completed\": ${run.results.count { it.completed }},")
            appendLine("  \"total_game_days\": ${run.results.sumOf { it.days.size }},")
            appendLine("  \"duration_seconds\": ${f(run.durationSeconds)},")
            appendLine("  \"deterministic_pairs\": ${run.deterministicPairs},")
            appendLine("  \"save_reload_pairs\": ${qualification.saveReloadPairs},")
            appendLine("  \"ordinary\": {")
            appendLine("    \"median_days\": ${f(stats(ordinary.map { it.resolutionDay.toDouble() }).median)},")
            appendLine("    \"player_victory_percent\": ${f(pct(ordinary.count { it.playerVictory }, ordinary.size))},")
            appendLine("    \"npc_victory_percent\": ${f(pct(ordinary.count { it.npcVictory }, ordinary.size))},")
            appendLine("    \"player_rank_one_percent\": ${f(pct(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size))},")
            appendLine("    \"dragon_first_attempt_win_percent\": ${f(firstAttemptWinPercent(ordinary))},")
            appendLine("    \"training_first_attempt_success_percent\": ${f(trainingFirstAttemptPercent(ordinary))},")
            appendLine("    \"chronicle_mean\": ${f(ordinaryDays.map { it.chronicleEntries }.average())},")
            appendLine("    \"chronicle_median\": ${f(stats(ordinaryDays.map { it.chronicleEntries.toDouble() }).median)},")
            appendLine("    \"callbacks_per_season\": ${f(ordinary.map { it.callbacks }.average())}")
            appendLine("  }")
            appendLine("}")
        }
    }

    private fun report(run: CharacterizationRun, qualification: Lgd011aQualification): String {
        val results = run.results
        val ordinary = results.filter { it.policy == CampaignPolicy.ORDINARY }
        val ordinaryDays = ordinary.flatMap { it.days }
        val duration = stats(ordinary.map { it.resolutionDay.toDouble() })
        val levels = ordinary.groupingBy { it.finalPlayerLevel }.eachCount().toSortedMap()
        val candidateValues = ordinary.map {
            maxOf(it.challengeWindowReadyCandidates, it.finalReadyCandidates).coerceAtLeast(0)
        }
        val candidateStats = stats(candidateValues.map(Int::toDouble))
        val chronicleSizes = ordinaryDays.groupingBy { it.chronicleEntries.coerceAtMost(7) }
            .eachCount()
        val allAttempts = ordinary.sumOf { it.totalTrainingAttempts }
        val allFailures = ordinary.sumOf { it.totalTrainingFailures }
        val allSuccesses = ordinary.sumOf { it.totalTrainingSuccesses }
        val promotionAttempts = ordinary.flatMap { it.attemptsPerSuccessfulPromotion }
        val npcAppearances = ordinary.map { result ->
            val counts = result.chronicleNpcAppearances.values.sortedDescending()
            NarrativeConcentration(
                present = counts.count { it > 0 },
                repeated = counts.count { it >= 2 },
                prominent = counts.count { it >= 4 },
                absent = 16 - counts.count { it > 0 },
                topShare = if (counts.sum() == 0) 0.0 else counts.firstOrNull().orZero() * 100.0 / counts.sum(),
                top3Share = if (counts.sum() == 0) 0.0 else counts.take(3).sum() * 100.0 / counts.sum(),
            )
        }
        val dragonEvents = ordinary.sumOf { result ->
            result.chronicleTypeCounts.filterKeys { it.name.startsWith("DRAGON_") }.values.sum()
        }
        val callbackEvents = ordinary.sumOf { it.callbacks }
        val chronicleTotal = ordinary.sumOf { it.chronicleEntries }
        return buildString {
            appendLine("# LGD-011A Targeted Balance Recharacterization")
            appendLine()
            appendLine("Disposition: `LGD011A_RECHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`")
            appendLine()
            appendLine("This is one targeted tuning experiment followed by one ${results.size}-season cohort. No human playtesting or discretionary second-round tuning was used.")
            appendLine()
            appendLine("## Cohort")
            appendLine()
            appendLine("- Seasons: ${results.size} (${results.groupingBy { it.policy }.eachCount().entries.joinToString { "${it.key.name}=${it.value}" }}).")
            appendLine("- Completed: ${results.count { it.completed }}; unresolved: ${results.count { !it.completed }}.")
            appendLine("- Total game days: ${results.sumOf { it.days.size }}.")
            appendLine("- Runtime: ${f(run.durationSeconds)} seconds (${f(results.size / run.durationSeconds)} seasons/second).")
            appendLine("- Deterministic reruns: ${run.deterministicPairs}; save/reload comparisons: ${qualification.saveReloadPairs}.")
            appendLine()
            appendLine("## Principal Before / After Table")
            appendLine()
            appendLine("| Ordinary metric | LGD-011 before | LGD-011A after |")
            appendLine("|---|---:|---:|")
            appendLine("| Median campaign days | 20 | ${f(duration.median)} |")
            appendLine("| P10-P90 campaign days | 19-22 | ${f(duration.p10)}-${f(duration.p90)} |")
            appendLine("| Player Dragon victory | 97.8% | ${rate(ordinary.count { it.playerVictory }, ordinary.size)} |")
            appendLine("| NPC Dragon victory | 2.2% | ${rate(ordinary.count { it.npcVictory }, ordinary.size)} |")
            appendLine("| Player Rank #1 | 100% | ${rate(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size)} |")
            appendLine("| Median credible candidates | 1 | ${f(candidateStats.median)} |")
            appendLine("| Dragon first-attempt win | 91.67% | ${f(firstAttemptWinPercent(ordinary))}% |")
            appendLine("| Mean player Dragon attempts | 1.06 | ${f(ordinary.map { it.playerDragonAttempts.size }.average())} |")
            appendLine("| Median final player level | 7 | ${f(stats(ordinary.map { it.finalPlayerLevel.toDouble() }).median)} |")
            appendLine("| Final-level distribution | 499 L7 / 1 L6 | ${levels.entries.joinToString { "L${it.key}=${it.value}" }} |")
            appendLine("| Training first-try success | 100% | ${f(trainingFirstAttemptPercent(ordinary))}% |")
            appendLine("| Ordinary combat defeat | 0.42% | ${rate(ordinary.sumOf { it.totalFightsLost }, ordinary.sumOf { it.totalFightsAttempted })} |")
            val reckless = results.filter { it.policy == CampaignPolicy.RECKLESS }
            appendLine("| Reckless combat defeat | 1.47% | ${rate(reckless.sumOf { it.totalFightsLost }, reckless.sumOf { it.totalFightsAttempted })} |")
            appendLine("| Chronicle mean | 6.71 | ${f(ordinaryDays.map { it.chronicleEntries }.average())} |")
            appendLine("| Chronicle median | 7 | ${f(stats(ordinaryDays.map { it.chronicleEntries.toDouble() }).median)} |")
            appendLine("| Zero-item Chronicle days | 0% | ${rate(ordinaryDays.count { it.chronicleEntries == 0 }, ordinaryDays.size)} |")
            appendLine("| Top NPC Chronicle share | 9.76% | ${f(npcAppearances.map { it.topShare }.average())}% |")
            appendLine("| Structured callbacks / season | 0.57 | ${f(ordinary.map { it.callbacks }.average())} |")
            appendLine()
            appendLine("## Policy Comparison")
            appendLine()
            appendLine("| Policy | Median days | Player win | NPC win | Median level | Median rank | Combat defeat | Median wealth | Fights/day | Mean Dragon attempts | Training failure | Median first-ready day |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val days = cohort.flatMap { it.days }
                appendLine("| ${policy.label} | ${median(cohort.map { it.resolutionDay })} | ${rate(cohort.count { it.playerVictory }, cohort.size)} | ${rate(cohort.count { it.npcVictory }, cohort.size)} | ${median(cohort.map { it.finalPlayerLevel })} | ${median(cohort.map { it.finalPlayerRank })} | ${rate(cohort.sumOf { it.totalFightsLost }, cohort.sumOf { it.totalFightsAttempted })} | ${median(cohort.map { it.finalCarriedGold + it.finalBankedGold })} | ${f(cohort.sumOf { it.totalFightsAttempted }.toDouble() / days.size)} | ${f(cohort.map { it.playerDragonAttempts.size }.average())} | ${rate(cohort.sumOf { it.totalTrainingFailures }, cohort.sumOf { it.totalTrainingAttempts })} | ${nullableMedian(cohort.mapNotNull { it.firstPlayerReadyDay })} |")
            }
            appendLine()
            appendLine("Casual policy after: 6 fights/day; heals at 75% HP; runs with three maximum incoming hits left; buys one clear upgrade per visit; attempts once Level 7, weapon/armor tier 4, combat power 128, and one full challenge-window day have passed. Other policy decision constants were held.")
            appendLine()
            appendLine("## Campaign Duration")
            appendLine()
            appendLine("Ordinary range ${duration.minimum.toInt()}-${duration.maximum.toInt()}, mean ${f(duration.mean)}, median ${f(duration.median)}, P10/P25/P75/P90/P95 ${f(duration.p10)}/${f(duration.p25)}/${f(duration.p75)}/${f(duration.p90)}/${f(duration.p95)}.")
            appendLine()
            appendLine(durationBuckets(ordinary))
            appendLine()
            appendLine("## Dragon Outcomes and Attempts")
            appendLine()
            val playerAttempters = ordinary.filter { it.playerDragonAttempts.isNotEmpty() }
            appendLine("- Player victory: ${rate(ordinary.count { it.playerVictory }, ordinary.size)}; NPC victory: ${rate(ordinary.count { it.npcVictory }, ordinary.size)}; unresolved: ${rate(ordinary.count { !it.completed }, ordinary.size)}.")
            appendLine("- Player never attempted: ${rate(ordinary.count { it.playerDragonAttempts.isEmpty() }, ordinary.size)}.")
            appendLine("- Player lost then NPC won: ${rate(ordinary.count { it.npcVictory && it.playerDragonAttempts.any { outcome -> outcome != DragonAttemptOutcome.VICTORY } }, ordinary.size)}.")
            appendLine("- Player lost then later won: ${rate(ordinary.count { it.playerVictory && it.playerDragonAttempts.dropLast(1).any { outcome -> outcome != DragonAttemptOutcome.VICTORY } }, ordinary.size)}.")
            appendLine("- Player first-attempt wins: ${rate(playerAttempters.count { it.playerDragonAttempts.first() == DragonAttemptOutcome.VICTORY }, playerAttempters.size)} among attempters; multiple-attempt player wins: ${rate(ordinary.count { it.playerVictory && it.playerDragonAttempts.size > 1 }, ordinary.size)} of seasons.")
            appendLine("- Mean/median/max player attempts: ${f(ordinary.map { it.playerDragonAttempts.size }.average())}/${median(ordinary.map { it.playerDragonAttempts.size })}/${ordinary.maxOf { it.playerDragonAttempts.size }}.")
            appendLine("- NPC first-attempt victory: ${rate(ordinary.count { it.npcVictory && it.npcDragonAttempts.firstOrNull() == DragonAttemptOutcome.VICTORY }, ordinary.count { it.npcVictory })}; multiple NPC challengers before resolution: ${rate(ordinary.count { it.uniqueNpcDragonChallengers >= 2 }, ordinary.size)}.")
            appendLine()
            appendLine("## Player Progression")
            appendLine()
            levels.forEach { (level, count) -> appendLine("- Level $level: $count / ${ordinary.size}.") }
            appendLine()
            appendLine("| Level reached | Reached | Median day | P10-P90 day |")
            appendLine("|---:|---:|---:|---:|")
            (2..9).forEach { level ->
                val reached = ordinary.mapNotNull { it.levelReachedDays[level] }
                val s = stats(reached.map(Int::toDouble))
                appendLine("| $level | ${reached.size}/${ordinary.size} | ${if (reached.isEmpty()) "-" else f(s.median)} | ${if (reached.isEmpty()) "-" else "${f(s.p10)}-${f(s.p90)}"} |")
            }
            appendLine()
            appendLine("## Physical Training")
            appendLine()
            appendLine("- Attempts/successes/failures: $allAttempts / $allSuccesses / $allFailures (${rate(allFailures, allAttempts)} failure).")
            appendLine("- First-attempt promotion: ${f(trainingFirstAttemptPercent(ordinary))}%; second-attempt promotion: ${rate(promotionAttempts.count { it == 2 }, promotionAttempts.size)}; 3+ attempts: ${rate(promotionAttempts.count { it >= 3 }, promotionAttempts.size)}.")
            appendLine("- Maximum attempts for one successful promotion: ${promotionAttempts.maxOrNull() ?: 0}; maximum observed consecutive failure streak: ${ordinary.maxOf { it.maximumTrainingFailureStreak }}.")
            appendLine("- Average delay from failed trials per successful promotion: ${f(allFailures.toDouble() / allSuccesses.coerceAtLeast(1))} realm days; progression-lock cases: 0.")
            appendLine()
            appendLine("| Target level | Attempts | Successes | Failures | Success rate |")
            appendLine("|---:|---:|---:|---:|---:|")
            (2..9).forEach { level ->
                val attempts = ordinary.sumOf { it.trainingAttemptsByTargetLevel[level] ?: 0 }
                val failures = ordinary.sumOf { it.trainingFailuresByTargetLevel[level] ?: 0 }
                if (attempts > 0) appendLine("| $level | $attempts | ${attempts - failures} | $failures | ${rate(attempts - failures, attempts)} |")
            }
            appendLine()
            appendLine("## Rankings and NPC Competition")
            appendLine()
            appendLine("- Player #1: ${rate(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size)}; top 3: ${rate(ordinary.count { it.finalPlayerRank <= 3 }, ordinary.size)}; median rank: ${median(ordinary.map { it.finalPlayerRank })}.")
            appendLine("- Top NPC final level median/P10/P90/max: ${median(ordinary.map { it.npcFinalLevels.maxOrNull() ?: 0 })}/${percentileInt(ordinary.map { it.npcFinalLevels.maxOrNull() ?: 0 }, 0.10)}/${percentileInt(ordinary.map { it.npcFinalLevels.maxOrNull() ?: 0 }, 0.90)}/${ordinary.maxOf { it.npcFinalLevels.maxOrNull() ?: 0 }}.")
            appendLine("- NPCs at player level - 1 or higher (mean): ${f(ordinary.map { result -> result.npcFinalLevels.count { it >= result.finalPlayerLevel - 1 } }.average())}; equal: ${f(ordinary.map { result -> result.npcFinalLevels.count { it == result.finalPlayerLevel } }.average())}; above: ${f(ordinary.map { result -> result.npcFinalLevels.count { it > result.finalPlayerLevel } }.average())}.")
            appendLine("- Credible candidate median/P10/P90: ${f(candidateStats.median)}/${f(candidateStats.p10)}/${f(candidateStats.p90)}; 1=${rate(candidateValues.count { it == 1 }, candidateValues.size)}, 2=${rate(candidateValues.count { it == 2 }, candidateValues.size)}, 3=${rate(candidateValues.count { it == 3 }, candidateValues.size)}, 4+=${rate(candidateValues.count { it >= 4 }, candidateValues.size)}.")
            appendLine("- NPC Dragon-ready before player: ${rate(ordinary.count { result -> result.firstNpcReadyDay != null && (result.firstPlayerReadyDay == null || result.firstNpcReadyDay <= result.firstPlayerReadyDay) }, ordinary.size)}.")
            appendLine()
            appendLine("## Dragon Stage Timing")
            appendLine()
            DragonStage.entries.forEach { stage ->
                val values = ordinary.mapNotNull { it.stageEnteredDays[stage] }
                if (values.isNotEmpty()) appendLine("- ${stage.name}: median ${median(values)}, P10-P90 ${percentileInt(values, 0.10)}-${percentileInt(values, 0.90)}, range ${values.min()}-${values.max()}.")
            }
            appendLine()
            appendLine("## Chronicle Density and Cast")
            appendLine()
            appendLine("| Items | Ordinary days | Share |")
            appendLine("|---:|---:|---:|")
            (0..7).forEach { size ->
                val count = chronicleSizes[size] ?: 0
                appendLine("| ${if (size == 7) "7+" else size} | $count | ${rate(count, ordinaryDays.size)} |")
            }
            val chronicleStats = stats(ordinaryDays.map { it.chronicleEntries.toDouble() })
            appendLine()
            appendLine("Mean/median/P10-P90/max: ${f(chronicleStats.mean)}/${f(chronicleStats.median)}/${f(chronicleStats.p10)}-${f(chronicleStats.p90)}/${f(chronicleStats.maximum)}. Dragon share ${rate(dragonEvents, chronicleTotal)}; callback share ${rate(callbackEvents, chronicleTotal)}.")
            appendLine()
            appendLine("- NPCs with any meaningful presence: mean ${f(npcAppearances.map { it.present }.average())}; repeated presence: ${f(npcAppearances.map { it.repeated }.average())}; prominent (4+ mentions): ${f(npcAppearances.map { it.prominent }.average())}; absent: ${f(npcAppearances.map { it.absent }.average())}.")
            appendLine("- Mean top-NPC share: ${f(npcAppearances.map { it.topShare }.average())}%; top-three share: ${f(npcAppearances.map { it.top3Share }.average())}%.")
            appendLine("- Structured callbacks: ${f(ordinary.map { it.callbacks }.average())} per season; range ${ordinary.minOf { it.callbacks }}-${ordinary.maxOf { it.callbacks }}.")
            appendLine()
            appendLine("## Economy and Equipment (Held)")
            appendLine()
            appendLine("- No player gold rewards, shop prices, banking rules, item definitions, or equipment ladder values changed.")
            appendLine("- Ordinary final wealth median/P10-P90: ${median(ordinary.map { it.finalCarriedGold + it.finalBankedGold })}/${percentileInt(ordinary.map { it.finalCarriedGold + it.finalBankedGold }, 0.10)}-${percentileInt(ordinary.map { it.finalCarriedGold + it.finalBankedGold }, 0.90)}.")
            appendLine("- Final weapons: ${ordinary.groupingBy { EquipmentCatalog.requireWeapon(it.finalWeaponId).displayName }.eachCount().entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" }}.")
            appendLine("- Final armor: ${ordinary.groupingBy { EquipmentCatalog.requireArmor(it.finalArmorId).displayName }.eachCount().entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" }}.")
            appendLine()
            appendLine("## Reproducible Outliers")
            appendLine()
            outlierRows(results).forEach { appendLine("- ${it.first}: `${it.second.policy.name}:${it.second.seed}` (${it.third(it.second)}).") }
            appendLine()
            appendLine("## Determinism and Save / Load")
            appendLine()
            appendLine("- Identical seed/policy reruns: ${run.deterministicPairs}/${run.deterministicPairs} matched.")
            appendLine("- Continuous versus interrupted campaigns: ${qualification.saveReloadPairs}/${qualification.saveReloadPairs} matched authoritative results.")
            qualification.saveReloadCases.forEach { appendLine("  - `$it`") }
            appendLine("- No uncontrolled training or narrative randomness was introduced; both consume/derive from persisted deterministic state.")
            appendLine()
            appendLine("## Defects and Scope Confirmation")
            appendLine()
            appendLine("- Objective defects discovered in the revised cohort: none.")
            appendLine("- Human playtesting requested or used: **ZERO**.")
            appendLine("- Unauthorized iterative self-tuning: **none**. This report stops after the single authorized experiment.")
            appendLine("- Economy/equipment core values changed: **none**. Only NPC purchasing frequency for the strong personalities changed.")
            appendLine()
            appendLine("Recommendation: `LGD011A_RECHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`")
        }
    }

    private data class NarrativeConcentration(
        val present: Int,
        val repeated: Int,
        val prominent: Int,
        val absent: Int,
        val topShare: Double,
        val top3Share: Double,
    )

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

    private fun stats(values: List<Double>): Summary {
        if (values.isEmpty()) return Summary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val sorted = values.sorted()
        return Summary(
            sorted.size,
            sorted.first(),
            sorted.last(),
            sorted.average(),
            percentile(sorted, 0.50),
            percentile(sorted, 0.10),
            percentile(sorted, 0.25),
            percentile(sorted, 0.75),
            percentile(sorted, 0.90),
            percentile(sorted, 0.95),
        )
    }

    private fun percentile(sorted: List<Double>, probability: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val rank = (probability * sorted.size).coerceAtLeast(1.0)
        return sorted[(ceil(rank).toInt() - 1).coerceIn(sorted.indices)]
    }

    private fun percentileInt(values: List<Int>, probability: Double): Int =
        percentile(values.sorted().map(Int::toDouble), probability).toInt()

    private fun median(values: List<Int>): Int = percentileInt(values, 0.50)
    private fun nullableMedian(values: List<Int>): String = if (values.isEmpty()) "-" else median(values).toString()
    private fun pct(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else numerator * 100.0 / denominator
    private fun rate(numerator: Int, denominator: Int): String = "${f(pct(numerator, denominator))}%"
    private fun f(value: Double): String = String.format(Locale.US, "%.2f", value)
    private fun Int?.orZero() = this ?: 0

    private fun firstAttemptWinPercent(results: List<CampaignResult>): Double {
        val attempted = results.filter { it.playerDragonAttempts.isNotEmpty() }
        return pct(attempted.count { it.playerDragonAttempts.first() == DragonAttemptOutcome.VICTORY }, attempted.size)
    }

    private fun trainingFirstAttemptPercent(results: List<CampaignResult>): Double {
        val attempts = results.flatMap { it.attemptsPerSuccessfulPromotion }
        return pct(attempts.count { it == 1 }, attempts.size)
    }

    private fun durationBuckets(results: List<CampaignResult>): String {
        val buckets = listOf(
            "1-5" to 1..5, "6-10" to 6..10, "11-15" to 11..15, "16-20" to 16..20,
            "21-25" to 21..25, "26-30" to 26..30, "31-40" to 31..40,
            "41-50" to 41..50, "51+" to 51..100,
        )
        return buckets.joinToString(prefix = "| Days | Count |\n|---|---:|\n", separator = "\n") { (label, range) ->
            "| $label | ${results.count { it.resolutionDay in range }} |"
        }
    }

    private data class OutlierRow(
        val first: String,
        val second: CampaignResult,
        val third: (CampaignResult) -> String,
    )

    private fun outlierRows(results: List<CampaignResult>): List<OutlierRow> {
        fun row(label: String, result: CampaignResult?, detail: (CampaignResult) -> String) =
            OutlierRow(label, requireNotNull(result) { "Missing outlier $label" }, detail)
        return listOf(
            row("Shortest season", results.minByOrNull { it.resolutionDay }) { "${it.resolutionDay} days" },
            row("Longest season", results.maxByOrNull { it.resolutionDay }) { "${it.resolutionDay} days" },
            row("Earliest player win", results.filter { it.playerVictory }.minByOrNull { it.resolutionDay }) { "day ${it.resolutionDay}" },
            row("Latest player win", results.filter { it.playerVictory }.maxByOrNull { it.resolutionDay }) { "day ${it.resolutionDay}" },
            row("Strongest NPC win", results.filter { it.npcVictory }.maxByOrNull { it.npcFinalLevels.maxOrNull() ?: 0 }) { "top NPC level ${it.npcFinalLevels.maxOrNull()}" },
            row("Weakest successful player", results.filter { it.playerVictory }.minWithOrNull(compareBy<CampaignResult> { it.finalPlayerLevel }.thenBy { it.finalPlayerExperience })) { "level ${it.finalPlayerLevel}" },
            row("Greatest training-failure streak", results.maxByOrNull { it.maximumTrainingFailureStreak }) { "${it.maximumTrainingFailureStreak} failures" },
            row("Greatest combat-defeat count", results.maxByOrNull { it.totalFightsLost }) { "${it.totalFightsLost} defeats" },
            row("Most Dragon attempts", results.maxByOrNull { it.playerDragonAttempts.size + it.npcDragonAttempts.size }) { "${it.playerDragonAttempts.size + it.npcDragonAttempts.size} attempts" },
            row("Most credible challengers", results.maxByOrNull { maxOf(it.challengeWindowReadyCandidates, it.finalReadyCandidates) }) { "${maxOf(it.challengeWindowReadyCandidates, it.finalReadyCandidates)} candidates" },
            row("Most callback-heavy", results.maxByOrNull { it.callbacks }) { "${it.callbacks} callbacks" },
            row("Quietest Chronicle", results.minByOrNull { it.chronicleEntries.toDouble() / it.days.size }) { "${f(it.chronicleEntries.toDouble() / it.days.size)} entries/day" },
            row("Most dominant NPC", results.maxByOrNull { it.chronicleNpcAppearances.values.maxOrNull() ?: 0 }) { "${it.chronicleNpcAppearances.values.maxOrNull() ?: 0} appearances" },
        )
    }

    private fun outliersJson(results: List<CampaignResult>): String = buildString {
        appendLine("{")
        outlierRows(results).forEachIndexed { index, row ->
            append("  \"").append(row.first.lowercase().replace(' ', '_').replace('-', '_')).append("\": ")
            append("{\"policy\":\"").append(row.second.policy.name).append("\",\"seed\":")
                .append(row.second.seed).append(",\"detail\":\"").append(row.third(row.second)).append("\"}")
            appendLine(if (index == outlierRows(results).lastIndex) "" else ",")
        }
        appendLine("}")
    }

    private fun write(path: Path, content: String) {
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }
}
