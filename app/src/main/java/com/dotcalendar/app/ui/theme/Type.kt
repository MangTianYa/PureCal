package com.dotcalendar.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Google Pixel 标准系统字体（清晰无衬线字体） */
val PixelFont = FontFamily.Default

// 兼容别名
val DotFont = PixelFont

val MonoDigits = TextStyle(
    fontFamily = PixelFont,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 18.sp,
)

val MicroLabel = TextStyle(
    fontFamily = PixelFont,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.5.sp,
)
