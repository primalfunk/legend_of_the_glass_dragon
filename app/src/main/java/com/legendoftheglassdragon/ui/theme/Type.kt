package com.legendoftheglassdragon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TerminalBody = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp,
)

val TerminalTypography = Typography(
    bodyLarge = TerminalBody,
    bodyMedium = TerminalBody.copy(fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = TerminalBody.copy(fontWeight = FontWeight.Bold),
    titleLarge = TerminalBody.copy(
        fontSize = 25.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
    ),
)
