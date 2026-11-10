package dev.ritsu.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DemoCoreGatewayTest {
    private class FakeClock(var time: Long = 1_000_000L) : CoreClock {
        override fun nowMillis() = time
    }

    @Test fun `goal action cannot bypass core authorization`() = runTest {
        val gateway = DemoCoreGateway()
        val before = gateway.snapshot.value.goals.first()
        val result = gateway.advanceGoal(before.id)
        assertFalse(result.accepted)
        assertEquals(before.progress, gateway.snapshot.value.goals.first().progress)
        assertFalse(gateway.snapshot.value.events.last().allowed)
    }

    @Test fun `grant is scoped and consumed exactly once per action`() = runTest {
        val gateway = DemoCoreGateway()
        gateway.grant(Capability.UPDATE_GOALS, maxActions = 1)
        assertFalse(gateway.exploreInterest("music").accepted)
        assertTrue(gateway.advanceGoal("day-plan").accepted)
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        assertEquals(50, gateway.snapshot.value.goals.first().progress)
    }

    @Test fun `grant expires at exact boundary without UI refresh`() = runTest {
        val clock = FakeClock()
        val gateway = DemoCoreGateway(clock = clock)
        gateway.grant(Capability.UPDATE_GOALS, durationMinutes = 1, maxActions = 3)
        clock.time += 60_000L
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        assertEquals(40, gateway.snapshot.value.goals.first().progress)
    }

    @Test fun `refresh expires grants without creating repetitive audit events`() = runTest {
        val clock = FakeClock()
        val gateway = DemoCoreGateway(clock = clock)
        gateway.grant(Capability.UPDATE_GOALS, durationMinutes = 1)
        val eventCount = gateway.snapshot.value.events.size
        repeat(10) { gateway.refresh() }
        assertEquals(eventCount, gateway.snapshot.value.events.size)
        clock.time += 60_000L
        gateway.refresh()
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        assertEquals(eventCount + 1, gateway.snapshot.value.events.size)
    }

    @Test fun `STOP is latched revokes grants and survives gateway recreation`() = runTest {
        val store = MemorySafetyStateStore()
        val gateway = DemoCoreGateway(store)
        gateway.grant(Capability.UPDATE_GOALS)
        assertTrue(gateway.stop().accepted)
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        assertFalse(gateway.resume().accepted)
        assertFalse(gateway.pause().accepted)
        assertFalse(gateway.grant(Capability.UPDATE_GOALS).accepted)
        assertFalse(gateway.sendMessage("Привет").accepted)
        val recreated = DemoCoreGateway(store)
        assertEquals(CoreMode.STOPPED, recreated.snapshot.value.mode)
        assertFalse(recreated.resume().accepted)
        assertTrue(recreated.resume(acknowledgeStop = true).accepted)
        assertEquals(CoreMode.ACTIVE, recreated.snapshot.value.mode)
        assertFalse(recreated.advanceGoal("day-plan").accepted)
    }

    @Test fun `pause blocks all agent actions while expiry keeps running`() = runTest {
        val clock = FakeClock()
        val gateway = DemoCoreGateway(clock = clock)
        gateway.grant(Capability.UPDATE_GOALS, durationMinutes = 1)
        gateway.pause()
        assertEquals(CoreMode.PAUSED, gateway.snapshot.value.mode)
        assertFalse(gateway.sendMessage("Привет").accepted)
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        clock.time += 60_000L
        assertTrue(gateway.resume().accepted)
        assertFalse(gateway.advanceGoal("day-plan").accepted)
    }

    @Test fun `temporary grants are not restored across process restart`() = runTest {
        val store = MemorySafetyStateStore()
        val gateway = DemoCoreGateway(store)
        gateway.grant(Capability.UPDATE_GOALS)
        val recreated = DemoCoreGateway(store)
        assertEquals(CoreMode.ACTIVE, recreated.snapshot.value.mode)
        assertTrue(recreated.snapshot.value.grants.isEmpty())
        assertFalse(recreated.advanceGoal("day-plan").accepted)
        assertTrue(recreated.snapshot.value.events.any { it.title == "Разрешения сброшены при запуске" })
    }

    @Test fun `PAUSED survives gateway recreation and needs explicit resume`() = runTest {
        val store = MemorySafetyStateStore()
        val gateway = DemoCoreGateway(store)
        assertTrue(gateway.pause().accepted)
        val recreated = DemoCoreGateway(store)
        assertEquals(CoreMode.PAUSED, recreated.snapshot.value.mode)
        assertFalse(recreated.sendMessage("Привет").accepted)
        assertFalse(recreated.exploreInterest("music").accepted)
        assertTrue(recreated.resume().accepted)
        assertEquals(CoreMode.ACTIVE, recreated.snapshot.value.mode)
        assertTrue(recreated.sendMessage("Привет").accepted)
    }

    @Test fun `invalid and excessive grants are rejected without escalation`() = runTest {
        val gateway = DemoCoreGateway()
        for ((minutes, actions) in listOf(0 to 1, 61 to 1, 1 to 0, 1 to 11, Int.MAX_VALUE to Int.MAX_VALUE)) {
            assertFalse(gateway.grant(Capability.UPDATE_GOALS, minutes, actions).accepted)
            assertTrue(gateway.snapshot.value.grants.isEmpty())
        }
    }

    @Test fun `concurrent actions cannot duplicate a single remaining allowance`() = runTest {
        val gateway = DemoCoreGateway()
        gateway.grant(Capability.UPDATE_GOALS, maxActions = 1)
        val results = (1..30).map { async(Dispatchers.Default) { gateway.advanceGoal("day-plan") } }.awaitAll()
        assertEquals(1, results.count { it.accepted })
        assertEquals(50, gateway.snapshot.value.goals.first().progress)
    }

    @Test fun `invalid target does not consume grant`() = runTest {
        val gateway = DemoCoreGateway()
        gateway.grant(Capability.UPDATE_GOALS, maxActions = 1)
        assertFalse(gateway.advanceGoal("missing-goal").accepted)
        assertEquals(1, gateway.snapshot.value.grants.single().remainingActions)
        assertTrue(gateway.advanceGoal("day-plan").accepted)
    }

    @Test fun `storage write failure blocks action and fails closed`() = runTest {
        val store = FailingStore()
        val gateway = DemoCoreGateway(store)
        gateway.grant(Capability.UPDATE_GOALS)
        store.failWrites = true
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        assertEquals(40, gateway.snapshot.value.goals.first().progress)
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        assertFalse(gateway.resume(true).accepted)
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)
    }

    @Test fun `storage read failure starts in STOP`() = runTest {
        val store = object : SafetyStateStore {
            override fun load(): SafetyState = error("private diagnostic")
            override fun save(state: SafetyState) = Unit
        }
        val gateway = DemoCoreGateway(store)
        assertEquals(CoreMode.STOPPED, gateway.snapshot.value.mode)
        assertFalse(gateway.sendMessage("Привет").accepted)
        assertFalse(gateway.snapshot.value.events.toString().contains("private diagnostic"))
    }

    @Test fun `malformed saved permission fails closed`() = runTest {
        val clock = FakeClock()
        val store = MemorySafetyStateStore(SafetyState(grants = listOf(
            PermissionGrant(Capability.UPDATE_GOALS, Long.MAX_VALUE, -1),
        )))
        assertEquals(CoreMode.STOPPED, DemoCoreGateway(store, clock).snapshot.value.mode)
    }

    @Test fun `moving wall clock backwards does not restore expired grants`() = runTest {
        val clock = FakeClock()
        val gateway = DemoCoreGateway(clock = clock)
        gateway.grant(Capability.UPDATE_GOALS, durationMinutes = 1)
        clock.time += 60_000L
        gateway.refresh()
        clock.time -= 120_000L
        assertFalse(gateway.advanceGoal("day-plan").accepted)
    }

    @Test fun `moving wall clock backwards before expiry revokes grant before action`() = runTest {
        val clock = FakeClock()
        val gateway = DemoCoreGateway(clock = clock)
        gateway.grant(Capability.UPDATE_GOALS, durationMinutes = 1)
        clock.time += 30_000L
        gateway.refresh()
        assertEquals(1, gateway.snapshot.value.grants.size)

        clock.time -= 10_000L
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        assertTrue(gateway.snapshot.value.grants.isEmpty())
        assertEquals(40, gateway.snapshot.value.goals.first().progress)
        assertTrue(gateway.snapshot.value.events.any { it.title == "Разрешения отозваны: откат часов" })

        // Even a newly issued grant cannot execute while the clock remains behind.
        assertTrue(gateway.grant(Capability.UPDATE_GOALS, durationMinutes = 1).accepted)
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        assertTrue(gateway.snapshot.value.grants.isEmpty())

        clock.time += 20_000L
        assertFalse(gateway.advanceGoal("day-plan").accepted)
        assertTrue(gateway.snapshot.value.grants.isEmpty())
    }

    @Test fun `chat validates input and records no message body in audit`() = runTest {
        val gateway = DemoCoreGateway()
        val initialCount = gateway.snapshot.value.messages.size
        assertFalse(gateway.sendMessage("   ").accepted)
        assertFalse(gateway.sendMessage("a".repeat(4_001)).accepted)
        assertEquals(initialCount, gateway.snapshot.value.messages.size)
        val privateText = "message-private-body"
        assertTrue(gateway.sendMessage(privateText).accepted)
        assertEquals(initialCount + 2, gateway.snapshot.value.messages.size)
        assertEquals(privateText, gateway.snapshot.value.messages[gateway.snapshot.value.messages.lastIndex - 1].text)
        assertEquals(MessageRole.RITSU, gateway.snapshot.value.messages.last().role)
        assertFalse(gateway.snapshot.value.events.toString().contains(privateText))
        assertFalse(gateway.snapshot.value.connection.connected)
    }

    @Test fun `journal remains bounded including denied actions`() = runTest {
        val gateway = DemoCoreGateway()
        repeat(250) { gateway.advanceGoal("day-plan") }
        assertEquals(DemoCoreGateway.MAX_EVENTS, gateway.snapshot.value.events.size)
        assertFalse(gateway.snapshot.value.events.last().allowed)
    }

    private class FailingStore : SafetyStateStore {
        private var state = SafetyState()
        var failWrites = false
        override fun load() = state
        override fun save(state: SafetyState) {
            if (failWrites) error("private diagnostic")
            this.state = state
        }
    }
}
