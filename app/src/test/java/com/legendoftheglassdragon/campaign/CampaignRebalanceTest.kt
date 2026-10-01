package com.legendoftheglassdragon.campaign

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignRebalanceTest {
    @Test
    fun generateLgd011bRebalanceEvidence() {
        val seasons = System.getProperty("lgd011b.seasons")?.toIntOrNull() ?: 25
        val output = System.getProperty("lgd011b.output")?.let(Path::of)
            ?: Path.of("build", "reports", "lgd011b-sample")
        val run = CampaignCharacterizationRunner.run(seasons)
        val qualification = CampaignRebalanceReporter.qualify(
            results = run.results,
            requireFullCoverage = seasons >= 2_500,
        )
        CampaignRebalanceReporter.writeEvidence(run, qualification, output)

        assertEquals(seasons, run.results.size)
        assertTrue(run.results.all { it.completed })
        assertEquals(10, qualification.deterministicCases.size)
        assertEquals(5, qualification.saveReloadCases.size)
        listOf(
            "lgd011b_campaigns.csv",
            "lgd011b_days.jsonl",
            "lgd011b_dragon_attempts.csv",
            "lgd011b_training.csv",
            "lgd011b_summary.json",
            "lgd011b_outliers.json",
        ).forEach { filename ->
            assertTrue(output.resolve(filename).exists())
            assertTrue(output.resolve(filename).fileSize() > 50)
        }
    }
}
