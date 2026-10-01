package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterNameValidatorTest {
    @Test
    fun trimsOrdinaryNameWithoutOverPolicingIt() {
        val result = CharacterNameValidator.validate("  Arcturus the III!  ")

        assertEquals(NameValidationResult.Valid("Arcturus the III!"), result)
    }

    @Test
    fun removesControlCharactersAndPreservesUnicode() {
        val result = CharacterNameValidator.validate("  Éowyn\n龙\u0000  ")

        assertEquals(NameValidationResult.Valid("Éowyn龙"), result)
    }

    @Test
    fun emptyNameIsRejected() {
        val result = CharacterNameValidator.validate(" \n\t ")

        assertEquals(NameValidationResult.Invalid(NameValidationError.Empty), result)
    }

    @Test
    fun overlongNameIsRejectedWithoutSplittingUnicodeCodePoints() {
        val exactlyMaximum = "🐉".repeat(CharacterNameValidator.MAX_CODE_POINTS)
        val tooLong = exactlyMaximum + "A"

        assertTrue(CharacterNameValidator.validate(exactlyMaximum) is NameValidationResult.Valid)
        assertEquals(
            NameValidationResult.Invalid(NameValidationError.TooLong),
            CharacterNameValidator.validate(tooLong),
        )
    }
}
