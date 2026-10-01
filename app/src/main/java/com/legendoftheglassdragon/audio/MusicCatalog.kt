package com.legendoftheglassdragon.audio

import kotlin.random.Random

/** The complete Principal-supplied music library. click.mp3 deliberately lives elsewhere. */
enum class MusicTrack(val packagedName: String) {
    MUSS_01("muss_01"),
    MUSS_02("muss_02"),
    MUSS_03("muss_03"),
    MUSS_04("muss_04"),
    MUSS_05("muss_05"),
    MUSS_06("muss_06"),
    MUSS_07("muss_07"),
    MUSS_08("muss_08"),
    MUSS_09("muss_09"),
    MUSS_10("muss_10"),
    MUSS_11("muss_11"),
    MUSS_12("muss_12"),
    MUSS_13("muss_13"),
    MUSS_14("muss_14"),
    MUSS_15("muss_15"),
}

/**
 * Plays every entry once before reshuffling. The boundary swap prevents the
 * final track of one bag from immediately opening the next bag.
 */
class MusicShuffleBag<T>(
    items: List<T>,
    private val shuffle: (List<T>) -> List<T> = { source -> source.shuffled(Random.Default) },
) {
    private val source = items.distinct()
    private var remaining = emptyList<T>()
    private var lastPlayed: T? = null

    init {
        require(source.isNotEmpty()) { "A music shuffle bag cannot be empty." }
    }

    @Synchronized
    fun next(): T {
        if (remaining.isEmpty()) refill()
        return remaining.first().also { selected ->
            remaining = remaining.drop(1)
            lastPlayed = selected
        }
    }

    @Synchronized
    fun remainingCount(): Int = remaining.size

    private fun refill() {
        val candidate = shuffle(source).toMutableList()
        require(candidate.size == source.size && candidate.toSet() == source.toSet()) {
            "Shuffle must return every catalog entry exactly once."
        }
        if (candidate.size > 1 && candidate.first() == lastPlayed) {
            val replacement = candidate.indexOfFirst { it != lastPlayed }
            candidate[0] = candidate[replacement].also { candidate[replacement] = candidate[0] }
        }
        remaining = candidate
    }
}

