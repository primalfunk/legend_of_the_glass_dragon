package com.legendoftheglassdragon.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.TrophyCatalog
import com.legendoftheglassdragon.game.TrophyDefinition
import com.legendoftheglassdragon.game.TrophyEngine
import com.legendoftheglassdragon.game.TrophyMetagameSnapshot
import com.legendoftheglassdragon.game.TrophyUnlock
import java.util.Properties

data class EarnedTrophy(
    val id: String,
    val earnedAtMillis: Long,
    val campaignId: String? = null,
    val day: Int? = null,
    val value: Int? = null,
)

data class TrophyStoreState(
    val earned: Map<String, EarnedTrophy> = emptyMap(),
    val dragonVictories: Int = 0,
    val countedDragonCampaignIds: Set<String> = emptySet(),
) {
    val earnedCount: Int get() = earned.size
}

internal interface TrophyStringStore {
    fun read(): String?
    fun write(value: String)
    fun delete()
}

/** Separate from the active character slot: New Game intentionally cannot clear this cabinet. */
class TrophyRepository internal constructor(
    private val storage: TrophyStringStore,
) {
    fun load(): TrophyStoreState = try {
        decode(storage.read() ?: return TrophyStoreState())
    } catch (_: Exception) {
        TrophyStoreState()
    }

    fun evaluate(state: GameState): List<TrophyUnlock> {
        val current = load()
        val dragonVictories = current.dragonVictories + if (
            state.trophyStats.dragonVictoryInCampaign &&
                state.season.victorId == state.player.id &&
                state.player.id !in current.countedDragonCampaignIds
        ) 1 else 0
        return TrophyEngine.unlocked(
            state,
            TrophyMetagameSnapshot(current.earned.keys, dragonVictories),
        )
    }

    /** Awards are an atomic read/modify/write and return only newly earned records. */
    fun award(state: GameState, nowMillis: Long = System.currentTimeMillis()): List<TrophyUnlock> {
        val current = load()
        val dragonVictories = current.dragonVictories + if (
            state.trophyStats.dragonVictoryInCampaign &&
                state.season.victorId == state.player.id &&
                state.player.id !in current.countedDragonCampaignIds
        ) 1 else 0
        val snapshot = TrophyMetagameSnapshot(current.earned.keys, dragonVictories)
        val unlocks = TrophyEngine.unlocked(state, snapshot)
        if (unlocks.isEmpty() && dragonVictories == current.dragonVictories) return emptyList()
        val updated = current.copy(
            dragonVictories = dragonVictories,
            countedDragonCampaignIds = current.countedDragonCampaignIds + if (dragonVictories > current.dragonVictories) setOf(state.player.id) else emptySet<String>(),
            earned = current.earned + unlocks.associate { unlock ->
                unlock.trophy.id to EarnedTrophy(
                    id = unlock.trophy.id,
                    earnedAtMillis = nowMillis,
                    campaignId = state.player.id,
                    day = state.currentDay,
                    value = unlock.progress?.current,
                )
            },
        )
        save(updated)
        return unlocks
    }

    fun save(state: TrophyStoreState) {
        storage.write(encode(state))
    }

    fun delete(): Boolean = try {
        storage.delete()
        true
    } catch (_: Exception) {
        false
    }

    private fun encode(state: TrophyStoreState): String = Properties().apply {
        setProperty("version", "1")
        setProperty("dragonVictories", state.dragonVictories.toString())
        setProperty("dragonCampaigns", state.countedDragonCampaignIds.joinToString(","))
        setProperty("earned.count", state.earned.size.toString())
        state.earned.values.sortedBy { it.id }.forEachIndexed { index, record ->
            val prefix = "earned.$index."
            setProperty(prefix + "id", record.id)
            setProperty(prefix + "earnedAt", record.earnedAtMillis.toString())
            setProperty(prefix + "campaignId", record.campaignId.orEmpty())
            setProperty(prefix + "day", record.day?.toString().orEmpty())
            setProperty(prefix + "value", record.value?.toString().orEmpty())
        }
    }.let { values -> java.io.StringWriter().also { values.store(it, null) }.toString() }

    private fun decode(serialized: String): TrophyStoreState {
        val values = Properties().apply { load(serialized.reader()) }
        val count = values.getProperty("earned.count")?.toIntOrNull() ?: 0
        val earned = buildMap {
            repeat(count) { index ->
                val prefix = "earned.$index."
                val id = values.getProperty(prefix + "id") ?: return@repeat
                if (TrophyCatalog.all.none { it.id == id }) return@repeat
                put(id, EarnedTrophy(
                    id = id,
                    earnedAtMillis = values.getProperty(prefix + "earnedAt")?.toLongOrNull() ?: 0L,
                    campaignId = values.getProperty(prefix + "campaignId")?.ifBlank { null },
                    day = values.getProperty(prefix + "day")?.toIntOrNull(),
                    value = values.getProperty(prefix + "value")?.toIntOrNull(),
                ))
            }
        }
        return TrophyStoreState(
            earned = earned,
            dragonVictories = values.getProperty("dragonVictories")?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            countedDragonCampaignIds = values.getProperty("dragonCampaigns")
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet(),
        )
    }

    companion object {
        private const val FILE_NAME = "glass_dragon_trophies"
        private const val KEY = "trophy_state"

        fun from(context: Context): TrophyRepository = TrophyRepository(
            SharedPreferencesTrophyStore(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)),
        )
    }
}

@SuppressLint("UseKtx")
private class SharedPreferencesTrophyStore(
    private val preferences: SharedPreferences,
) : TrophyStringStore {
    override fun read(): String? = preferences.getString("trophy_state", null)
    override fun write(value: String) { check(preferences.edit().putString("trophy_state", value).commit()) }
    override fun delete() { check(preferences.edit().remove("trophy_state").commit()) }
}
