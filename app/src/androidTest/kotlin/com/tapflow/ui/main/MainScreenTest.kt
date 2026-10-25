package com.tapflow.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tapflow.data.db.entity.RegisteredTagEntity
import org.junit.Rule
import org.junit.Test

/** UI instrumentation tests for [MainScreenContent]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun emptyState_displaysCorrectMessage() {
    composeTestRule.setContent {
      MainScreenContent(
        state = MainUiState(
          isNfcAvailable = true,
          isNfcEnabled = true,
          registeredTags = emptyList()
        )
      )
    }

    composeTestRule.onNodeWithText("TapFlow").assertIsDisplayed()
    composeTestRule.onNodeWithText("No tags registered yet").assertIsDisplayed()
  }

  @Test
  fun registeredTags_displayedInList() {
    val sampleTags = listOf(
      RegisteredTagEntity(
        tagKey = "04A1B2C3D4E5F6|NfcA",
        displayName = "Desk Launcher",
        actionPayload = "https://example.com/desk",
        rawUidHex = "04A1B2C3D4E5F6",
        techList = "NfcA",
        registeredAt = System.currentTimeMillis()
      )
    )

    composeTestRule.setContent {
      MainScreenContent(
        state = MainUiState(
          isNfcAvailable = true,
          isNfcEnabled = true,
          registeredTags = sampleTags
        )
      )
    }

    composeTestRule.onNodeWithText("Desk Launcher").assertIsDisplayed()
    composeTestRule.onNodeWithText("https://example.com/desk").assertIsDisplayed()
  }
}

