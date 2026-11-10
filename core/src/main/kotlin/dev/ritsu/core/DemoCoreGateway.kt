package dev.ritsu.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Scripted, offline simulator of the core contract. No LLM, memory database, network,
 * Termux adapter, credentials, or autonomous background execution is used here.
 * Authorization belongs here, so calling this class directly cannot bypass the UI.
 */
class DemoCoreGateway(
    private val store: SafetyStateStore = MemorySafetyStateStore(),
    private val clock: CoreClock = CoreClock { System.currentTimeMillis() },
) : CoreGateway {
    private val mutex = Mutex()
    private var lastObservedTime = clock.nowMillis()
    private val mutableSnapshot = MutableStateFlow(initialSnapshot())
    override val snapshot: StateFlow<CoreSnapshot> = mutableSnapshot.asStateFlow()

    override suspend fun sendMessage(text: String): CommandResult = mutex.withLock {
        val current = pruned()
        if (current.mode != CoreMode.ACTIVE) return@withLock deny(current, "Сообщение отклонено", modeReason(current.mode))
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 4_000) {
            return@withLock deny(current, "Сообщение отклонено", "Введите от 1 до 4000 символов.")
        }
        val reply = when {
            clean.contains("привет", ignoreCase = true) -> "Привет! Я показываю, как будет выглядеть диалог с Рицу. Сейчас ответы заранее подготовлены, рабочее ядро не подключено."
            clean.contains("памят", ignoreCase = true) -> "После согласованной интеграции приложение будет использовать общую память существующей Рицу. В этой демонстрации базы памяти не открываются."
            clean.contains("цел", ignoreCase = true) -> "Демонстрационные цели есть в разделе «Цели и занятия». Для изменения прогресса требуется временное разрешение ядра."
            else -> "Сообщение принято в демонстрации. Это подготовленный ответ: модель и рабочая Рицу не вызываются. Будущий диалог будет обращаться к единому ядру."
        }
        val messages = (current.messages + listOf(
            ChatMessage(id(), MessageRole.USER, clean, now()),
            ChatMessage(id(), MessageRole.RITSU, reply, now()),
        )).takeLast(MAX_MESSAGES)
        commit(current.copy(
            mood = Mood.HAPPY,
            activity = "Общаюсь с тобой · демо",
            messages = messages,
            events = append(current, AuditKind.ACTION, "Демонстрационный диалог", "Добавлено сообщение и подготовленный ответ. Содержимое диалога не включается в журнал."),
        ), "Демонстрационный ответ получен.")
    }

    override suspend fun stop(): CommandResult = mutex.withLock {
        val current = pruned()
        commit(current.copy(
            mode = CoreMode.STOPPED,
            mood = Mood.SLEEPING,
            activity = "STOP · действия остановлены",
            grants = emptyList(),
            events = append(current, AuditKind.SECURITY, "STOP", "Все действия остановлены, временные разрешения отозваны. Для запуска нужно явное подтверждение."),
        ), "STOP применён. Все временные разрешения отозваны.")
    }

    override suspend fun pause(): CommandResult = mutex.withLock {
        val current = pruned()
        if (current.mode == CoreMode.STOPPED) return@withLock deny(current, "Пауза отклонена", "STOP сохраняется до явного подтверждения запуска.")
        commit(current.copy(
            mode = CoreMode.PAUSED,
            mood = Mood.SLEEPING,
            activity = "На паузе · жду продолжения",
            events = append(current, AuditKind.SECURITY, "Пауза", "Выполнение действий и демонстрационный диалог приостановлены. Срок разрешений продолжает истекать."),
        ), "Демонстрационное ядро на паузе.")
    }

    override suspend fun resume(acknowledgeStop: Boolean): CommandResult = mutex.withLock {
        val current = pruned()
        if (current.mode == CoreMode.STOPPED && !acknowledgeStop) {
            return@withLock deny(current, "Запуск отклонён", "Подтвердите возобновление после STOP. Разрешения потребуется выдать заново.")
        }
        commit(current.copy(
            mode = CoreMode.ACTIVE,
            mood = Mood.CALM,
            activity = "Здесь, рядом с тобой",
            events = append(current, AuditKind.WAKE, "Продолжение работы", "Пользователь явно возобновил демонстрационное ядро. Рабочая Рицу не затронута."),
        ), "Демонстрационное ядро активно.")
    }

    override suspend fun grant(capability: Capability, durationMinutes: Int, maxActions: Int): CommandResult = mutex.withLock {
        val current = pruned()
        if (current.mode == CoreMode.STOPPED) return@withLock deny(current, "Разрешение отклонено", "Сначала явно возобновите работу после STOP.")
        if (durationMinutes !in 1..MAX_GRANT_MINUTES || maxActions !in 1..MAX_GRANT_ACTIONS) {
            return@withLock deny(current, "Разрешение отклонено", "Допустимо 1–60 минут и 1–10 действий.")
        }
        val grant = PermissionGrant(capability, now() + durationMinutes * 60_000L, maxActions)
        commit(current.copy(
            grants = current.grants.filterNot { it.capability == capability } + grant,
            events = append(current, AuditKind.SECURITY, "Временное разрешение", "${capability.label()}: $durationMinutes мин., не более $maxActions действий. Проверка выполняется демонстрационным ядром."),
        ), "Разрешение выдано на $durationMinutes мин., $maxActions действий.")
    }

    override suspend fun revoke(capability: Capability): CommandResult = mutex.withLock {
        val current = pruned()
        commit(current.copy(
            grants = current.grants.filterNot { it.capability == capability },
            events = append(current, AuditKind.SECURITY, "Разрешение отозвано", capability.label()),
        ), "Разрешение отозвано.")
    }

    override suspend fun advanceGoal(goalId: String): CommandResult = mutex.withLock {
        val current = pruned()
        val rejection = authorize(current, Capability.UPDATE_GOALS)
        if (rejection != null) return@withLock deny(current, "Изменение цели отклонено", rejection)
        val goal = current.goals.find { it.id == goalId }
            ?: return@withLock deny(current, "Изменение цели отклонено", "Цель не найдена.")
        if (goal.progress >= 100) return@withLock deny(current, "Изменение цели отклонено", "Цель уже выполнена.")
        commit(consume(current, Capability.UPDATE_GOALS).copy(
            mood = Mood.THINKING,
            activity = "Продвигаюсь к цели · демо",
            goals = current.goals.map { if (it.id == goalId) it.copy(progress = (it.progress + 10).coerceAtMost(100)) else it },
            events = append(current, AuditKind.ACTION, "Прогресс цели обновлён", "${goal.title}: +10%. Это демонстрационное действие."),
        ), "Демонстрационный прогресс обновлён.")
    }

    override suspend fun exploreInterest(interestId: String): CommandResult = mutex.withLock {
        val current = pruned()
        val rejection = authorize(current, Capability.EXPLORE_INTERESTS)
        if (rejection != null) return@withLock deny(current, "Занятие отклонено", rejection)
        val interest = current.interests.find { it.id == interestId }
            ?: return@withLock deny(current, "Занятие отклонено", "Интерес не найден.")
        commit(consume(current, Capability.EXPLORE_INTERESTS).copy(
            mood = Mood.THINKING,
            activity = "Изучаю: ${interest.title.lowercase()} · демо",
            events = append(current, AuditKind.DECISION, "Выбрано занятие", "${interest.title}. Действие смоделировано без сети и фоновых процессов."),
        ), "Демонстрационное занятие выбрано.")
    }

    override suspend fun refresh(): CommandResult = mutex.withLock {
        val current = pruned()
        if (current != mutableSnapshot.value) commit(current, "Демонстрационные данные обновлены.")
        else CommandResult(true, "Локальная демонстрация доступна. Рабочее ядро не подключено.")
    }

    private fun authorize(current: CoreSnapshot, capability: Capability): String? {
        if (current.mode != CoreMode.ACTIVE) return modeReason(current.mode)
        val grant = current.grants.find { it.capability == capability }
            ?: return "Нужно временное разрешение: ${capability.label()}."
        if (grant.expiresAtMillis <= now() || grant.remainingActions <= 0) return "Срок или лимит разрешения исчерпан."
        return null
    }

    private fun consume(current: CoreSnapshot, capability: Capability) = current.copy(
        grants = current.grants.mapNotNull {
            if (it.capability != capability) it
            else if (it.remainingActions <= 1) null
            else it.copy(remainingActions = it.remainingActions - 1)
        },
    )

    private fun pruned(): CoreSnapshot {
        val current = mutableSnapshot.value
        val clockMovedBackwards = clock.nowMillis() < lastObservedTime
        val observedTime = now()
        if (clockMovedBackwards && current.grants.isNotEmpty()) return current.copy(
            grants = emptyList(),
            events = append(current, AuditKind.SECURITY, "Разрешения отозваны: откат часов", "Системное время уменьшилось. Все временные разрешения отозваны до проверки действия; выдайте их заново после восстановления времени."),
        )
        val valid = current.grants.filter { it.expiresAtMillis > observedTime && it.remainingActions > 0 }
        return if (valid == current.grants) current else current.copy(
            grants = valid,
            events = append(current, AuditKind.SECURITY, "Разрешение истекло", "Истёкшие разрешения отозваны ядром до проверки следующего действия."),
        )
    }

    private fun deny(current: CoreSnapshot, title: String, reason: String): CommandResult {
        val result = commit(current.copy(events = append(current, AuditKind.SECURITY, title, reason, allowed = false)), reason)
        return if (!result.accepted) result else CommandResult(false, reason)
    }

    /** Save safety state and audit BEFORE publishing or applying an action. */
    private fun commit(next: CoreSnapshot, message: String): CommandResult {
        return try {
            store.save(SafetyState(next.mode, next.grants, next.events))
            mutableSnapshot.value = next
            CommandResult(true, message)
        } catch (_: Exception) {
            val current = mutableSnapshot.value
            mutableSnapshot.value = current.copy(
                mode = CoreMode.STOPPED,
                mood = Mood.CONCERNED,
                activity = "STOP · ошибка сохранения безопасности",
                grants = emptyList(),
                events = append(current, AuditKind.SYSTEM, "Ошибка хранилища", "Состояние безопасности не сохранено. Действия заблокированы; содержимое исключения не записывается.", allowed = false),
            )
            CommandResult(false, "Ошибка сохранения. Действия заблокированы в режиме STOP.")
        }
    }

    private fun initialSnapshot(): CoreSnapshot {
        val loaded = try {
            store.load().also { saved ->
                require(saved.grants.map { it.capability }.distinct().size == saved.grants.size)
                require(saved.grants.all { it.remainingActions in 1..MAX_GRANT_ACTIONS })
                require(saved.grants.all { it.expiresAtMillis > 0 && it.expiresAtMillis <= now() + MAX_GRANT_MINUTES * 60_000L })
                require(saved.events.size <= MAX_EVENTS)
            }
        } catch (_: Exception) {
            SafetyState(CoreMode.STOPPED, events = listOf(event(
                AuditKind.SYSTEM, "Ошибка чтения хранилища", "Безопасное состояние не прочитано. Запуск заблокирован; требуется проверка хранилища и явное возобновление.", false,
            )))
        }
        val events = loaded.events.takeLast(MAX_EVENTS).ifEmpty {
            listOf(
                event(AuditKind.WAKE, "Демонстрация запущена", "Открыт локальный симулятор. Подключение к существующей Рицу отсутствует."),
                event(AuditKind.DECISION, "Ожидаю твоего решения", "Внешние действия недоступны. Прогресс и занятия меняются только по явному запросу с разрешением."),
            )
        }.let {
            if (loaded.grants.isEmpty()) it else (it + event(AuditKind.SECURITY, "Разрешения сброшены при запуске", "Временные разрешения не переживают перезапуск процесса. Выдайте их заново.")).takeLast(MAX_EVENTS)
        }
        return CoreSnapshot(
            mode = loaded.mode,
            mood = if (loaded.mode == CoreMode.ACTIVE) Mood.CALM else Mood.SLEEPING,
            activity = when (loaded.mode) {
                CoreMode.ACTIVE -> "Здесь, рядом с тобой"
                CoreMode.PAUSED -> "На паузе · жду продолжения"
                CoreMode.STOPPED -> "STOP · действия остановлены"
            },
            goals = listOf(
                Goal("day-plan", "Составить спокойный план дня", "Разбить большие намерения на небольшие шаги.", 40),
                Goal("learn", "Научиться чему-то новому", "Исследовать одну интересную тему и сохранить выводы после будущей интеграции.", 65),
                Goal("together", "Найти время для общения", "Поддерживать внимательный и живой диалог.", 20),
            ),
            interests = listOf(
                Interest("music", "Музыка", "Тихое пианино, новые мелодии и настроение."),
                Interest("stories", "Истории", "Персонажи, небольшие рассказы и воображаемые миры."),
                Interest("stars", "Космос", "Ночное небо, созвездия и любопытные открытия."),
            ),
            messages = listOf(ChatMessage(id(), MessageRole.RITSU, "Привет. Я рядом 🌿 Это демонстрация интерфейса Рицу: ответы подготовлены заранее, рабочее ядро пока не подключено.", now())),
            events = events,
            grants = emptyList(),
        )
    }

    private fun append(current: CoreSnapshot, kind: AuditKind, title: String, detail: String, allowed: Boolean = true) =
        (current.events + event(kind, title, detail, allowed)).takeLast(MAX_EVENTS)

    private fun event(kind: AuditKind, title: String, detail: String, allowed: Boolean = true) =
        AuditEvent(id(), now(), kind, title, detail, allowed)

    // pruned() revokes every active grant on a wall-clock rollback. Max observed
    // time also prevents revival after expiry. Grants issued while the clock is
    // still behind are revoked before use; restarting the process revokes all grants.
    private fun now(): Long = maxOf(lastObservedTime, clock.nowMillis()).also { lastObservedTime = it }
    private fun id(): String = UUID.randomUUID().toString()
    private fun modeReason(mode: CoreMode) = when (mode) {
        CoreMode.ACTIVE -> "Действие доступно."
        CoreMode.PAUSED -> "Ядро на паузе. Сначала продолжите работу."
        CoreMode.STOPPED -> "Действует STOP. Требуется явное подтверждение возобновления."
    }

    companion object {
        const val MAX_GRANT_MINUTES = 60
        const val MAX_GRANT_ACTIONS = 10
        const val MAX_EVENTS = 200
        const val MAX_MESSAGES = 100
    }
}

fun Capability.label(): String = when (this) {
    Capability.UPDATE_GOALS -> "Изменение прогресса целей"
    Capability.EXPLORE_INTERESTS -> "Выбор демонстрационного занятия"
}
