package dev.ritsu.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ritsu.android.RitsuViewModel
import dev.ritsu.core.AuditKind
import dev.ritsu.core.Capability
import dev.ritsu.core.CoreMode
import dev.ritsu.core.CoreSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(snapshot: CoreSnapshot, vm: RitsuViewModel) {
    ManagementColumn("screen_goals") {
        SectionHeading("Маленькие шаги", "Демонстрационные цели и интересы")
        Pill("${snapshot.goals.count { it.progress == 100 }} из ${snapshot.goals.size} завершено", Olive)
        snapshot.goals.forEach { goal ->
            RitsuCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(goal.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text("${goal.progress}%", color = Accent, fontWeight = FontWeight.SemiBold)
                }
                Text(goal.description, color = Muted, style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = { goal.progress / 100f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = Olive,
                    trackColor = Line,
                )
                OutlinedButton(
                    onClick = { vm.advanceGoal(goal.id) },
                    enabled = goal.progress < 100,
                    modifier = Modifier.fillMaxWidth().testTag("goal_${goal.id}"),
                ) { Text(if (goal.progress < 100) "Выполнить демо-шаг" else "Завершено") }
            }
        }
        Text(
            "Для шага нужно временное разрешение «Изменение целей». Каждый шаг расходует одно действие; решение принимает демонстрационный шлюз.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        SectionHeading("Что мне интересно")
        snapshot.interests.forEach { interest ->
            RitsuCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Accent, modifier = Modifier.size(18.dp))
                    Text(interest.title, style = MaterialTheme.typography.titleMedium, color = Ink)
                }
                Text(interest.description, color = Muted, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { vm.exploreInterest(interest.id) }) { Text("Изучить в демо →") }
            }
        }
        Text("Изучение интересов требует отдельного разрешения. Внешних запросов и фоновых действий нет.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PermissionsScreen(snapshot: CoreSnapshot, vm: RitsuViewModel) {
    var duration by rememberSaveable { mutableIntStateOf(15) }
    var quota by rememberSaveable { mutableIntStateOf(3) }
    var confirmResume by rememberSaveable { mutableStateOf(false) }

    ManagementColumn("screen_permissions") {
        SectionHeading("Контроль в ваших руках", "Правила проверяет шлюз, а не экран")
        RitsuCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Shield, null, tint = if (snapshot.mode == CoreMode.STOPPED) Danger else Olive)
                Text(modeLabel(snapshot.mode), style = MaterialTheme.typography.titleMedium, color = Ink)
            }
            Text(
                when (snapshot.mode) {
                    CoreMode.ACTIVE -> "Демо активно. Действия с целями и интересами требуют ограниченных разрешений."
                    CoreMode.PAUSED -> "paused · диалог и действия приостановлены. Возобновление выполняется вручную."
                    CoreMode.STOPPED -> "STOP зафиксирован. Все разрешения отозваны. Самостоятельное возобновление запрещено."
                },
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = vm::stop,
                modifier = Modifier.fillMaxWidth().testTag("permissions_stop"),
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
            ) {
                Icon(Icons.Outlined.Stop, null, modifier = Modifier.size(18.dp))
                Text("STOP · остановить", Modifier.padding(start = 8.dp))
            }
            if (snapshot.mode == CoreMode.ACTIVE) {
                OutlinedButton(onClick = vm::pause, modifier = Modifier.fillMaxWidth().testTag("permissions_pause")) {
                    Icon(Icons.Outlined.Pause, null, modifier = Modifier.size(18.dp))
                    Text("Поставить на паузу", Modifier.padding(start = 8.dp))
                }
            } else {
                OutlinedButton(
                    onClick = { if (snapshot.mode == CoreMode.STOPPED) confirmResume = true else vm.resume() },
                    modifier = Modifier.fillMaxWidth().testTag("permissions_resume"),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Text("Возобновить демо", Modifier.padding(start = 8.dp))
                }
            }
        }
        Text("Управление относится только к демонстрации. Рабочая Рицу не подключена.", color = Muted, style = MaterialTheme.typography.bodySmall)
        SectionHeading("Временные разрешения", "Срок и число действий ограничены")
        RitsuCard {
            Text("Срок действия", style = MaterialTheme.typography.labelLarge, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 60).forEach { minutes ->
                    FilterChip(selected = duration == minutes, onClick = { duration = minutes }, label = { Text("$minutes мин") })
                }
            }
            Text("Лимит действий", style = MaterialTheme.typography.labelLarge, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 10).forEach { count ->
                    FilterChip(selected = quota == count, onClick = { quota = count }, label = { Text("$count") })
                }
            }
        }
        Capability.entries.forEach { capability ->
            val grant = snapshot.grants.firstOrNull { it.capability == capability }
            RitsuCard {
                Text(capabilityLabel(capability), style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    when (capability) {
                        Capability.UPDATE_GOALS -> "Разрешает один демонстрационный шаг цели за действие."
                        Capability.EXPLORE_INTERESTS -> "Разрешает демонстрационное изучение одного интереса за действие."
                    },
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (grant == null) {
                    Pill("Не выдано", Muted)
                } else {
                    Pill("Осталось: ${grant.remainingActions}", Olive)
                    Text("Истекает ${displayTime(grant.expiresAtMillis)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { vm.grant(capability, duration, quota) },
                        enabled = snapshot.mode == CoreMode.ACTIVE,
                        modifier = Modifier.testTag("grant_${capability.name}"),
                    ) { Text(if (grant == null) "Выдать" else "Переиздать") }
                    if (grant != null) {
                        TextButton(onClick = { vm.revoke(capability) }) { Text("Отозвать", color = Danger) }
                    }
                }
            }
        }
        Text("Команды Termux, произвольный код и неограниченные разрешения не предусмотрены. Выдача, расход и отказ записываются в журнал.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }

    if (confirmResume) {
        AlertDialog(
            onDismissRequest = { confirmResume = false },
            title = { Text("Снять STOP в демонстрации?") },
            text = { Text("Демо снова сможет отвечать в диалоге. Разрешения на действия останутся отозванными — их нужно выдать заново.") },
            confirmButton = {
                TextButton(onClick = { confirmResume = false; vm.resume(acknowledgeStop = true) }, modifier = Modifier.testTag("confirm_resume")) {
                    Text("Снять STOP")
                }
            },
            dismissButton = { TextButton(onClick = { confirmResume = false }) { Text("Отмена") } },
        )
    }
}

@Composable
fun HistoryScreen(snapshot: CoreSnapshot, vm: RitsuViewModel) {
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    val selectedKind = AuditKind.entries.firstOrNull { it.name == selectedFilter }
    val events = snapshot.events.filter { selectedKind == null || it.kind == selectedKind }.sortedByDescending { it.timestampMillis }
    ManagementColumn("screen_history") {
        SectionHeading("След каждого решения", "Локальный журнал демонстрации")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selectedFilter == "ALL", { selectedFilter = "ALL" }, label = { Text("Все") })
            AuditKind.entries.forEach { kind ->
                FilterChip(selectedFilter == kind.name, { selectedFilter = kind.name }, label = { Text(auditLabel(kind)) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${events.size} событий", color = Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = vm::refresh) { Text("Обновить") }
        }
        if (events.isEmpty()) {
            RitsuCard {
                Text("Здесь пока тихо", color = Ink, style = MaterialTheme.typography.titleMedium)
                Text("Для этого фильтра нет событий. Выберите «Все» или выполните действие в демо.", color = Muted)
            }
        }
        events.forEach { event ->
            RitsuCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Pill(auditLabel(event.kind), if (event.allowed) Accent else Danger)
                    Text(displayTime(event.timestampMillis), color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                Text(event.title, color = Ink, style = MaterialTheme.typography.titleMedium)
                Text(event.detail, color = Muted, style = MaterialTheme.typography.bodyMedium)
                if (!event.allowed) Text("Действие отклонено", color = Danger, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("События относятся к демо-данным. Журнал рабочей Рицу появится после отдельного этапа подключения.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SystemScreen(snapshot: CoreSnapshot, vm: RitsuViewModel) {
    ManagementColumn("screen_system") {
        SectionHeading("Одно ядро. Одна Рицу.", "Состояние приложения и граница подключения")
        RitsuCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CloudOff, null, tint = Accent)
                Text("Рабочее ядро не подключено", color = Ink, style = MaterialTheme.typography.titleMedium)
            }
            Text("Сейчас работает локальный набор демонстрационных данных. Он не вызывает ИИ-модель и не читает память Рицу.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Pill("DEMO · без сети", Accent)
            HorizontalDivider(color = Line)
            LabelValue("Источник", snapshot.connection.label)
            LabelValue("Идентификатор", snapshot.connection.coreId)
            LabelValue("Контракт", snapshot.connection.protocolVersion)
            LabelValue("Режим демо", modeLabel(snapshot.mode))
        }
        SectionHeading("Диагностика")
        RitsuCard {
            LabelValue("Платформа", "Android 11 и выше")
            LabelValue("Версия", "0.1.0-demo")
            LabelValue("Цели / интересы", "${snapshot.goals.size} / ${snapshot.interests.size}")
            LabelValue("Разрешения", snapshot.grants.size.toString())
            LabelValue("События в снимке", snapshot.events.size.toString())
            OutlinedButton(onClick = vm::refresh, modifier = Modifier.fillMaxWidth().testTag("system_refresh")) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Text("Обновить состояние", Modifier.padding(start = 8.dp))
            }
        }
        SectionHeading("Резервирование")
        RitsuCard {
            Text("Резервная копия принадлежит ядру", color = Ink, style = MaterialTheme.typography.titleMedium)
            Text("Приложение пока не имеет доступа к памяти, базам и конфигурации Рицу. Создание и восстановление копий будут согласованы вместе с интеграцией.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Недоступно до подключения") }
        }
        RitsuCard {
            Text("Будущее подключение", color = Ink, style = MaterialTheme.typography.titleMedium)
            Text("CoreGateway заменит демо-источник транспортом к существующему ядру. Личность, общая память, цели и проверка полномочий останутся в этом ядре.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text("Termux не получает произвольных команд из приложения.", color = Accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ManagementColumn(tag: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(tag).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

private fun modeLabel(mode: CoreMode): String = when (mode) {
    CoreMode.ACTIVE -> "Активна"
    CoreMode.PAUSED -> "На паузе"
    CoreMode.STOPPED -> "Остановлена · STOP"
}

private fun capabilityLabel(capability: Capability): String = when (capability) {
    Capability.UPDATE_GOALS -> "Изменение целей"
    Capability.EXPLORE_INTERESTS -> "Изучение интересов"
}

private fun auditLabel(kind: AuditKind): String = when (kind) {
    AuditKind.DECISION -> "Решения"
    AuditKind.WAKE -> "Пробуждения"
    AuditKind.ACTION -> "Действия"
    AuditKind.SECURITY -> "Разрешения"
    AuditKind.SYSTEM -> "Система"
}

private fun displayTime(timestampMillis: Long): String =
    DateTimeFormatter.ofPattern("dd.MM · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestampMillis))
