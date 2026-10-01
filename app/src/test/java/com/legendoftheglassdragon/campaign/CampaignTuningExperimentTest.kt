package com.legendoftheglassdragon.campaign

import com.legendoftheglassdragon.game.DragonAttemptOutcome
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.ceil
import kotlin.system.measureNanoTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignTuningExperimentTest {
    @Test
    fun runDocumentedLgd011bExploratoryCohort() {
        val output = System.getProperty("lgd011b.output")?.let(Path::of)
            ?: Path.of("build", "reports", "lgd011b-smoke")
        val round = System.getProperty("lgd011b.round") ?: "smoke"
        val counts = if (round == "smoke") {
            CampaignPolicy.entries.associateWith { 5 }
        } else {
            mapOf(
                CampaignPolicy.PASSIVE to 25,
                CampaignPolicy.CASUAL to 25,
                CampaignPolicy.ORDINARY to 400,
                CampaignPolicy.AGGRESSIVE to 25,
                CampaignPolicy.RECKLESS to 25,
            )
        }
        val results = mutableListOf<CampaignResult>()
        val elapsed = measureNanoTime {
            counts.forEach { (policy, count) ->
                repeat(count) { index ->
                    val seed = (policy.ordinal + 1L) * 1_000_000L + index + 1L
                    results += CampaignSimulationHarness().run(seed, policy)
                }
            }
        }
        Files.createDirectories(output)
        Files.writeString(
            output.resolve("summary.json"),
            exploratoryJson(round, results, elapsed / 1_000_000_000.0),
            StandardCharsets.UTF_8,
        )
        Files.writeString(
            output.resolve("dragon_attempts.csv"),
            dragonAttemptsCsv(results),
            StandardCharsets.UTF_8,
        )
        Files.writeString(
            output.resolve("campaigns.csv"),
            campaignsCsv(results),
            StandardCharsets.UTF_8,
        )

        assertEquals(counts.values.sum(), results.size)
        assertTrue(results.all { it.completed })
    }

    private fun exploratoryJson(round: String, results: List<CampaignResult>, seconds: Double): String {
        val ordinary = results.filter { it.policy == CampaignPolicy.ORDINARY }
        val ordinaryDays = ordinary.flatMap { it.days }
        val playerAttempts = ordinary.flatMap { result ->
            result.dragonAttemptMetrics.filter { it.challengerType == "PLAYER" }
        }
        val npcAttempts = ordinary.flatMap { result ->
            result.dragonAttemptMetrics.filter { it.challengerType == "NPC" }
        }
        val candidateCounts = ordinary.map {
            maxOf(it.challengeWindowReadyCandidates, it.finalReadyCandidates).coerceAtLeast(0)
        }
        return buildString {
            appendLine("{")
            appendLine("  \"round\": \"$round\",")
            appendLine("  \"seasons\": ${results.size},")
            appendLine("  \"game_days\": ${results.sumOf { it.days.size }},")
            appendLine("  \"seconds\": ${f(seconds)},")
            appendLine("  \"ordinary\": {")
            appendLine("    \"count\": ${ordinary.size},")
            appendLine("    \"median_days\": ${median(ordinary.map { it.resolutionDay })},")
            appendLine("    \"p10_days\": ${percentile(ordinary.map { it.resolutionDay }, 0.10)},")
            appendLine("    \"p90_days\": ${percentile(ordinary.map { it.resolutionDay }, 0.90)},")
            appendLine("    \"player_victory_percent\": ${f(pct(ordinary.count { it.playerVictory }, ordinary.size))},")
            appendLine("    \"npc_victory_percent\": ${f(pct(ordinary.count { it.npcVictory }, ordinary.size))},")
            appendLine("    \"player_rank_one_percent\": ${f(pct(ordinary.count { it.finalPlayerRank == 1 }, ordinary.size))},")
            appendLine("    \"player_top_three_percent\": ${f(pct(ordinary.count { it.finalPlayerRank <= 3 }, ordinary.size))},")
            appendLine("    \"median_candidates\": ${median(candidateCounts)},")
            appendLine("    \"first_attempt_win_percent\": ${f(pct(playerAttempts.count { it.attemptNumber == 1 && it.outcome == DragonAttemptOutcome.VICTORY }, playerAttempts.count { it.attemptNumber == 1 }))},")
            appendLine("    \"all_player_attempt_win_percent\": ${f(pct(playerAttempts.count { it.outcome == DragonAttemptOutcome.VICTORY }, playerAttempts.size))},")
            appendLine("    \"mean_player_attempts\": ${f(ordinary.map { it.playerDragonAttempts.size }.average())},")
            appendLine("    \"training_first_attempt_percent\": ${f(trainingFirstAttemptPercent(ordinary))},")
            appendLine("    \"combat_defeat_percent\": ${f(pct(ordinary.sumOf { it.totalFightsLost }, ordinary.sumOf { it.totalFightsAttempted }))},")
            appendLine("    \"chronicle_mean\": ${f(ordinaryDays.map { it.chronicleEntries }.average())},")
            appendLine("    \"chronicle_median\": ${median(ordinaryDays.map { it.chronicleEntries })},")
            appendLine("    \"callbacks_per_season\": ${f(ordinary.map { it.callbacks }.average())},")
            appendLine("    \"final_level_frequency\": ${frequencyJson(ordinary.map { it.finalPlayerLevel })},")
            appendLine("    \"final_rank_frequency\": ${frequencyJson(ordinary.map { it.finalPlayerRank })}")
            appendLine("  },")
            appendLine("  \"player_attempts\": ${attemptSummary(playerAttempts)},")
            appendLine("  \"npc_attempts\": ${attemptSummary(npcAttempts)}")
            appendLine("}")
        }
    }

    private fun attemptSummary(attempts: List<DragonAttemptMetric>): String {
        val first = attempts.filter { it.attemptNumber == 1 }
        return buildString {
            append("{")
            append("\"count\":${attempts.size},")
            append("\"mean_level\":${f(attempts.map { it.level }.average())},")
            append("\"median_level\":${median(attempts.map { it.level })},")
            append("\"mean_attack\":${f(attempts.map { it.attack }.average())},")
            append("\"mean_defense\":${f(attempts.map { it.defense }.average())},")
            append("\"mean_hp\":${f(attempts.map { it.maximumHp }.average())},")
            append("\"mean_power\":${f(attempts.map { it.combatPower }.average())},")
            append("\"median_first_attempt_day\":${median(first.map { it.day })},")
            append("\"first_success_percent\":${f(pct(first.count { it.outcome == DragonAttemptOutcome.VICTORY }, first.size))},")
            append("\"all_success_percent\":${f(pct(attempts.count { it.outcome == DragonAttemptOutcome.VICTORY }, attempts.size))},")
            append("\"tiers\":${frequencyJson(attempts.map { it.readinessTier.name })},")
            append("\"weapons\":${frequencyJson(attempts.map { it.weaponId })},")
            append("\"armor\":${frequencyJson(attempts.map { it.armorId })}")
            append("}")
        }
    }

    private fun dragonAttemptsCsv(results: List<CampaignResult>) = buildString {
        appendLine("seed,policy,challenger_type,challenger_id,day,level,rank,weapon_id,armor_id,attack,defense,maximum_hp,current_hp,readiness_tier,combat_power,dragon_power,attempt_number,outcome")
        results.forEach { result ->
            result.dragonAttemptMetrics.forEach { attempt ->
                appendLine(
                    listOf(
                        result.seed, result.policy.name, attempt.challengerType, attempt.challengerId,
                        attempt.day, attempt.level, attempt.rank, attempt.weaponId, attempt.armorId,
                        attempt.attack, attempt.defense, attempt.maximumHp, attempt.currentHp,
                        attempt.readinessTier.name, attempt.combatPower, attempt.dragonPower,
                        attempt.attemptNumber, attempt.outcome.name,
                    ).joinToString(","),
                )
            }
        }
    }

    private fun campaignsCsv(results: List<CampaignResult>) = buildString {
        appendLine("seed,policy,days,player_victory,final_level,final_rank,final_candidates,callbacks,chronicle_entries,player_attempts,npc_attempts")
        results.forEach { result ->
            appendLine(
                listOf(
                    result.seed, result.policy.name, result.resolutionDay, result.playerVictory,
                    result.finalPlayerLevel, result.finalPlayerRank,
                    maxOf(result.challengeWindowReadyCandidates, result.finalReadyCandidates),
                    result.callbacks, result.chronicleEntries, result.playerDragonAttempts.size,
                    result.npcDragonAttempts.size,
                ).joinToString(","),
            )
        }
    }

    private fun trainingFirstAttemptPercent(results: List<CampaignResult>): Double {
        val episodes = results.flatMap { result ->
            result.trainingAttemptsByTargetLevel.map { (level, attempts) ->
                attempts to (result.trainingFailuresByTargetLevel[level] ?: 0)
            }
        }
        return pct(episodes.count { (attempts, failures) -> attempts > 0 && failures == 0 }, episodes.size)
    }

    private fun <T : Comparable<T>> frequencyJson(values: List<T>): String =
        values.groupingBy { it }.eachCount().entries.sortedBy { it.key }
            .joinToString(prefix = "{", postfix = "}") { (key, count) -> "\"$key\":$count" }

    private fun median(values: List<Int>): Int = percentile(values, 0.50)
    private fun percentile(values: List<Int>, probability: Double): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sorted()
        return sorted[(ceil(probability * sorted.size).toInt() - 1).coerceIn(sorted.indices)]
    }

    private fun pct(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else numerator * 100.0 / denominator

    private fun f(value: Double): String = String.format(Locale.US, "%.2f", value)
}
