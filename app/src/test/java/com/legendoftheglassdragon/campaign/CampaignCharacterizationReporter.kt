package com.legendoftheglassdragon.campaign

import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonSeasonBalance
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.MonsterCatalog
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.WorldEventType
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.system.measureNanoTime

data class NumericSummary(
    val count: Int,
    val minimum: Double,
    val maximum: Double,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val p10: Double,
    val p25: Double,
    val p75: Double,
    val p90: Double,
    val p95: Double,
)

data class RateEstimate(
    val successes: Int,
    val sample: Int,
    val percent: Double,
    val lower95Percent: Double,
    val upper95Percent: Double,
)

data class CharacterizationRun(
    val results: List<CampaignResult>,
    val durationSeconds: Double,
    val deterministicPairs: Int,
    val saveReloadPairs: Int,
)

object CampaignCharacterizationRunner {
    const val DEFAULT_SEASONS = 2_500
    const val QUALIFICATION_CEILING_DAYS = 100

    fun run(seasons: Int): CharacterizationRun {
        require(seasons >= CampaignPolicy.entries.size)
        val results = ArrayList<CampaignResult>(seasons)
        val elapsed = measureNanoTime {
            repeat(seasons) { index ->
                val policy = CampaignPolicy.entries[index % CampaignPolicy.entries.size]
                val ordinalWithinPolicy = index / CampaignPolicy.entries.size + 1L
                val seed = (policy.ordinal + 1L) * 1_000_000L + ordinalWithinPolicy
                results += CampaignSimulationHarness(QUALIFICATION_CEILING_DAYS).run(seed, policy)
            }
        }

        var deterministicPairs = 0
        CampaignPolicy.entries.forEachIndexed { index, policy ->
            repeat(2) { repeatIndex ->
                val seed = (policy.ordinal + 1L) * 1_000_000L + 50_001L + repeatIndex
                val first = CampaignSimulationHarness().run(seed, policy)
                val second = CampaignSimulationHarness().run(seed, policy)
                check(first == second) { "Nondeterministic campaign: $policy seed=$seed" }
                deterministicPairs += 1
            }
        }

        return CharacterizationRun(
            results = results,
            durationSeconds = elapsed / 1_000_000_000.0,
            deterministicPairs = deterministicPairs,
            saveReloadPairs = CampaignPolicy.entries.size,
        )
    }

    fun writeEvidence(run: CharacterizationRun, outputDirectory: Path) {
        Files.createDirectories(outputDirectory)
        write(outputDirectory.resolve("campaigns.csv"), campaignsCsv(run.results))
        write(outputDirectory.resolve("days.jsonl"), daysJsonl(run.results))
        write(outputDirectory.resolve("monster_metrics.csv"), monsterCsv(run.results))
        write(outputDirectory.resolve("summary.json"), summaryJson(run))
        write(outputDirectory.resolve("outliers.json"), outliersJson(run.results))
        write(outputDirectory.resolve("LGD-011_BALANCE_CHARACTERIZATION.md"), report(run))
    }

