package com.drivetheory.cbt

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.drivetheory.cbt.presentation.MainActivity
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_displays_title_and_buttons() {
        composeRule.onNodeWithText("DriveTheory CBT").assertIsDisplayed()
        composeRule.onNodeWithText("Cars exams").assertIsDisplayed()
        composeRule.onNodeWithText("Motorcycle exams").assertIsDisplayed()
        composeRule.onNodeWithText("Lorries exams").assertIsDisplayed()
        composeRule.onNodeWithText("Buses and coaches exams").assertIsDisplayed()
    }
}
