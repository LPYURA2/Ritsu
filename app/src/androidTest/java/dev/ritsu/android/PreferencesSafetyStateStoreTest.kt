package dev.ritsu.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ritsu.android.data.PreferencesSafetyStateStore
import dev.ritsu.core.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesSafetyStateStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences get() = context.getSharedPreferences("demo_safety", Context.MODE_PRIVATE)

    @Before fun clearBefore() { assertTrue(preferences.edit().clear().commit()) }
    @After fun clearAfter() { assertTrue(preferences.edit().clear().commit()) }

    @Test fun stopAndAuditSurviveNewAndroidStoreInstance() = runBlocking {
        val original = DemoCoreGateway(PreferencesSafetyStateStore(context))
        original.grant(Capability.UPDATE_GOALS)
        original.stop()
        val recreated = DemoCoreGateway(PreferencesSafetyStateStore(context))
        assertEquals(CoreMode.STOPPED, recreated.snapshot.value.mode)
        assertTrue(recreated.snapshot.value.grants.isEmpty())
        assertTrue(recreated.snapshot.value.events.any { it.title == "STOP" })
        assertFalse(recreated.resume().accepted)
    }

    @Test fun corruptJsonCannotEnableActions() = runBlocking {
        assertTrue(preferences.edit().putString("state", "{broken-json").commit())
        val gateway = DemoCoreGateway(PreferencesSafetyStateStore(context))
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)
        assertFalse(gateway.sendMessage("Привет").accepted)
    }

    @Test fun storedTemporaryAuthorityIsDroppedOnRecreation() = runBlocking {
        val original = DemoCoreGateway(PreferencesSafetyStateStore(context))
        original.grant(Capability.UPDATE_GOALS, durationMinutes = 15, maxActions = 3)
        assertEquals(1, PreferencesSafetyStateStore(context).load().grants.size)
        val recreated = DemoCoreGateway(PreferencesSafetyStateStore(context))
        assertTrue(recreated.snapshot.value.grants.isEmpty())
        assertFalse(recreated.advanceGoal("day-plan").accepted)
    }
}
