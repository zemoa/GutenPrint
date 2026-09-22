package net.zemoa.gutenprint.views.printing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.zemoa.gutenprint.domains.printing.PrintingState
import net.zemoa.gutenprint.views.theme.GutenPrintTheme
import org.junit.Rule
import org.junit.Test

class PrintingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateShowsFileSelectionAction() {
        composeRule.setContent {
            GutenPrintTheme {
                PrintingScreen(state = PrintingState())
            }
        }

        composeRule.onNodeWithText("Choose file").assertIsDisplayed()
    }
}
