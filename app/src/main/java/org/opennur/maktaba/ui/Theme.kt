package org.opennur.maktaba.ui

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
        headlineSmall = headlineSmall.copy(fontSize = 20.sp, lineHeight = 24.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontSize = 17.sp, lineHeight = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
        bodyLarge = bodyLarge.copy(fontSize = 14.sp, lineHeight = 21.sp),
        bodyMedium = bodyMedium.copy(fontSize = 12.sp, lineHeight = 18.sp),
        bodySmall = bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
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
