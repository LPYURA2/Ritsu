package dev.ritsu.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.ritsu.android.RitsuViewModel
import dev.ritsu.core.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatScreen(snapshot: CoreSnapshot, vm: RitsuViewModel) {
    var draft by rememberSaveable { mutableStateOf("") }
    var voiceInfo by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val active = snapshot.mode == CoreMode.ACTIVE
    val send = {
        if (draft.isNotBlank() && draft.length <= 2000 && active) {
            vm.sendMessage(draft)
            draft = ""
        }
    }
    LaunchedEffect(snapshot.messages.size) {
        if (snapshot.messages.isNotEmpty()) list.animateScrollToItem(snapshot.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().testTag("screen_chat").imePadding().padding(horizontal = 24.dp).padding(top = 8.dp)) {
        SectionHeading("Есть о чём поговорить", "Текстовый диалог · демонстрационные ответы")
        Spacer(Modifier.height(12.dp))
        LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth().testTag("chat_messages"), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(snapshot.messages, key = { it.id }) { message ->
                val user = message.role == MessageRole.USER
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
                    Text(if (user) "ВЫ" else "РИЦУ · ДЕМО", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 5.dp))
                    Surface(shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = if (user) 4.dp else 20.dp, bottomStart = if (user) 20.dp else 4.dp), color = if (user) Color(0xFFEAE5F0) else Color(0xFFEDEDE5), modifier = Modifier.widthIn(max = 480.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            Text(DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(message.timestampMillis)), style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                }
            }
        }
        if (!active) {
            Text("${snapshot.mode.displayName()}. Возобновите действия в разделе «Доступ».", style = MaterialTheme.typography.bodySmall, color = Danger, modifier = Modifier.padding(vertical = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
            OutlinedTextField(value = draft, onValueChange = { if (it.length <= 2000) draft = it }, modifier = Modifier.weight(1f).testTag("chat_input"),
                placeholder = { Text("Написать Рицу…") }, shape = RoundedCornerShape(20.dp), maxLines = 4, enabled = active,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                trailingIcon = { IconButton(onClick = { voiceInfo = true }, modifier = Modifier.testTag("voice_info")) { Icon(Icons.Outlined.MicNone, "О голосовом общении", tint = Muted) } })
            FilledIconButton(onClick = { send() }, enabled = active && draft.isNotBlank(), modifier = Modifier.size(48.dp).testTag("chat_send")) {
                Icon(Icons.Outlined.ArrowUpward, "Отправить сообщение")
            }
        }
    }
    if (voiceInfo) AlertDialog(onDismissRequest = { voiceInfo = false }, icon = { Icon(Icons.Outlined.MicNone, null) }, title = { Text("Голос — следующий этап") },
        text = { Text("Место для голосового общения подготовлено. Запись и распознавание пока не подключены; разрешение микрофона не запрашивается.") },
        confirmButton = { TextButton(onClick = { voiceInfo = false }) { Text("Понятно") } })
}
