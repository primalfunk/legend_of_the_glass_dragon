package com.legendoftheglassdragon.campaign

import com.legendoftheglassdragon.data.DecodeResult
import com.legendoftheglassdragon.data.GameStateCodec
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignSimulationHarnessTest {
    @Test
    fun everyTransparentPolicyCompletesLawfullyOnRepresentativeSeeds() {
        CampaignPolicy.entries.forEachIndexed { policyIndex, policy ->
            repeat(3) { offset ->
                val result = CampaignSimulationHarness().run(
                    seed = 10_000L * policyIndex + offset + 1L,
                    policy = policy,
                )
                assertTrue("$policy seed ${result.seed} did not resolve", result.completed)
                assertTrue(result.resolutionDay in 1..100)
                assertTrue(GameStateValidator.validate(result.finalState) is GameStateValidationResult.Valid)
                assertTrue(result.finalCarriedGold >= 0)
                assertTrue(result.finalBankedGold >= 0)
            }
        }
    }

    @Test
    fun identicalSeedAndPolicyProduceIdenticalAuthoritativeTrace() {
        CampaignPolicy.entries.forEachIndexed { index, policy ->
            val seed = 80_001L + index
            val first = CampaignSimulationHarness().run(seed, policy)
            val second = CampaignSimulationHarness().run(seed, policy)
            assertEquals(first, second)
        }
    }

    @Test
    fun saveReloadAtDayBoundariesDoesNotChangeCampaignOutcome() {
        val codec = GameStateCodec()
        val roundTrip: (com.legendoftheglassdragon.game.GameState) ->
            com.legendoftheglassdragon.game.GameState = { state ->
            when (val decoded = codec.decode(codec.encode(state))) {
                is DecodeResult.Success -> decoded.gameState
                is DecodeResult.Failure -> error("Round trip failed: ${decoded.failure}")
            }
        }
        CampaignPolicy.entries.forEachIndexed { index, policy ->
            val seed = 90_001L + index
            val continuous = CampaignSimulationHarness().run(seed, policy)
            val interrupted = CampaignSimulationHarness(
                roundTripDays = setOf(3, 7, 11, 17, 23),
                stateRoundTrip = roundTrip,
            ).run(seed, policy)
            assertEquals(continuous, interrupted)
        }
    }
}
