package org.maktaba.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

private val MaktabaTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontSize = 22.sp, lineHeight = 26.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontSize = 15.sp, lineHeight = 19.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
        bodySmall = bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = labelLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelMedium = labelMedium.copy(fontSize = 11.sp, lineHeight = 14.sp),
        labelSmall = labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
    )
}

@Composable
fun MaktabaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaktabaTypography,
        content = content,
    )
}
