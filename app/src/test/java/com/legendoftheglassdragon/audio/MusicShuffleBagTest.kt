package com.legendoftheglassdragon.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicShuffleBagTest {
    @Test
    fun catalogContainsExactlyFifteenMusicTracksAndNoClickEffect() {
        assertEquals(15, MusicTrack.entries.size)
        assertEquals(15, MusicTrack.entries.map { it.packagedName }.distinct().size)
        assertTrue(MusicTrack.entries.none { it.packagedName == "click" })
    }

    @Test
    fun eachBagPlaysEveryTrackExactlyOnce() {
        val tracks = (1..15).toList()
        val bag = MusicShuffleBag(tracks) { it.reversed() }

        val cycle = List(15) { bag.next() }

        assertEquals(tracks.toSet(), cycle.toSet())
        assertEquals(15, cycle.distinct().size)
        assertEquals(0, bag.remainingCount())
    }

    @Test
    fun reshuffleBoundaryCannotImmediatelyRepeat() {
        val tracks = listOf(1, 2, 3)
        var cycle = 0
        val bag = MusicShuffleBag(tracks) {
            cycle += 1
            if (cycle == 1) listOf(1, 2, 3) else listOf(3, 2, 1)
        }

        val firstCycle = List(3) { bag.next() }
        val firstOfNextCycle = bag.next()

        assertEquals(3, firstCycle.last())
        assertNotEquals(firstCycle.last(), firstOfNextCycle)
        assertEquals(2, firstOfNextCycle)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidShuffleCannotLoseOrDuplicateCatalogEntries() {
        MusicShuffleBag(listOf(1, 2, 3)) { listOf(1, 1, 2) }.next()
    }
}

