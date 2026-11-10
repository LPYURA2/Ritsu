package dev.ritsu.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import dev.ritsu.android.RitsuViewModel
import dev.ritsu.core.CoreMode
import dev.ritsu.core.CoreSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class Destination(val route: String, val label: String, val icon: ImageVector)
val destinations = listOf(
    Destination("home", "Главная", Icons.Outlined.Home),
    Destination("chat", "Диалог", Icons.Outlined.ChatBubbleOutline),
    Destination("goals", "Цели", Icons.Outlined.TrackChanges),
    Destination("permissions", "Доступ", Icons.Outlined.Shield),
    Destination("history", "Журнал", Icons.Outlined.History),
    Destination("system", "Система", Icons.Outlined.Tune),
)

@Composable
fun RitsuApp(snapshot: CoreSnapshot, vm: RitsuViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val selected = entry?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(vm) { vm.notices.collect { snackbar.showSnackbar(it, withDismissAction = true) } }
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                withContext(Dispatchers.IO) { vm.refreshSilently() }
                delay(1_000)
            }
        }
    }
    val navigate: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).background(Accent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("RITSU", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 3.sp)
                        Text("ПРОСТРАНСТВО РИЦУ", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    Pill("ДЕМО · 01", Olive)
                    IconButton(onClick = vm::stop, modifier = Modifier.testTag("global_stop")) {
                        Icon(Icons.Outlined.StopCircle, "Остановить Рицу", tint = Danger)
                    }
                }
                if (snapshot.mode != CoreMode.ACTIVE) {
                    Surface(onClick = { navigate("permissions") }, color = if (snapshot.mode == CoreMode.STOPPED) Danger.copy(alpha = .08f) else Accent.copy(alpha = .08f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (snapshot.mode == CoreMode.STOPPED) "STOP · действия остановлены" else "Пауза · действия приостановлены", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Открыть управление доступом", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Canvas, tonalElevation = 0.dp) {
                destinations.forEach { item ->
                    NavigationBarItem(
                        selected = selected == item.route,
                        onClick = { navigate(item.route) },
                        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_${item.route}"),
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Accent, selectedTextColor = Accent, indicatorColor = Accent.copy(alpha = .11f), unselectedIconColor = Muted, unselectedTextColor = Muted),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                composable("home") { HomeScreen(snapshot, navigate) }
                composable("chat") { ChatScreen(snapshot, vm) }
                composable("goals") { GoalsScreen(snapshot, vm) }
                composable("permissions") { PermissionsScreen(snapshot, vm) }
                composable("history") { HistoryScreen(snapshot, vm) }
                composable("system") { SystemScreen(snapshot, vm) }
            }
        }
    }
}
