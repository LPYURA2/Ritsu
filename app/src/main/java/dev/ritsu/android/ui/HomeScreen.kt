package dev.ritsu.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ritsu.android.avatar.AvatarMood
import dev.ritsu.android.avatar.RitsuAvatar
import dev.ritsu.core.*

fun CoreMode.displayName() = when (this) {
    CoreMode.ACTIVE -> "Активна"
    CoreMode.PAUSED -> "На паузе"
    CoreMode.STOPPED -> "Остановлена"
}

fun Mood.displayName() = when (this) {
    Mood.CALM -> "Спокойствие"
    Mood.HAPPY -> "Радость"
    Mood.THINKING -> "Сосредоточенность"
    Mood.SLEEPING -> "Отдых"
    Mood.CONCERNED -> "Внимание"
}

@Composable
fun HomeScreen(snapshot: CoreSnapshot, onNavigate: (String) -> Unit) {
    val mood = AvatarMood.valueOf(snapshot.mood.name)
    Column(Modifier.fillMaxSize().testTag("screen_home").verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Рядом.\nВ своём ритме.", style = MaterialTheme.typography.headlineLarge)
            Text("01 / 06", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(bottom = 5.dp))
        }
        Box(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFFEAE6EE), Color(0xFFF0ECE5))))) {
            // Original vector avatar; expressions are projections of the current core snapshot.
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = .35f), radius = size.width * .38f, center = center.copy(y = size.height * .52f))
                drawCircle(Accent.copy(alpha = .1f), radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .86f, size.height * .32f))
            }
            RitsuAvatar(mood, Modifier.align(Alignment.BottomCenter).fillMaxWidth(.95f).height(315.dp), animate = snapshot.mode != CoreMode.STOPPED)
            Column(Modifier.align(Alignment.TopStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("РИЦУ", style = MaterialTheme.typography.labelSmall, color = Accent)
                Text("ひとつの心", fontFamily = FontFamily.Serif, fontSize = 12.sp, color = Muted)
            }
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0xFFFCFBF7).copy(alpha = .88f)).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbTwilight, null, tint = Accent, modifier = Modifier.size(20.dp))
                Text(snapshot.activity, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 9.dp).weight(1f))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                Pill(snapshot.mode.displayName(), if (snapshot.mode == CoreMode.STOPPED) Danger else Olive)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = Color(0xFFEEEDE5)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Outlined.Spa, null, tint = Olive, modifier = Modifier.size(20.dp))
                    Text("Настроение", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(snapshot.mood.displayName(), style = MaterialTheme.typography.labelLarge)
                }
            }
            Surface(onClick = { onNavigate("goals") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = Color(0xFFEEEAF1)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Outlined.TrackChanges, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Text("Маленькие шаги", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("${snapshot.goals.count { it.progress < 100 }} цели в пути →", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Button(onClick = { onNavigate("chat") }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("home_chat"), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Поговорить с Рицу")
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
        Text("Демонстрация интерфейса · рабочее ядро не подключено", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
