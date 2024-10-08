package com.martinlutern.smartcalculator.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.martinlutern.smartcalculator.BuildConfig

private val RedColorScheme = darkColorScheme(
    primary = Color.Red,
    secondary = Color(0xFFe74c3c),
    tertiary = Color(0xFFc0392b),
    background = Color.Red,
)

private val GreenColorScheme = darkColorScheme(
    primary = Color.Green,
    secondary = Color(0xFF2ecc71),
    tertiary = Color(0xFF27ae60),
    background = Color.Green
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

@Composable
fun SmartCalculatorTheme(content: @Composable () -> Unit) {
    val colorScheme = when (BuildConfig.BUILD_TYPE) {
        "red" -> {
            println("masuk sini 1")
            RedColorScheme
        }
        "green" -> {
            println("masuk sini 2")
            GreenColorScheme
        }
        else -> {
            println("masuk sini 3")
            DarkColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}