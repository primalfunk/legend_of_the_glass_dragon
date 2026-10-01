package com.legendoftheglassdragon.game

sealed interface NameValidationResult {
    data class Valid(val name: String) : NameValidationResult
    data class Invalid(val error: NameValidationError) : NameValidationResult
}

enum class NameValidationError {
    Empty,
    TooLong,
}

object CharacterNameValidator {
    /** Counted as Unicode code points so a surrogate pair is never split. */
    const val MAX_CODE_POINTS = 24

    fun validate(input: String): NameValidationResult {
        val normalized = input
            .filterNot { character -> character.isISOControl() }
            .trim()

        if (normalized.isEmpty()) {
            return NameValidationResult.Invalid(NameValidationError.Empty)
        }

        val codePointCount = normalized.codePointCount(0, normalized.length)
        if (codePointCount > MAX_CODE_POINTS) {
            return NameValidationResult.Invalid(NameValidationError.TooLong)
        }

        return NameValidationResult.Valid(normalized)
    }
}