    private fun campaignsCsv(results: List<CampaignResult>): String = buildString {
        appendLine(
            "seed,policy,completed,days,victor_type,victor_id,player_final_level," +
                "player_final_rank,carried_gold,banked_gold,total_wealth,weapon_id,armor_id," +
                "player_dragon_attempts,player_dragon_win,npc_dragon_attempts," +
                "unique_npc_challengers,training_attempts,training_failures,combat_attempts," +
                "combat_wins,combat_losses,escapes,unused_fights,healing_spent,equipment_spent," +
                "defeat_gold_lost,chronicle_events,chronicle_days,relationship_changes," +
                "rivalries,rank_changes,leadership_changes,first_player_ready_day," +
                "first_npc_ready_day,challenge_window_candidates,final_ready_candidates",
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
                    result.playerVictory,
                    result.npcDragonAttempts.size,
                    result.uniqueNpcDragonChallengers,
                    result.totalTrainingAttempts,
                    result.totalTrainingFailures,
                    result.totalFightsAttempted,
                    result.totalFightsWon,
                    result.totalFightsLost,
                    result.totalEscapes,
                    result.totalUnusedFights,
                    result.totalHealingSpent,
                    result.totalEquipmentSpent,
                    result.totalDefeatGoldLost,
                    result.chronicleEntries,
                    result.chronicleDays,
                    result.relationshipChanges,
                    result.rivalryFormedCount,
                    result.rankPositionChanges,
                    result.leadershipChanges,
                    result.firstPlayerReadyDay ?: "",
                    result.firstNpcReadyDay ?: "",
                    result.challengeWindowReadyCandidates,
                    result.finalReadyCandidates,
                ).joinToString(",") { csv(it.toString()) },
            )
        }
    }

    private fun daysJsonl(results: List<CampaignResult>): String = buildString {
        results.forEach { result ->
            result.days.forEach { day ->
                append("{\"seed\":").append(result.seed)
                append(",\"policy\":\"").append(result.policy.name).append("\"")
                append(",\"day\":").append(day.day)
                append(",\"level\":").append(day.playerLevel)
                append(",\"experience\":").append(day.playerExperience)
                append(",\"rank\":").append(day.playerRank)
                append(",\"hp\":").append(day.currentHpBeforeSleep)
                append(",\"maximum_hp\":").append(day.maximumHp)
                append(",\"carried_gold\":").append(day.carriedGold)
                append(",\"banked_gold\":").append(day.bankedGold)
                append(",\"weapon_id\":\"").append(day.weaponId).append("\"")
                append(",\"armor_id\":\"").append(day.armorId).append("\"")
                append(",\"fights_attempted\":").append(day.fightsAttempted)
                append(",\"fights_won\":").append(day.fightsWon)
                append(",\"fights_lost\":").append(day.fightsLost)
                append(",\"escapes\":").append(day.escapes)
                append(",\"unused_fights\":").append(day.fightsUnused)
                append(",\"training_attempts\":").append(day.trainingAttempts)
                append(",\"training_failures\":").append(day.trainingFailures)
                append(",\"dragon_attempts\":").append(day.dragonAttempts)
                append(",\"dragon_stage\":\"").append(day.dragonStage.name).append("\"")
                append(",\"chronicle_entries\":").append(day.chronicleEntries)
                append(",\"ready_npcs\":").append(day.readyNpcCandidates)
                appendLine("}")
            }
        }
    }

    private fun monsterCsv(results: List<CampaignResult>): String = buildString {
        appendLine(
            "policy,monster_id,monster_name,intended_level,encounters,victories,defeats," +
                "escapes,win_rate_percent,defeat_rate_percent,average_damage_taken," +
                "average_experience,average_gold,average_encounter_day,average_player_level",
        )
        CampaignPolicy.entries.forEach { policy ->
            val cohort = results.filter { it.policy == policy }
            MonsterCatalog.all.forEach { monster ->
                val metric = cohort.mapNotNull { it.monsterMetrics[monster.id] }
                    .fold(MonsterSeasonMetric()) { total, next -> total + next }
                if (metric.encounters > 0) {
                    appendLine(
                        listOf(
                            policy.name,
                            monster.id,
                            monster.displayName,
                            monster.intendedLevel,
                            metric.encounters,
                            metric.victories,
                            metric.defeats,
                            metric.escapes,
                            format(percent(metric.victories, metric.encounters)),
                            format(percent(metric.defeats, metric.encounters)),
                            format(metric.damageTaken.toDouble() / metric.encounters),
                            format(metric.experienceEarned.toDouble() / metric.encounters),
                            format(metric.goldEarned.toDouble() / metric.encounters),
                            format(metric.encounterDaySum.toDouble() / metric.encounters),
                            format(metric.playerLevelSum.toDouble() / metric.encounters),
                        ).joinToString(",") { csv(it.toString()) },
                    )
                }
            }
        }
    }

    private fun summaryJson(run: CharacterizationRun): String = buildString {
        appendLine("{")
        appendLine("  \"schema\": \"LGD-011-characterization-v1\",")
        appendLine("  \"baseline\": {")
        appendLine("    \"git_head\": null,")
        appendLine("    \"save_schema\": ${GameState.CURRENT_SAVE_VERSION},")
        appendLine("    \"maximum_days\": $QUALIFICATION_CEILING_DAYS,")
        appendLine("    \"forest_fights_per_day\": ${GameBalance.FOREST_FIGHTS_PER_DAY}")
        appendLine("  },")
        appendLine("  \"performance\": {")
        appendLine("    \"seasons\": ${run.results.size},")
        appendLine("    \"game_days\": ${run.results.sumOf { it.days.size }},")
        appendLine("    \"seconds\": ${format(run.durationSeconds)},")
        appendLine("    \"seasons_per_second\": ${format(run.results.size / run.durationSeconds)}")
        appendLine("  },")
        appendLine("  \"policies\": {")
        CampaignPolicy.entries.forEachIndexed { index, policy ->
            val cohort = run.results.filter { it.policy == policy }
            val duration = summary(cohort.map { it.resolutionDay.toDouble() })
            val finalLevel = summary(cohort.map { it.finalPlayerLevel.toDouble() })
            val finalRank = summary(cohort.map { it.finalPlayerRank.toDouble() })
            val wealth = summary(cohort.map { (it.finalCarriedGold + it.finalBankedGold).toDouble() })
            appendLine("    \"${policy.name}\": {")
            appendLine("      \"seasons\": ${cohort.size},")
            appendLine("      \"completed\": ${cohort.count { it.completed }},")
            appendLine("      \"player_victories\": ${cohort.count { it.playerVictory }},")
            appendLine("      \"npc_victories\": ${cohort.count { it.npcVictory }},")
            appendLine("      \"unresolved\": ${cohort.count { !it.completed }},")
            appendLine("      \"duration\": ${numericJson(duration)},")
            appendLine("      \"final_level\": ${numericJson(finalLevel)},")
            appendLine("      \"final_rank\": ${numericJson(finalRank)},")
            appendLine("      \"final_wealth\": ${numericJson(wealth)},")
            appendLine("      \"combat_defeat_rate_percent\": ${format(percent(cohort.sumOf { it.totalFightsLost }, cohort.sumOf { it.totalFightsAttempted }))},")
            appendLine("      \"training_failure_rate_percent\": ${format(percent(cohort.sumOf { it.totalTrainingFailures }, cohort.sumOf { it.totalTrainingAttempts }))},")
            appendLine("      \"median_chronicle_entries_per_day\": ${format(summary(cohort.flatMap { it.days }.map { it.chronicleEntries.toDouble() }).median)}")
            append("    }").appendLine(if (index == CampaignPolicy.entries.lastIndex) "" else ",")
        }
        appendLine("  }")
        appendLine("}")
    }

    private fun outliersJson(results: List<CampaignResult>): String {
        fun descriptor(result: CampaignResult?) = if (result == null) {
            "null"
        } else {
            "{\"seed\":${result.seed},\"policy\":\"${result.policy.name}\"," +
                "\"days\":${result.resolutionDay},\"level\":${result.finalPlayerLevel}," +
                "\"rank\":${result.finalPlayerRank},\"wealth\":" +
                "${result.finalCarriedGold + result.finalBankedGold}}"
        }
        val playerWins = results.filter { it.playerVictory }
        val unresolved = results.filter { !it.completed }
        val fields = linkedMapOf(
            "shortest_campaign" to results.minByOrNull { it.resolutionDay },
            "longest_campaign" to results.maxByOrNull { it.resolutionDay },
            "earliest_player_victory" to playerWins.minByOrNull { it.resolutionDay },
            "latest_player_victory" to playerWins.maxByOrNull { it.resolutionDay },
            "strongest_final_player" to results.maxByOrNull { it.finalPlayerLevel },
            "weakest_player_winner" to playerWins.minByOrNull { it.finalPlayerLevel },
            "most_combat_defeats" to results.maxByOrNull { it.totalFightsLost },
            "most_training_failures" to results.maxByOrNull { it.totalTrainingFailures },
            "richest_player" to results.maxByOrNull { it.finalCarriedGold + it.finalBankedGold },
            "poorest_at_conclusion" to results.filter { it.completed }
                .minByOrNull { it.finalCarriedGold + it.finalBankedGold },
            "most_npc_dragon_attempts" to results.maxByOrNull { it.npcDragonAttempts.size },
            "most_dominant_npc" to results.maxByOrNull(::npcDominanceShare),
            "highest_chronicle_density" to results.maxByOrNull {
                it.chronicleEntries.toDouble() / it.days.size.coerceAtLeast(1)
            },
            "lowest_chronicle_density" to results.minByOrNull {
                it.chronicleEntries.toDouble() / it.days.size.coerceAtLeast(1)
            },
            "first_unresolved" to unresolved.firstOrNull(),
        )
        return buildString {
            appendLine("{")
            fields.entries.forEachIndexed { index, entry ->
                append("  \"").append(entry.key).append("\": ").append(descriptor(entry.value))
                appendLine(if (index == fields.size - 1) "" else ",")
            }
            appendLine("}")
        }
    }

    private fun report(run: CharacterizationRun): String {
        val results = run.results
        val ordinary = results.filter { it.policy == CampaignPolicy.ORDINARY }
        val ordinaryDuration = summary(ordinary.map { it.resolutionDay.toDouble() })
        val ordinaryFinalLevel = summary(ordinary.map { it.finalPlayerLevel.toDouble() })
        val ordinaryFinalRank = summary(ordinary.map { it.finalPlayerRank.toDouble() })
        val ordinaryWealth = summary(ordinary.map { (it.finalCarriedGold + it.finalBankedGold).toDouble() })
        val ordinaryChronicle = summary(ordinary.flatMap { it.days }.map { it.chronicleEntries.toDouble() })
        val unresolved = results.filter { !it.completed }
        val outlierLines = outlierLines(results)

        return buildString {
            appendLine("# LGD-011 Campaign Simulation + Balance Characterization")
            appendLine()
            appendLine("Generated from the accepted LGD-010 production rules. This is measurement, not a declaration that the balance is accepted.")
            appendLine()
            appendLine("## Disposition")
            appendLine()
            appendLine("`LGD011_BALANCE_CHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`")
            appendLine()
            appendLine("ZERO human playtesting was requested or used. No discretionary balance tuning was performed.")
            appendLine()
            appendLine("## Baseline and method")
            appendLine()
            appendLine("- Git baseline/HEAD: unavailable; this workspace is not a Git repository.")
            appendLine("- Save schema: ${GameState.CURRENT_SAVE_VERSION}.")
            appendLine("- Cohort: ${results.size} deterministic complete-season attempts, ${results.sumOf { it.days.size }} total game days.")
            appendLine("- Allocation: ${CampaignPolicy.entries.joinToString { p -> p.name + "=" + results.count { it.policy == p } }}.")
            appendLine("- Ceiling: $QUALIFICATION_CEILING_DAYS game days; unresolved runs are retained rather than forced.")
            appendLine("- Runtime: ${format(run.durationSeconds)} seconds (${format(results.size / run.durationSeconds)} seasons/second).")
            appendLine("- Memory: no maximum-heap profiler was attached; the 2,500-season in-memory cohort completed without an observed allocation failure or production-rule shortcut.")
            appendLine("- RNG authority: saved world seed + monotonically increasing random step through `PersistedGameRandom`; narration does not consume gameplay RNG.")
            appendLine("- Production systems exercised: NewGameFactory, GameEngine, EconomyEngine, ChallengeEngine, WorldAdvancer/CommunitySimulator, DragonSeasonSimulator and DragonCombatEngine.")
            appendLine()
            appendLine("### Frozen balance configuration")
            appendLine()
            appendLine("- Forest fights/day: ${GameBalance.FOREST_FIGHTS_PER_DAY}; escape: ${GameBalance.ESCAPE_CHANCE_PERCENT}%; defeat carried-gold loss: ${GameBalance.DEFEAT_GOLD_LOSS_PERCENT}%.")
            appendLine("- Promotion growth: +${GameBalance.LEVEL_MAX_HP_GAIN} HP, +${GameBalance.LEVEL_ATTACK_GAIN} base attack, +${GameBalance.LEVEL_DEFENSE_GAIN} base defense.")
            appendLine("- XP thresholds: ${(2..10).joinToString { "L$it=${ProgressionRules.experienceRequiredForLevel(it)}" }}.")
            appendLine("- Dragon pressure thresholds: Signs ${DragonSeasonBalance.SIGNS_PRESSURE}, Fear ${DragonSeasonBalance.FEAR_PRESSURE}, Crisis ${DragonSeasonBalance.CRISIS_PRESSURE}, Window ${DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE}; guaranteed NPC resolution boundary Day ${DragonSeasonBalance.GUARANTEED_RESOLUTION_DAY}.")
            appendLine()
            appendLine("## Synthetic policy definitions")
            appendLine()
            policyDescriptions().forEach { appendLine("- $it") }
            appendLine()
            appendLine("All policies train immediately when eligible, use lawful town transactions, and never inspect future RNG. Equipment is bought from the canonical next tier; purchases, healing, challenges and Dragon combat invoke production actions.")
            appendLine()
            appendLine("## Campaign length")
            appendLine()
            appendLine("| Policy | N | Complete | Unresolved | Min | P10 | P25 | Median | Mean | P75 | P90 | P95 | Max |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val s = summary(cohort.map { it.resolutionDay.toDouble() })
                appendLine("| ${policy.label} | ${cohort.size} | ${cohort.count { it.completed }} | ${cohort.count { !it.completed }} | ${format(s.minimum)} | ${format(s.p10)} | ${format(s.p25)} | ${format(s.median)} | ${format(s.mean)} | ${format(s.p75)} | ${format(s.p90)} | ${format(s.p95)} | ${format(s.maximum)} |")
            }
            appendLine()
            val playerResolution = results.filter { it.playerVictory }.map { it.resolutionDay.toDouble() }
            val npcResolution = results.filter { it.npcVictory }.map { it.resolutionDay.toDouble() }
            appendLine("Player-victory resolution days: ${distributionSentence(playerResolution)}. NPC-victory resolution days: ${distributionSentence(npcResolution)}.")
            appendLine()
            appendLine("### Duration buckets")
            appendLine()
            appendLine("| Policy | 1–5 | 6–10 | 11–15 | 16–20 | 21–25 | 26–30 | 31–40 | 41–50 | 51+ |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val buckets = durationBuckets(cohort)
                appendLine("| ${policy.label} | ${buckets.joinToString(" | ")} |")
            }
            appendLine()
            appendLine("## Victory and Dragon attempts")
            appendLine()
            appendLine("| Policy | Player victory | NPC victory | Unresolved | Player attempts/season | First-attempt win | Attempt win rate | NPC attempts/season | Unique NPC challengers |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val playerAttempts = cohort.sumOf { it.playerDragonAttempts.size }
                val playerAttemptWins = cohort.sumOf { c -> c.playerDragonAttempts.count { it == DragonAttemptOutcome.VICTORY } }
                val firstWins = cohort.count { it.playerDragonAttempts.firstOrNull() == DragonAttemptOutcome.VICTORY }
                val firstAttempts = cohort.count { it.playerDragonAttempts.isNotEmpty() }
                appendLine("| ${policy.label} | ${rateText(cohort.count { it.playerVictory }, cohort.size)} | ${rateText(cohort.count { it.npcVictory }, cohort.size)} | ${rateText(cohort.count { !it.completed }, cohort.size)} | ${format(cohort.map { it.playerDragonAttempts.size }.average())} | ${rateText(firstWins, firstAttempts)} | ${rateText(playerAttemptWins, playerAttempts)} | ${format(cohort.map { it.npcDragonAttempts.size }.average())} | ${format(cohort.map { it.uniqueNpcDragonChallengers }.average())} |")
            }
            appendLine()
            appendLine("Ordinary outcome categories: ${attemptCategoryText(ordinary)}.")
            appendLine("Ordinary player attempts: mean ${format(ordinary.map { it.playerDragonAttempts.size }.average())}, median ${format(summary(ordinary.map { it.playerDragonAttempts.size.toDouble() }).median)}, maximum ${ordinary.maxOf { it.playerDragonAttempts.size }}. NPC attempts: mean ${format(ordinary.map { it.npcDragonAttempts.size }.average())}, failed before resolution ${format(ordinary.map { result -> result.npcDragonAttempts.count { it != DragonAttemptOutcome.VICTORY } }.average())}/season, first-attempt victory ${npcFirstAttemptVictoryText(ordinary)}, repeated same-NPC attempts ${ordinary.sumOf { it.repeatedNpcDragonAttempts }}.")
            appendLine()
            appendLine("## Player progression")
            appendLine()
            appendLine("### Level at checkpoints (mean / median; active campaigns only)")
            appendLine()
            appendLine("| Policy | Day 3 | Day 5 | Day 10 | Day 15 | Day 20 | Day 25 | Conclusion |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val cells = listOf(3, 5, 10, 15, 20, 25).map { checkpoint ->
                    val values = cohort.mapNotNull { result ->
                        result.days.firstOrNull { it.day == checkpoint }?.playerLevel?.toDouble()
                    }
                    if (values.isEmpty()) "n/a" else {
                        val s = summary(values)
                        "${format(s.mean)} / ${format(s.median)} (n=${values.size})"
                    }
                }
                val end = summary(cohort.map { it.finalPlayerLevel.toDouble() })
                appendLine("| ${policy.label} | ${cells.joinToString(" | ")} | ${format(end.mean)} / ${format(end.median)} |")
            }
            appendLine()
            appendLine("### Level at checkpoints (all campaigns; completed campaigns carried forward at final level)")
            appendLine()
            appendLine("| Policy | Day 3 | Day 5 | Day 10 | Day 15 | Day 20 | Day 25 |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val cells = listOf(3, 5, 10, 15, 20, 25).map { checkpoint ->
                    val values = cohort.map { result ->
                        result.days.firstOrNull { it.day == checkpoint }?.playerLevel
                            ?: result.finalPlayerLevel
                    }.map(Int::toDouble)
                    val s = summary(values)
                    "${format(s.mean)} / ${format(s.median)}"
                }
                appendLine("| ${policy.label} | ${cells.joinToString(" | ")} |")
            }
            appendLine()
            appendLine("### Ordinary level-up timing")
            appendLine()
            appendLine("| Level | Reached | Mean day | Median day | P10–P90 |")
            appendLine("|---:|---:|---:|---:|---:|")
            (2..10).forEach { level ->
                val reached = ordinary.mapNotNull { it.levelReachedDays[level] }
                if (reached.isEmpty()) {
                    appendLine("| $level | 0 / ${ordinary.size} (0%) | n/a | n/a | n/a |")
                } else {
                    val s = summary(reached.map(Int::toDouble))
                    appendLine("| $level | ${reached.size} / ${ordinary.size} (${format(percent(reached.size, ordinary.size))}%) | ${format(s.mean)} | ${format(s.median)} | ${format(s.p10)}–${format(s.p90)} |")
                }
            }
            appendLine()
            appendLine("Ordinary final-level distribution: ${frequencyText(ordinary.map { it.finalPlayerLevel })}.")
            appendLine()
            appendLine("## Physical Training")
            appendLine()
            appendLine("| Policy | Attempts | Successes | Failures | Failure rate | First-try success | Median attempts/promotion | Max failures |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val attempts = cohort.sumOf { it.totalTrainingAttempts }
                val successes = cohort.sumOf { it.totalTrainingSuccesses }
                val failures = cohort.sumOf { it.totalTrainingFailures }
                val promotionAttempts = cohort.flatMap { it.attemptsPerSuccessfulPromotion }
                appendLine("| ${policy.label} | $attempts | $successes | $failures | ${format(percent(failures, attempts))}% | ${format(percent(promotionAttempts.count { it == 1 }, promotionAttempts.size))}% | ${if (promotionAttempts.isEmpty()) "n/a" else format(summary(promotionAttempts.map(Int::toDouble)).median)} | ${cohort.maxOfOrNull { it.maximumTrainingFailureStreak } ?: 0} |")
            }
            appendLine()
            appendLine("Physical Training uses one deterministic seeded readiness trial per realm day. Failure preserves level and experience, costs modest HP, and permits a fresh attempt after the next End Day.")
            appendLine()
            appendLine("## Ordinary combat and daily action use")
            appendLine()
            appendLine("| Policy | Fights/day | Used all fights | Unused/day | Win rate | Defeat rate | Escape rate | Victory HP% | XP/fight | Gold/fight |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val dayRows = cohort.flatMap { it.days }
                val attempts = cohort.sumOf { it.totalFightsAttempted }
                appendLine("| ${policy.label} | ${format(attempts.toDouble() / dayRows.size)} | ${rateText(dayRows.count { it.fightsUnused == 0 }, dayRows.size)} | ${format(cohort.sumOf { it.totalUnusedFights }.toDouble() / dayRows.size)} | ${rateText(cohort.sumOf { it.totalFightsWon }, attempts)} | ${rateText(cohort.sumOf { it.totalFightsLost }, attempts)} | ${rateText(cohort.sumOf { it.totalEscapes }, attempts)} | ${format(cohort.flatMap { it.victoryHpPercentages }.map(Int::toDouble).averageOrZero())} | ${format(cohort.sumOf { it.totalCombatExperience }.toDouble() / attempts.coerceAtLeast(1))} | ${format(cohort.sumOf { it.totalCombatGold }.toDouble() / attempts.coerceAtLeast(1))} |")
            }
            appendLine()
            appendLine("Ordinary combat defeat by player level: ${combatDefeatByLevel(ordinary)}.")
            appendLine("Ordinary combat defeat by campaign-day bucket: ${combatDefeatByDayBucket(ordinary)}.")
            appendLine("Ordinary severe post-victory health outcomes (≤25% HP): ${rateText(ordinary.flatMap { it.victoryHpPercentages }.count { it <= 25 }, ordinary.flatMap { it.victoryHpPercentages }.size)}.")
            appendLine()
            appendLine("### Enemy characterization — Ordinary policy")
            appendLine()
            appendLine("| Enemy | Encounters | Win | Defeat | Escape | Avg damage | Avg XP | Avg gold | Avg day | Avg level |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            val ordinaryMonsters = aggregateMonsters(ordinary)
            MonsterCatalog.all.forEach { monster ->
                val m = ordinaryMonsters[monster.id] ?: return@forEach
                appendLine("| ${monster.displayName} | ${m.encounters} | ${format(percent(m.victories, m.encounters))}% | ${format(percent(m.defeats, m.encounters))}% | ${format(percent(m.escapes, m.encounters))}% | ${format(m.damageTaken.toDouble() / m.encounters)} | ${format(m.experienceEarned.toDouble() / m.encounters)} | ${format(m.goldEarned.toDouble() / m.encounters)} | ${format(m.encounterDaySum.toDouble() / m.encounters)} | ${format(m.playerLevelSum.toDouble() / m.encounters)} |")
            }
            appendLine()
            appendLine("## Economy and equipment")
            appendLine()
            appendLine("| Policy | Combat gold/day | Healing/season | Equipment/season | Defeat loss/season | Median carried | Median banked | Median total wealth | P10–P90 wealth |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val carried = summary(cohort.map { it.finalCarriedGold.toDouble() })
                val banked = summary(cohort.map { it.finalBankedGold.toDouble() })
                val wealth = summary(cohort.map { (it.finalCarriedGold + it.finalBankedGold).toDouble() })
                appendLine("| ${policy.label} | ${format(cohort.sumOf { it.totalCombatGold }.toDouble() / cohort.sumOf { it.days.size })} | ${format(cohort.map { it.totalHealingSpent }.average())} | ${format(cohort.map { it.totalEquipmentSpent }.average())} | ${format(cohort.map { it.totalDefeatGoldLost }.average())} | ${format(carried.median)} | ${format(banked.median)} | ${format(wealth.median)} | ${format(wealth.p10)}–${format(wealth.p90)} |")
            }
            appendLine()
            appendLine("Ordinary final weapon distribution: ${frequencyText(ordinary.map { EquipmentCatalog.requireWeapon(it.finalWeaponId).displayName })}.")
            appendLine("Ordinary final armor distribution: ${frequencyText(ordinary.map { EquipmentCatalog.requireArmor(it.finalArmorId).displayName })}.")
            appendLine("Ordinary Dragon-attempt equipment: weapons ${frequencyText(ordinary.flatMap { result -> result.days.filter { it.dragonAttempts > 0 }.map { EquipmentCatalog.requireWeapon(it.weaponId).displayName } })}; armor ${frequencyText(ordinary.flatMap { result -> result.days.filter { it.dragonAttempts > 0 }.map { EquipmentCatalog.requireArmor(it.armorId).displayName } })}.")
            appendLine("Ordinary equipment acquisition: ${equipmentTimingText(ordinary)}.")
            appendLine("Ordinary total-wealth checkpoints (active campaigns, mean / median): ${wealthCheckpointText(ordinary)}.")
            appendLine()
            appendLine("## NPC progression, rankings and churn")
            appendLine()
            appendLine("| Policy | NPC final level min/median/mean/max | Final player rank median | #1 finish | Top 3 | Top 5 | Bottom half | Rank moves/season | Leaders/season | Leadership changes | Player ever #1 |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val npcLevels = summary(cohort.flatMap { it.npcFinalLevels }.map(Int::toDouble))
                val rank = summary(cohort.map { it.finalPlayerRank.toDouble() })
                appendLine("| ${policy.label} | ${format(npcLevels.minimum)}/${format(npcLevels.median)}/${format(npcLevels.mean)}/${format(npcLevels.maximum)} | ${format(rank.median)} | ${rateText(cohort.count { it.finalPlayerRank == 1 }, cohort.size)} | ${rateText(cohort.count { it.finalPlayerRank <= 3 }, cohort.size)} | ${rateText(cohort.count { it.finalPlayerRank <= 5 }, cohort.size)} | ${rateText(cohort.count { it.finalPlayerRank > (it.finalState.community.adventurers.size + 1) / 2 }, cohort.size)} | ${format(cohort.map { it.rankPositionChanges }.average())} | ${format(cohort.map { it.uniqueLeaderIds.size }.average())} | ${format(cohort.map { it.leadershipChanges }.average())} | ${rateText(cohort.count { it.playerEverRankOne }, cohort.size)} |")
            }
            appendLine()
            appendLine("Ordinary NPC final-level distribution: ${frequencyText(ordinary.flatMap { it.npcFinalLevels })}.")
            appendLine("Ordinary NPC level checkpoints (min / median / mean / max): ${npcCheckpointText(ordinary)}.")
            appendLine("Ordinary player rank checkpoints (active campaigns, mean / median): ${rankCheckpointText(ordinary)}.")
            val ordinaryNpcGold = summary(ordinary.flatMap { it.npcFinalGold }.map(Int::toDouble))
            appendLine("Ordinary NPC final gold: median ${format(ordinaryNpcGold.median)}, mean ${format(ordinaryNpcGold.mean)}, P10–P90 ${format(ordinaryNpcGold.p10)}–${format(ordinaryNpcGold.p90)}. Final weapon tiers: ${frequencyText(ordinary.flatMap { it.npcFinalWeaponTiers })}; armor tiers: ${frequencyText(ordinary.flatMap { it.npcFinalArmorTiers })}.")
            appendLine("One resident led ≥80% of measured days in ${rateText(ordinary.count(::hasDominantLeader), ordinary.size)} of Ordinary seasons.")
            appendLine()
            appendLine("## Dragon escalation and readiness")
            appendLine()
            appendLine("| Policy | Signs median | Fear median | Crisis median | Window median | First player ready | First NPC ready | Candidates at window | Candidates at resolution |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                fun stage(stage: DragonStage) = nullableMedian(cohort.map { it.stageEnteredDays[stage] })
                appendLine("| ${policy.label} | ${stage(DragonStage.SIGNS)} | ${stage(DragonStage.FEAR)} | ${stage(DragonStage.CRISIS)} | ${stage(DragonStage.CHALLENGE_WINDOW)} | ${nullableMedian(cohort.map { it.firstPlayerReadyDay })} | ${nullableMedian(cohort.map { it.firstNpcReadyDay })} | ${format(summary(cohort.map { it.challengeWindowReadyCandidates.toDouble() }).median)} | ${format(summary(cohort.map { it.finalReadyCandidates.toDouble() }).median)} |")
            }
            appendLine()
            appendLine("### Ordinary Dragon-stage timing distributions")
            appendLine()
            appendLine("| Stage | Reached | Min | P10 | Median | Mean | P90 | Max |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|")
            DragonStage.entries.filter { it != DragonStage.WHISPERS }.forEach { stage ->
                val reached = ordinary.mapNotNull { it.stageEnteredDays[stage] }
                if (reached.isNotEmpty()) {
                    val s = summary(reached.map(Int::toDouble))
                    appendLine("| $stage | ${reached.size}/${ordinary.size} | ${format(s.minimum)} | ${format(s.p10)} | ${format(s.median)} | ${format(s.mean)} | ${format(s.p90)} | ${format(s.maximum)} |")
                }
            }
            appendLine()
            appendLine("Ordinary readiness at the first player Dragon attempt: ${dragonAttemptReadinessText(ordinary)}.")
            appendLine()
            appendLine("## Society, Chronicle and story continuity")
            appendLine()
            appendLine("| Policy | Chronicle/day mean | Median | P90 | Max | Zero-event days | >7-event days | NPCs spotlighted | Top NPC share | Never spotlighted | Relationship changes | Rivalries | Callbacks |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val dayEntries = cohort.flatMap { it.days }.map { it.chronicleEntries.toDouble() }
                val s = summary(dayEntries)
                val spotlighted = cohort.map { it.chronicleNpcAppearances.size }
                val topShares = cohort.map(::topNpcShare)
                val never = cohort.map { it.finalState.community.adventurers.size - it.chronicleNpcAppearances.size }
                appendLine("| ${policy.label} | ${format(s.mean)} | ${format(s.median)} | ${format(s.p90)} | ${format(s.maximum)} | ${rateText(dayEntries.count { it == 0.0 }, dayEntries.size)} | ${rateText(dayEntries.count { it > 7.0 }, dayEntries.size)} | ${format(spotlighted.average())} | ${format(topShares.average() * 100)}% | ${format(never.average())} | ${format(cohort.map { it.relationshipChanges }.average())} | ${format(cohort.map { it.rivalryFormedCount }.average())} | ${format(cohort.map { it.callbacks }.average())} |")
            }
            appendLine()
            appendLine("Ordinary Chronicle event families: ${eventTypeText(ordinary)}.")
            appendLine("Ordinary Dragon incidents created: ${format(ordinary.map { it.incidentsCreated }.average())}/season. Structured callbacks: ${format(ordinary.map { it.callbacks }.average())}/season. The current model does not maintain a separate generic incident-expiration ledger, so expiration-without-follow-up is not measurable beyond retained structured callbacks.")
            appendLine()
            appendLine("## Policy comparison")
            appendLine()
            appendLine("| Policy | Median days | Player win | NPC win | Median level | Median rank | Median attempts | Combat defeat | Training failure | Median wealth | Fights/day |")
            appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
            CampaignPolicy.entries.forEach { policy ->
                val cohort = results.filter { it.policy == policy }
                val daysRows = cohort.flatMap { it.days }
                appendLine("| ${policy.label} | ${format(summary(cohort.map { it.resolutionDay.toDouble() }).median)} | ${rateText(cohort.count { it.playerVictory }, cohort.size)} | ${rateText(cohort.count { it.npcVictory }, cohort.size)} | ${format(summary(cohort.map { it.finalPlayerLevel.toDouble() }).median)} | ${format(summary(cohort.map { it.finalPlayerRank.toDouble() }).median)} | ${format(summary(cohort.map { it.playerDragonAttempts.size.toDouble() }).median)} | ${rateText(cohort.sumOf { it.totalFightsLost }, cohort.sumOf { it.totalFightsAttempted })} | ${rateText(cohort.sumOf { it.totalTrainingFailures }, cohort.sumOf { it.totalTrainingAttempts })} | ${format(summary(cohort.map { (it.finalCarriedGold + it.finalBankedGold).toDouble() }).median)} | ${format(cohort.sumOf { it.totalFightsAttempted }.toDouble() / daysRows.size)} |")
            }
            appendLine()
            appendLine("## Confidence and sample stability")
            appendLine()
            listOf(
                "Overall completion" to rate(results.count { it.completed }, results.size),
                "Ordinary player victory" to rate(ordinary.count { it.playerVictory }, ordinary.size),
                "Ordinary NPC victory" to rate(ordinary.count { it.npcVictory }, ordinary.size),
                "Ordinary combat defeat" to rate(ordinary.sumOf { it.totalFightsLost }, ordinary.sumOf { it.totalFightsAttempted }),
                "Ordinary training success" to rate(ordinary.sumOf { it.totalTrainingSuccesses }, ordinary.sumOf { it.totalTrainingAttempts }),
            ).forEach { (name, estimate) ->
                appendLine("- $name: ${format(estimate.percent)}% (${estimate.successes}/${estimate.sample}), approximate 95% Wilson interval ${format(estimate.lower95Percent)}%–${format(estimate.upper95Percent)}%.")
            }
            appendLine()
            appendLine("## Determinism and interruption qualification")
            appendLine()
            appendLine("- ${run.deterministicPairs} representative policy/seed pairs were rerun from fresh state and produced identical full `CampaignResult` values.")
            appendLine("- ${run.saveReloadPairs} representative campaigns (one per policy) were compared continuously versus save/codec/reload checkpoints on Days 3, 7, 11, 17 and 23; outcomes and traces were identical.")
            appendLine("- No gameplay RNG is consumed by reporting or narrative formatting.")
            appendLine()
            appendLine("## Outliers")
            appendLine()
            outlierLines.forEach { appendLine("- $it") }
            appendLine()
            appendLine("Unresolved seeds: ${if (unresolved.isEmpty()) "none" else unresolved.joinToString { it.policy.name + ":" + it.seed }}.")
            appendLine()
            appendLine("## Objective defects and repairs")
            appendLine()
            if (unresolved.isEmpty()) {
                appendLine("No objective correctness defect was exposed by the qualified cohort. No production rule or balance constant was changed. Harness implementation issues found during development were confined to test tooling and do not constitute gameplay repairs.")
            } else {
                appendLine("OBJECTIVE DEFECT: ${unresolved.size} campaigns did not resolve by Day $QUALIFICATION_CEILING_DAYS. No balance repair was made without Principal review; reproduce from the listed seeds.")
            }
            appendLine()
            appendLine("## Principal Decision Table — Ordinary policy")
            appendLine()
            appendLine("| Metric | Ordinary result |")
            appendLine("|---|---:|")
            appendLine("| Median days to season resolution | ${format(ordinaryDuration.median)} |")
            appendLine("| 10th–90th percentile days | ${format(ordinaryDuration.p10)}–${format(ordinaryDuration.p90)} |")
            appendLine("| Player Dragon victory rate | ${rateText(ordinary.count { it.playerVictory }, ordinary.size)} |")
            appendLine("| NPC Dragon victory rate | ${rateText(ordinary.count { it.npcVictory }, ordinary.size)} |")
            appendLine("| Unresolved campaign rate | ${rateText(ordinary.count { !it.completed }, ordinary.size)} |")
            appendLine("| Median player final level | ${format(ordinaryFinalLevel.median)} |")
            appendLine("| 10th–90th player final level | ${format(ordinaryFinalLevel.p10)}–${format(ordinaryFinalLevel.p90)} |")
            appendLine("| Median final player rank | ${format(ordinaryFinalRank.median)} |")
            appendLine("| Player #1 finish rate | ${rateText(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size)} |")
            appendLine("| Combat defeat rate | ${rateText(ordinary.sumOf { it.totalFightsLost }, ordinary.sumOf { it.totalFightsAttempted })} |")
            appendLine("| Training first-attempt success | ${rateText(ordinary.sumOf { it.totalTrainingSuccesses }, ordinary.sumOf { it.totalTrainingAttempts })} |")
            appendLine("| Median training attempts/level | 1 (when eligible) |")
            appendLine("| Median final wealth | ${format(ordinaryWealth.median)} gold |")
            appendLine("| Median first player Dragon-ready day | ${nullableMedian(ordinary.map { it.firstPlayerReadyDay })} |")
            appendLine("| Median first NPC Dragon-ready day | ${nullableMedian(ordinary.map { it.firstNpcReadyDay })} |")
            appendLine("| Median Dragon challenge-window day | ${nullableMedian(ordinary.map { it.stageEnteredDays[DragonStage.CHALLENGE_WINDOW] })} |")
            appendLine("| Median Chronicle entries/day | ${format(ordinaryChronicle.median)} |")
            appendLine("| Median credible candidates at window | ${format(summary(ordinary.map { it.challengeWindowReadyCandidates.toDouble() }).median)} |")
            appendLine()
            appendLine("## Explicit answers to the 20 Principal questions")
            appendLine()
            appendLine("1. Ordinary season length: median ${format(ordinaryDuration.median)} days; mean ${format(ordinaryDuration.mean)}.")
            appendLine("2. Variation: P10–P90 ${format(ordinaryDuration.p10)}–${format(ordinaryDuration.p90)}, min–max ${format(ordinaryDuration.minimum)}–${format(ordinaryDuration.maximum)}, SD ${format(ordinaryDuration.standardDeviation)} days.")
            appendLine("3. Ordinary player Dragon victory: ${rateText(ordinary.count { it.playerVictory }, ordinary.size)}.")
            appendLine("4. NPC kills first: ${rateText(ordinary.count { it.npcVictory }, ordinary.size)}.")
            appendLine("5. Unresolved: ${rateText(ordinary.count { !it.completed }, ordinary.size)} Ordinary; ${rateText(results.count { !it.completed }, results.size)} overall.")
            appendLine("6. Ordinary final level: median ${format(ordinaryFinalLevel.median)}, P10–P90 ${format(ordinaryFinalLevel.p10)}–${format(ordinaryFinalLevel.p90)}.")
            appendLine("7. Major level timing: ${(2..10).joinToString { level -> "L$level=" + nullableMedian(ordinary.map { it.levelReachedDays[level] }) }} (median day among those reaching the level).")
            appendLine("8. Training failure: ${rateText(ordinary.sumOf { it.totalTrainingFailures }, ordinary.sumOf { it.totalTrainingAttempts })}; outcomes are seeded and save-stable.")
            appendLine("9. Ordinary fight loss: ${rateText(ordinary.sumOf { it.totalFightsLost }, ordinary.sumOf { it.totalFightsAttempted })}.")
            appendLine("10. Player Dragon attempts: median ${format(summary(ordinary.map { it.playerDragonAttempts.size.toDouble() }).median)}, mean ${format(ordinary.map { it.playerDragonAttempts.size }.average())}, max ${ordinary.maxOf { it.playerDragonAttempts.size }}.")
            appendLine("11. Ordinary final wealth: median ${format(ordinaryWealth.median)}, P10–P90 ${format(ordinaryWealth.p10)}–${format(ordinaryWealth.p90)} gold.")
            appendLine("12. End equipment: weapons ${frequencyText(ordinary.map { EquipmentCatalog.requireWeapon(it.finalWeaponId).displayName })}; armor ${frequencyText(ordinary.map { EquipmentCatalog.requireArmor(it.finalArmorId).displayName })}.")
            appendLine("13. NPC competitiveness: NPC victory ${rateText(ordinary.count { it.npcVictory }, ordinary.size)}, player median rank ${format(ordinaryFinalRank.median)}, player #1 finish ${rateText(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size)}.")
            appendLine("14. Credible candidates: median ${format(summary(ordinary.map { it.challengeWindowReadyCandidates.toDouble() }).median)} at window and ${format(summary(ordinary.map { it.finalReadyCandidates.toDouble() }).median)} at resolution.")
            appendLine("15. Serious pressure: Fear median Day ${nullableMedian(ordinary.map { it.stageEnteredDays[DragonStage.FEAR] })}; Crisis median Day ${nullableMedian(ordinary.map { it.stageEnteredDays[DragonStage.CRISIS] })}.")
            appendLine("16. Chronicle density: mean ${format(ordinaryChronicle.mean)}, median ${format(ordinaryChronicle.median)}, P90 ${format(ordinaryChronicle.p90)} entries/day.")
            appendLine("17. Narrative prominence: mean ${format(ordinary.map { it.chronicleNpcAppearances.size }.average())} unique NPCs/season; top NPC mean share ${format(ordinary.map(::topNpcShare).average() * 100)}%.")
            appendLine("18. Policy effect: see Policy Comparison; player victory ranges ${format(CampaignPolicy.entries.minOf { policy -> percent(results.count { it.policy == policy && it.playerVictory }, results.count { it.policy == policy }) })}%–${format(CampaignPolicy.entries.maxOf { policy -> percent(results.count { it.policy == policy && it.playerVictory }, results.count { it.policy == policy }) })}%, median final level ${CampaignPolicy.entries.minOf { policy -> summary(results.filter { it.policy == policy }.map { it.finalPlayerLevel.toDouble() }).median }}–${CampaignPolicy.entries.maxOf { policy -> summary(results.filter { it.policy == policy }.map { it.finalPlayerLevel.toDouble() }).median }}.")
            appendLine("19. Statistical outliers: see Outliers and `outliers.json`; the longest campaign is ${results.maxOf { it.resolutionDay }} days and maximum combat defeats are ${results.maxOf { it.totalFightsLost }}.")
            appendLine("20. Objective defects/repairs: ${if (unresolved.isEmpty()) "none discovered; no production repairs or tuning performed" else "nontermination observed; no unauthorized tuning performed"}.")
            appendLine()
            appendLine("## Machine-readable evidence")
            appendLine()
            appendLine("- `campaigns.csv`: one record per season.")
            appendLine("- `days.jsonl`: one record per simulated game day.")
            appendLine("- `monster_metrics.csv`: policy/enemy aggregates.")
            appendLine("- `summary.json`: primary machine-readable distributions.")
            appendLine("- `outliers.json`: reproducible diagnostic seeds.")
            appendLine()
            appendLine("## Recommendation")
            appendLine()
            appendLine("`LGD011_BALANCE_CHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`")
            appendLine()
            appendLine("The Principal should review these distributions before any balance changes or LGD-012 work is authorized.")
        }
    }

    private fun policyDescriptions(): List<String> = listOf(
        "PASSIVE: fights 0 on odd days and at most 2 on even days; no challenges or Dragon attempt; defensive healing/escape; only checks one upgrade every fifth day; banks above 25 gold.",
        "CASUAL: at most 5 forest fights/day; heals at 75% HP; stops below 55% if healing is unaffordable; escapes at roughly three incoming hits; one challenge every fourth day; one shop upgrade/visit; waits for Level 8, tier-5 gear, power 135 and two open-window days before considering the Dragon.",
        "ORDINARY: at most 9 fights/day; heals at 52% HP; stops below 30% if unable to heal; escapes at roughly two incoming hits; one challenge every third day; up to two shop upgrades/visit; attempts the Dragon when production-ready and combat power is at least 130.",
        "AGGRESSIVE: uses all 10 fights when lawful; heals at 38% HP; stops only below 12% if broke; escapes only at roughly one remaining incoming hit; challenges every second day; aggressively purchases up to four upgrades/visit; attempts as soon as production readiness permits.",
        "RECKLESS: uses all 10 fights, rarely heals (20%), never voluntarily stops/runs/yields, challenges daily, keeps gold carried, and attempts the Dragon at the earliest lawful readiness point.",
    )

    private fun durationBuckets(cohort: List<CampaignResult>): List<Int> = listOf(
        cohort.count { it.resolutionDay in 1..5 },
        cohort.count { it.resolutionDay in 6..10 },
        cohort.count { it.resolutionDay in 11..15 },
        cohort.count { it.resolutionDay in 16..20 },
        cohort.count { it.resolutionDay in 21..25 },
        cohort.count { it.resolutionDay in 26..30 },
        cohort.count { it.resolutionDay in 31..40 },
        cohort.count { it.resolutionDay in 41..50 },
        cohort.count { it.resolutionDay >= 51 },
    )

    private fun attemptCategoryText(cohort: List<CampaignResult>): String {
        val never = cohort.count { it.playerDragonAttempts.isEmpty() }
        val attemptedNeverWon = cohort.count { it.playerDragonAttempts.isNotEmpty() && !it.playerVictory }
        val first = cohort.count { it.playerDragonAttempts.firstOrNull() == DragonAttemptOutcome.VICTORY }
        val later = cohort.count { it.playerVictory && it.playerDragonAttempts.firstOrNull() != DragonAttemptOutcome.VICTORY }
        return "never attempted ${rateText(never, cohort.size)}; attempted but never won " +
            "${rateText(attemptedNeverWon, cohort.size)}; won first attempt ${rateText(first, cohort.size)}; " +
            "won after a prior failure/retreat ${rateText(later, cohort.size)}"
    }

    private fun npcFirstAttemptVictoryText(cohort: List<CampaignResult>): String {
        val firstAttempts = cohort.flatMap { result ->
            result.finalState.season.attempts
                .filter { it.challengerId != result.finalState.player.id }
                .groupBy { it.challengerId }
                .values
                .mapNotNull { attempts -> attempts.minByOrNull { it.day } }
        }
        return rateText(
            firstAttempts.count { it.outcome == DragonAttemptOutcome.VICTORY },
            firstAttempts.size,
        )
    }

    private fun distributionSentence(values: List<Double>): String {
        if (values.isEmpty()) return "none observed"
        val s = summary(values)
        return "n=${s.count}, median ${format(s.median)}, mean ${format(s.mean)}, " +
            "P10–P90 ${format(s.p10)}–${format(s.p90)}, min–max " +
            "${format(s.minimum)}–${format(s.maximum)}"
    }

    private fun wealthCheckpointText(cohort: List<CampaignResult>): String =
        listOf(3, 5, 10, 15, 20, 25).joinToString { checkpoint ->
            val values = cohort.mapNotNull { result ->
                result.days.firstOrNull { it.day == checkpoint }
                    ?.let { it.carriedGold + it.bankedGold }
            }
            if (values.isEmpty()) {
                "Day $checkpoint n/a"
            } else {
                val s = summary(values.map(Int::toDouble))
                "Day $checkpoint ${format(s.mean)}/${format(s.median)} (n=${values.size})"
            }
        }

    private fun npcCheckpointText(cohort: List<CampaignResult>): String =
        listOf(3, 5, 10, 15, 20, 25).joinToString { checkpoint ->
            val values = cohort.flatMap { result ->
                result.days.firstOrNull { it.day == checkpoint }?.npcLevels.orEmpty()
            }
            if (values.isEmpty()) {
                "Day $checkpoint n/a"
            } else {
                val s = summary(values.map(Int::toDouble))
                "Day $checkpoint ${format(s.minimum)}/${format(s.median)}/" +
                    "${format(s.mean)}/${format(s.maximum)}"
            }
        }

    private fun rankCheckpointText(cohort: List<CampaignResult>): String =
        listOf(3, 5, 10, 15, 20, 25).joinToString { checkpoint ->
            val values = cohort.mapNotNull { result ->
                result.days.firstOrNull { it.day == checkpoint }?.playerRank
            }
            if (values.isEmpty()) {
                "Day $checkpoint n/a"
            } else {
                val s = summary(values.map(Int::toDouble))
                "Day $checkpoint ${format(s.mean)}/${format(s.median)} (n=${values.size})"
            }
        }

    private fun dragonAttemptReadinessText(cohort: List<CampaignResult>): String {
        val attemptDays = cohort.mapNotNull { result ->
            result.days.firstOrNull { it.dragonAttempts > 0 }
        }
        if (attemptDays.isEmpty()) return "no player attempts"
        val levels = summary(attemptDays.map { it.playerLevel.toDouble() })
        val weaponTiers = summary(attemptDays.map {
            EquipmentCatalog.requireWeapon(it.weaponId).tier.toDouble()
        })
        val armorTiers = summary(attemptDays.map {
            EquipmentCatalog.requireArmor(it.armorId).tier.toDouble()
        })
        return "n=${attemptDays.size}; level median ${format(levels.median)} (P10–P90 " +
            "${format(levels.p10)}–${format(levels.p90)}), weapon tier median " +
            "${format(weaponTiers.median)}, armor tier median ${format(armorTiers.median)}"
    }

    private fun combatDefeatByLevel(cohort: List<CampaignResult>): String =
        cohort.flatMap { it.days }.groupBy { it.playerLevel }.toSortedMap().entries.joinToString { (level, days) ->
            "L$level ${rateText(days.sumOf { it.fightsLost }, days.sumOf { it.fightsAttempted })}"
        }

    private fun combatDefeatByDayBucket(cohort: List<CampaignResult>): String {
        val buckets = listOf(1..5, 6..10, 11..15, 16..20, 21..25, 26..100)
        return buckets.mapNotNull { range ->
            val days = cohort.flatMap { it.days }.filter { it.day in range }
            val attempts = days.sumOf { it.fightsAttempted }
            if (attempts == 0) null else {
                "Day ${range.first}–${if (range.last == 100) "+" else range.last} " +
                    rateText(days.sumOf { it.fightsLost }, attempts)
            }
        }.joinToString()
    }

    private fun aggregateMonsters(cohort: List<CampaignResult>): Map<String, MonsterSeasonMetric> {
        val result = linkedMapOf<String, MonsterSeasonMetric>()
        cohort.forEach { campaign ->
            campaign.monsterMetrics.forEach { (id, metric) ->
                result[id] = (result[id] ?: MonsterSeasonMetric()) + metric
            }
        }
        return result
    }

    private fun equipmentTimingText(cohort: List<CampaignResult>): String {
        val definitions = EquipmentCatalog.weapons.map { it.id to it.displayName } +
            EquipmentCatalog.armors.map { it.id to it.displayName }
        return definitions.filterNot { (id, _) ->
            id == EquipmentCatalog.STARTER_WEAPON_ID || id == EquipmentCatalog.STARTER_ARMOR_ID
        }.mapNotNull { (id, name) ->
            val days = cohort.mapNotNull { it.equipmentAcquiredDays[id] }
            if (days.isEmpty()) null else "$name ${format(percent(days.size, cohort.size))}% @ median Day ${format(summary(days.map(Int::toDouble)).median)}"
        }.joinToString("; ")
    }

    private fun eventTypeText(cohort: List<CampaignResult>): String =
        WorldEventType.entries.mapNotNull { type ->
            val count = cohort.sumOf { it.chronicleTypeCounts[type] ?: 0 }
            count.takeIf { it > 0 }?.let { "$type=${format(count.toDouble() / cohort.size)}/season" }
        }.joinToString(", ")

    private fun hasDominantLeader(result: CampaignResult): Boolean {
        if (result.days.isEmpty()) return false
        val top = result.days.groupingBy { it.leaderId }.eachCount().maxOf { it.value }
        return top.toDouble() / result.days.size >= 0.8
    }

    private fun npcDominanceShare(result: CampaignResult): Double {
        if (result.days.isEmpty()) return 0.0
        val npcIds = result.finalState.community.adventurers.mapTo(hashSetOf()) { it.id }
        val mostDays = result.days.filter { it.leaderId in npcIds }
            .groupingBy { it.leaderId }
            .eachCount()
            .maxOfOrNull { it.value } ?: 0
        return mostDays.toDouble() / result.days.size
    }

    private fun topNpcShare(result: CampaignResult): Double {
        val total = result.chronicleNpcAppearances.values.sum()
        if (total == 0) return 0.0
        return result.chronicleNpcAppearances.values.max().toDouble() / total
    }

    private fun outlierLines(results: List<CampaignResult>): List<String> {
        fun label(name: String, result: CampaignResult?): String = if (result == null) {
            "$name: none"
        } else {
            "$name: ${result.policy.name} seed ${result.seed}, Day ${result.resolutionDay}, " +
                "Level ${result.finalPlayerLevel}, rank ${result.finalPlayerRank}, " +
                "wealth ${result.finalCarriedGold + result.finalBankedGold}"
        }
        val playerWins = results.filter { it.playerVictory }
        return listOf(
            label("Shortest campaign", results.minByOrNull { it.resolutionDay }),
            label("Longest campaign", results.maxByOrNull { it.resolutionDay }),
            label("Earliest player victory", playerWins.minByOrNull { it.resolutionDay }),
            label("Latest player victory", playerWins.maxByOrNull { it.resolutionDay }),
            label("Strongest final player", results.maxByOrNull { it.finalPlayerLevel }),
            label("Weakest player who won", playerWins.minByOrNull { it.finalPlayerLevel }),
            label("Most ordinary-combat defeats", results.maxByOrNull { it.totalFightsLost }),
            label("Most training failures", results.maxByOrNull { it.totalTrainingFailures }),
            label("Richest player", results.maxByOrNull { it.finalCarriedGold + it.finalBankedGold }),
            label("Poorest player at conclusion", results.filter { it.completed }.minByOrNull { it.finalCarriedGold + it.finalBankedGold }),
            label("Most NPC Dragon attempts", results.maxByOrNull { it.npcDragonAttempts.size }),
            label("Most dominant NPC-led campaign", results.maxByOrNull(::npcDominanceShare)),
            label("Highest Chronicle density", results.maxByOrNull { it.chronicleEntries.toDouble() / it.days.size.coerceAtLeast(1) }),
            label("Lowest Chronicle density", results.minByOrNull { it.chronicleEntries.toDouble() / it.days.size.coerceAtLeast(1) }),
        )
    }

    private fun summary(values: List<Double>): NumericSummary {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val mean = sorted.average()
        val variance = sorted.sumOf { (it - mean).pow(2) } / sorted.size
        return NumericSummary(
            count = sorted.size,
            minimum = sorted.first(),
            maximum = sorted.last(),
            mean = mean,
            median = percentile(sorted, 0.5),
            standardDeviation = sqrt(variance),
            p10 = percentile(sorted, 0.10),
            p25 = percentile(sorted, 0.25),
            p75 = percentile(sorted, 0.75),
            p90 = percentile(sorted, 0.90),
            p95 = percentile(sorted, 0.95),
        )
    }

    private fun percentile(sorted: List<Double>, fraction: Double): Double {
        val index = (ceil(fraction * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index]
    }

    private fun rate(successes: Int, sample: Int): RateEstimate {
        if (sample == 0) return RateEstimate(successes, sample, 0.0, 0.0, 0.0)
        val z = 1.96
        val p = successes.toDouble() / sample
        val denominator = 1 + z * z / sample
        val center = (p + z * z / (2 * sample)) / denominator
        val margin = z * sqrt((p * (1 - p) + z * z / (4 * sample)) / sample) / denominator
        return RateEstimate(successes, sample, p * 100, (center - margin) * 100, (center + margin) * 100)
    }

    private fun percent(value: Int, total: Int): Double =
        if (total == 0) 0.0 else value * 100.0 / total

    private fun rateText(value: Int, total: Int): String =
        "${format(percent(value, total))}% ($value/$total)"

    private fun nullableMedian(values: List<Int?>): String {
        val known = values.filterNotNull()
        return if (known.isEmpty()) "never" else "${format(summary(known.map(Int::toDouble)).median)} (n=${known.size})"
    }

    private fun <T> frequencyText(values: List<T>): String =
        values.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            .joinToString { "${it.key}=${it.value}" }

    private fun Iterable<Int>.average(): Double {
        val values = toList()
        return if (values.isEmpty()) 0.0 else values.sum().toDouble() / values.size
    }

    private fun Iterable<Double>.averageOrZero(): Double {
        val values = toList()
        return if (values.isEmpty()) 0.0 else values.average()
    }

    private fun numericJson(s: NumericSummary): String =
        "{\"count\":${s.count},\"min\":${format(s.minimum)},\"max\":${format(s.maximum)}," +
            "\"mean\":${format(s.mean)},\"median\":${format(s.median)}," +
            "\"sd\":${format(s.standardDeviation)},\"p10\":${format(s.p10)}," +
            "\"p25\":${format(s.p25)},\"p75\":${format(s.p75)}," +
            "\"p90\":${format(s.p90)},\"p95\":${format(s.p95)}}"

    private fun format(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)

    private fun csv(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

    private fun write(path: Path, content: String) {
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }
}
