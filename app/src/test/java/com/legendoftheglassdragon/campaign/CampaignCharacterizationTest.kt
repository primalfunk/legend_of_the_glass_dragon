package com.legendoftheglassdragon.campaign

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignCharacterizationTest {
    @Test
    fun generateDeterministicMachineReadableCharacterization() {
        val seasons = System.getenv("LGD011_SEASONS")?.toIntOrNull() ?: 25
        val output = System.getenv("LGD011_OUTPUT_DIR")?.let(Path::of)
            ?: Path.of("build", "reports", "lgd011-sample")
        val run = CampaignCharacterizationRunner.run(seasons)
        CampaignCharacterizationRunner.writeEvidence(run, output)

        assertEquals(seasons, run.results.size)
        assertEquals(seasons, run.results.map { it.policy to it.seed }.distinct().size)
        assertTrue(run.results.all { it.resolutionDay in 1..100 })
        assertTrue(output.resolve("campaigns.csv").exists())
        assertTrue(output.resolve("campaigns.csv").fileSize() > 100)
        assertTrue(output.resolve("days.jsonl").exists())
        assertTrue(output.resolve("summary.json").exists())
        assertTrue(output.resolve("outliers.json").exists())
        assertTrue(output.resolve("LGD-011_BALANCE_CHARACTERIZATION.md").exists())
    }
}
