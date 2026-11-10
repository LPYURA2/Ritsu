package dev.ritsu.core

import kotlinx.coroutines.flow.StateFlow

enum class CoreMode { ACTIVE, PAUSED, STOPPED }
enum class Mood { CALM, HAPPY, THINKING, SLEEPING, CONCERNED }
enum class Capability { UPDATE_GOALS, EXPLORE_INTERESTS }
enum class MessageRole { USER, RITSU }
enum class AuditKind { DECISION, WAKE, ACTION, SECURITY, SYSTEM }

data class Goal(val id: String, val title: String, val description: String, val progress: Int)
data class Interest(val id: String, val title: String, val description: String)
data class ChatMessage(val id: String, val role: MessageRole, val text: String, val timestampMillis: Long)
data class AuditEvent(
    val id: String,
    val timestampMillis: Long,
    val kind: AuditKind,
    val title: String,
    val detail: String,
    val allowed: Boolean = true,
)
data class PermissionGrant(
    val capability: Capability,
    val expiresAtMillis: Long,
    val remainingActions: Int,
)
data class ConnectionInfo(
    val label: String = "Локальная демонстрация",
    val connected: Boolean = false,
    val coreId: String = "demo-ritsu",
    val protocolVersion: String = "ritsu-core/v1 · проект контракта",
    val latencyMillis: Long? = null,
)
data class CoreSnapshot(
    val mode: CoreMode,
    val mood: Mood,
    val activity: String,
    val goals: List<Goal>,
    val interests: List<Interest>,
    val messages: List<ChatMessage>,
    val events: List<AuditEvent>,
    val grants: List<PermissionGrant>,
    val connection: ConnectionInfo = ConnectionInfo(),
)
data class CommandResult(val accepted: Boolean, val message: String)

/**
 * UI-facing contract for the ONE authoritative agent core.
 * A future transport must return core-confirmed state and enforce authorization on the core.
 * This contract deliberately has no arbitrary command, shell, path, or process operation.
 */
interface CoreGateway {
    val snapshot: StateFlow<CoreSnapshot>
    suspend fun sendMessage(text: String): CommandResult
    suspend fun stop(): CommandResult
    suspend fun pause(): CommandResult
    suspend fun resume(acknowledgeStop: Boolean = false): CommandResult
    suspend fun grant(capability: Capability, durationMinutes: Int = 15, maxActions: Int = 3): CommandResult
    suspend fun revoke(capability: Capability): CommandResult
    suspend fun advanceGoal(goalId: String): CommandResult
    suspend fun exploreInterest(interestId: String): CommandResult
    suspend fun refresh(): CommandResult
}

fun interface CoreClock { fun nowMillis(): Long }

/** Durable safety state only. Demo conversations and progress are deliberately ephemeral. */
data class SafetyState(
    val mode: CoreMode = CoreMode.ACTIVE,
    val grants: List<PermissionGrant> = emptyList(),
    val events: List<AuditEvent> = emptyList(),
)

/** Implementations must save atomically or throw. Never silently ignore persistence failures. */
interface SafetyStateStore {
    fun load(): SafetyState
    fun save(state: SafetyState)
}

class MemorySafetyStateStore(initial: SafetyState = SafetyState()) : SafetyStateStore {
    private var state = initial
    override fun load(): SafetyState = state
    override fun save(state: SafetyState) { this.state = state }
}
