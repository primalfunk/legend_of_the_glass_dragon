package com.legendoftheglassdragon.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator

sealed interface SaveLoadResult {
    data object Missing : SaveLoadResult
    data class Loaded(val gameState: GameState) : SaveLoadResult
    data class Invalid(val failure: SaveFailure) : SaveLoadResult
}

sealed interface SaveFailure {
    val message: String

    data class Corrupt(override val message: String) : SaveFailure
    data class UnsupportedVersion(val version: Int) : SaveFailure {
        override val message: String = "Save version $version is not supported by this build"
    }
    data class InvalidState(override val message: String) : SaveFailure
    data class Storage(override val message: String) : SaveFailure
}

sealed interface SaveWriteResult {
    data object Saved : SaveWriteResult
    data class Failed(val failure: SaveFailure) : SaveWriteResult
}

internal interface StringSaveStorage {
    fun read(): String?
    fun write(value: String)
    fun delete()
}

class GameSaveRepository internal constructor(
    private val storage: StringSaveStorage,
    private val codec: GameStateCodec = GameStateCodec(),
) {
    fun save(state: GameState): SaveWriteResult {
        val validation = GameStateValidator.validate(state)
        if (validation is GameStateValidationResult.Invalid) {
            return SaveWriteResult.Failed(SaveFailure.InvalidState(validation.reason))
        }

        return try {
            storage.write(codec.encode(state))
            SaveWriteResult.Saved
        } catch (_: Exception) {
            SaveWriteResult.Failed(SaveFailure.Storage("The local save could not be written"))
        }
    }

    fun load(): SaveLoadResult {
        val serialized = try {
            storage.read()
        } catch (_: Exception) {
            return SaveLoadResult.Invalid(SaveFailure.Storage("The local save could not be read"))
        } ?: return SaveLoadResult.Missing

        return when (val result = codec.decode(serialized)) {
            is DecodeResult.Success -> SaveLoadResult.Loaded(result.gameState)
            is DecodeResult.Failure -> SaveLoadResult.Invalid(result.failure)
        }
    }

    fun hasStoredSave(): Boolean = try {
        storage.read() != null
    } catch (_: Exception) {
        true
    }

    fun delete(): Boolean = try {
        storage.delete()
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val FILE_NAME = "glass_dragon_game_state"
        private const val SAVE_KEY = "game_state"
        private const val COMPLETED_SEASON_FILE_NAME = "glass_dragon_completed_season"
        private const val COMPLETED_SEASON_KEY = "completed_season"

        fun from(context: Context): GameSaveRepository {
            val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return GameSaveRepository(SharedPreferencesSaveStorage(preferences, SAVE_KEY))
        }

        /** Independent archive retained when the active slot starts another season. */
        fun completedSeasonFrom(context: Context): GameSaveRepository {
            val preferences = context.getSharedPreferences(
                COMPLETED_SEASON_FILE_NAME,
                Context.MODE_PRIVATE,
            )
            return GameSaveRepository(
                SharedPreferencesSaveStorage(preferences, COMPLETED_SEASON_KEY),
            )
        }
    }
}

@SuppressLint("UseKtx") // commit() is intentional: gameplay advances only after a confirmed disk write.
private class SharedPreferencesSaveStorage(
    private val preferences: SharedPreferences,
    private val key: String,
) : StringSaveStorage {
    override fun read(): String? = preferences.getString(key, null)

    override fun write(value: String) {
        check(preferences.edit().putString(key, value).commit()) {
            "SharedPreferences refused the game save"
        }
    }

    override fun delete() {
        check(preferences.edit().remove(key).commit()) {
            "SharedPreferences refused to delete the game save"
        }
    }
}
