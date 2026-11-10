package dev.ritsu.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Canvas = Color(0xFFF7F5F0)
val Ink = Color(0xFF252B32)
val Muted = Color(0xFF747771)
val Accent = Color(0xFF73658D)
val Olive = Color(0xFF6B7859)
val Line = Color(0xFFE4E2DA)
val Danger = Color(0xFFAB4C4C)

@Composable
fun RitsuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent, onPrimary = Color.White,
            primaryContainer = Color(0xFFEAE5F0), onPrimaryContainer = Ink,
            secondary = Olive, secondaryContainer = Color(0xFFE8EBDD),
            background = Canvas, onBackground = Ink,
            surface = Canvas, onSurface = Ink, surfaceVariant = Color(0xFFEFEDE6),
            onSurfaceVariant = Muted, outline = Muted, outlineVariant = Line,
            error = Danger,
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 40.sp, color = Ink),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 36.sp, color = Ink),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            labelSmall = TextStyle(fontSize = 10.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Medium),
        ),
        content = content,
    )
}

@Composable
fun RitsuCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color(0xFFFCFBF7), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
fun Pill(text: String, color: Color = Accent) {
    Text(text, color = color, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.background(color.copy(alpha = 0.09f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp))
}

@Composable
fun LabelValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
