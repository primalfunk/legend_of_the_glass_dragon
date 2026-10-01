package com.legendoftheglassdragon.campaign

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignRecharacterizationTest {
    @Test
    fun generateLgd011aRecharacterizationEvidence() {
        val seasons = System.getProperty("lgd011a.seasons")?.toIntOrNull()
            ?: System.getenv("LGD011A_SEASONS")?.toIntOrNull()
            ?: 25
        val output = System.getProperty("lgd011a.output")?.let(Path::of)
            ?: System.getenv("LGD011A_OUTPUT_DIR")?.let(Path::of)
            ?: Path.of("build", "reports", "lgd011a-sample")
        val run = CampaignCharacterizationRunner.run(seasons)
        val qualification = CampaignRecharacterizationReporter.qualifySaveReload(run.results)
        CampaignRecharacterizationReporter.writeEvidence(run, qualification, output)

        assertEquals(seasons, run.results.size)
        assertTrue(run.results.all { it.completed })
        assertTrue(run.results.any { it.totalTrainingFailures > 0 })
        assertTrue(run.results.any { it.npcVictory })
        assertEquals(5, qualification.saveReloadPairs)
        listOf(
            "lgd011a_campaigns.csv",
            "lgd011a_days.jsonl",
            "lgd011a_training.csv",
            "lgd011a_summary.json",
            "lgd011a_outliers.json",
            "LGD-011A_TARGETED_BALANCE_RECHARACTERIZATION.md",
        ).forEach { filename ->
            assertTrue(output.resolve(filename).exists())
            assertTrue(output.resolve(filename).fileSize() > 50)
        }
    }
}
