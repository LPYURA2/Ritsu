package dev.ritsu.android

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import dev.ritsu.android.ui.RitsuApp
import dev.ritsu.android.ui.RitsuTheme
import dev.ritsu.core.Capability
import dev.ritsu.core.CoreMode
import dev.ritsu.core.DemoCoreGateway
import dev.ritsu.core.MessageRole
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Every test uses an isolated memory-only gateway, never application preferences or Termux. */
class RitsuUiTest {
    @get:Rule val compose = createComposeRule()
    private val viewModelStore = ViewModelStore()
    private lateinit var gateway: DemoCoreGateway

    @After fun disposeViewModel() {
        viewModelStore.clear()
    }

    @Test fun allSixDestinationsRemainReachable() {
        launchApp()
        listOf("home", "chat", "goals", "permissions", "history", "system", "home").forEach { route ->
            navigate(route)
            compose.onNodeWithTag("screen_$route").assertIsDisplayed()
        }
    }

    @Test fun sendingTextDisplaysTheScriptedDemoReply() {
        launchApp()
        navigate("chat")
        val previousCount = gateway.snapshot.value.messages.size
        compose.onNodeWithTag("chat_input").performTextInput("Привет, Рицу")
        compose.onNodeWithTag("chat_send").assertIsEnabled().performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.messages.size == previousCount + 2 }

        val messages = gateway.snapshot.value.messages
        assertEquals(MessageRole.USER, messages[messages.lastIndex - 1].role)
        assertEquals("Привет, Рицу", messages[messages.lastIndex - 1].text)
        assertEquals(MessageRole.RITSU, messages.last().role)
        assertTrue(messages.last().text.contains("ответы заранее подготовлены"))
        assertFalse(gateway.snapshot.value.connection.connected)
        compose.onNodeWithText(messages.last().text).assertIsDisplayed()
        compose.onNodeWithTag("chat_send").assertIsNotEnabled()
    }

    @Test fun pauseDisablesChatAndManualResumeRestoresIt() {
        launchApp()
        navigate("permissions")
        compose.onNodeWithTag("permissions_pause").performScrollTo().performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.mode == CoreMode.PAUSED }
        navigate("chat")
        compose.onNodeWithTag("chat_input").assertIsNotEnabled()
        compose.onNodeWithTag("chat_send").assertIsNotEnabled()

        navigate("permissions")
        compose.onNodeWithTag("permissions_resume").performScrollTo().performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.mode == CoreMode.ACTIVE }
        navigate("chat")
        compose.onNodeWithTag("chat_input").assertIsEnabled()
    }

    @Test fun stopRevokesScopesAndRequiresExplicitConfirmation() {
        launchApp()
        navigate("permissions")
        compose.onNodeWithTag("grant_UPDATE_GOALS").performScrollTo().performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.grants.any { it.capability == Capability.UPDATE_GOALS } }

        // Global STOP is reachable without scrolling to the permission card.
        compose.onNodeWithTag("global_stop").performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.mode == CoreMode.STOPPED }
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        compose.onNodeWithTag("grant_UPDATE_GOALS").assertIsNotEnabled()

        compose.onNodeWithTag("permissions_resume").performScrollTo().performClick()
        compose.onNodeWithTag("confirm_resume").assertIsDisplayed()
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)
        compose.onNodeWithText("Отмена").performClick()
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)

        compose.onNodeWithTag("permissions_resume").performClick()
        compose.onNodeWithTag("confirm_resume").performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.mode == CoreMode.ACTIVE }
        assertTrue(gateway.snapshot.value.grants.isEmpty())
    }

    @Test fun oneActionGrantAdvancesGoalOnceAndThenDeniesFurtherProgress() {
        launchApp()
        navigate("permissions")
        compose.onNodeWithText("1", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("grant_UPDATE_GOALS").performScrollTo().performClick()
        compose.waitUntil(20_000) {
            gateway.snapshot.value.grants.any { it.capability == Capability.UPDATE_GOALS && it.remainingActions == 1 }
        }
        val goal = gateway.snapshot.value.goals.first()
        navigate("goals")
        compose.onNodeWithTag("goal_${goal.id}").performScrollTo().performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.goals.first().progress == goal.progress + 10 }
        assertTrue(gateway.snapshot.value.grants.none { it.capability == Capability.UPDATE_GOALS })

        compose.onNodeWithTag("goal_${goal.id}").performClick()
        compose.waitUntil(20_000) { gateway.snapshot.value.events.lastOrNull()?.allowed == false }
        assertEquals(goal.progress + 10, gateway.snapshot.value.goals.first().progress)
        assertTrue(gateway.snapshot.value.events.last().title.contains("отклонено"))
    }

    private fun launchApp() {
        gateway = DemoCoreGateway()
        val vm = ViewModelProvider(viewModelStore, RitsuViewModel.Factory(gateway))[RitsuViewModel::class.java]
        compose.setContent {
            val snapshot by vm.snapshot.collectAsState()
            RitsuTheme { RitsuApp(snapshot, vm) }
        }
    }

    private fun navigate(route: String) {
        compose.onNodeWithTag("nav_$route").performClick()
    }
}
