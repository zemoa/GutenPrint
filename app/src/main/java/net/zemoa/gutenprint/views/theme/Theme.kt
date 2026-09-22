package net.zemoa.gutenprint.views.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.colorResource
import net.zemoa.gutenprint.R

@Composable
fun GutenPrintTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = colorResource(R.color.purple_200),
                onPrimary = colorResource(R.color.black),
                primaryContainer = colorResource(R.color.purple_700),
                secondary = colorResource(R.color.teal_200),
                onSecondary = colorResource(R.color.black),
            )
        } else {
            lightColorScheme(
                primary = colorResource(R.color.purple_500),
                onPrimary = colorResource(R.color.white),
                primaryContainer = colorResource(R.color.purple_200),
                secondary = colorResource(R.color.teal_200),
                onSecondary = colorResource(R.color.black),
            )
        },
        content = content,
    )
}
