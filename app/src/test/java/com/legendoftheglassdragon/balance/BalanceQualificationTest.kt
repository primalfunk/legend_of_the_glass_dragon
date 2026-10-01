package com.legendoftheglassdragon.balance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceQualificationTest {
    @Test
    fun threePolicyCohortsMeetOneTwelveAndThirtyDayPacingBounds() {
        val seedCount = 128
        val summaries = PlayPolicy.entries.associateWith { policy ->
            val traces = (1..seedCount).map { seed ->
                BalanceHarness.runCampaign(seed.toLong(), policy, realmDays = 30)
            }
            val summary = BalanceHarness.summarize(traces)
            println(summary)

            assertEquals(seedCount, summary.seedCount)
            assertEquals(30, summary.realmDays)
            assertTrue(summary.maximumConsecutiveDefeatedDays <= 8)
            assertTrue(traces.all { trace ->
                trace.finalState.player.gold >= 0 &&
                    trace.finalState.player.experience >= 0 &&
                    trace.days.all { day -> day.endingHp in 1..trace.finalState.player.maximumHp }
            })
            summary
        }

        val cautious = summaries.getValue(PlayPolicy.CAUTIOUS)
        val ordinary = summaries.getValue(PlayPolicy.ORDINARY)
        val aggressive = summaries.getValue(PlayPolicy.AGGRESSIVE)

        assertTrue(ordinary.dayOneMedianFightsWon >= 6)
        assertTrue(ordinary.dayOneMedianExperience >= 50)
        // A lawful training trial now has bounded seeded failure; most, not all, Day-1
        // candidates should promote immediately.
        assertTrue(ordinary.dayOneLevelTwoRatePercent >= 70.0)
        assertEquals(1, ordinary.levelTwoMedianDay)
        assertTrue(ordinary.levelTwoP90Day <= 2)

        assertTrue(cautious.defeatDayRatePercent < ordinary.defeatDayRatePercent)
        assertTrue(ordinary.defeatDayRatePercent in 10.0..20.0)
        assertTrue(aggressive.defeatDayRatePercent > ordinary.defeatDayRatePercent)
        assertTrue(aggressive.defeatDayRatePercent < 35.0)

        assertTrue(ordinary.medianDayByLevel.getValue(3) in 2..4)
        assertTrue(ordinary.medianDayByLevel.getValue(4) in 4..6)
        assertTrue(ordinary.medianDayByLevel.getValue(5) in 7..9)
        assertTrue(ordinary.medianDayByLevel.getValue(6) in 10..13)
        assertTrue(ordinary.medianDayByLevel.getValue(7) in 15..18)
        assertTrue(ordinary.medianDayByLevel.getValue(8) in 20..23)
        assertTrue(ordinary.medianDayByLevel.getValue(9) in 25..29)
        assertTrue(ordinary.medianDayByLevel.getValue(10) > 30)

        val ordinaryMiddleLevels = (5..6).sumOf {
            ordinary.dayTwelveLevelDistribution[it] ?: 0
        }
        assertTrue(ordinaryMiddleLevels >= seedCount * 9 / 10)

        val ordinaryLateLevels = (8..10).sumOf {
            ordinary.finalLevelDistribution[it] ?: 0
        }
        assertTrue(ordinaryLateLevels >= seedCount * 9 / 10)
        assertTrue((ordinary.finalLevelDistribution[10] ?: 0) <= seedCount / 10)
        assertTrue(ordinary.finalGoldMedian in 500..6_000)
    }

    @Test
    fun representativeOrdinaryTraceHasBeginningMiddleAndLateProgression() {
        val trace = BalanceHarness.runCampaign(
            seed = 42L,
            policy = PlayPolicy.ORDINARY,
            realmDays = 30,
        )
        trace.days.forEach { println(it) }
        val selectedDays = listOf(1, 12, 30).map { day -> trace.days[day - 1] }

        assertTrue(selectedDays[0].fightsWon >= 5)
        assertTrue(selectedDays[0].endingExperience > 0)
        assertTrue(selectedDays[1].endingLevel in 5..6)
        assertTrue(selectedDays[2].endingLevel in 8..10)
        assertTrue(trace.totalPromotions >= 7)
        assertTrue(trace.maximumConsecutiveDefeatedDays <= 4)
    }
}
