package com.rohan.lockin

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Line
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.Surface1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rohan.lockin.notify.Notifications
import com.rohan.lockin.ui.LockInViewModel
import com.rohan.lockin.ui.Overlay
import com.rohan.lockin.ui.overlays.AuditDialog
import com.rohan.lockin.ui.overlays.BreathingScreen
import com.rohan.lockin.ui.overlays.GroundingScreen
import com.rohan.lockin.ui.overlays.TiltDialog
import com.rohan.lockin.ui.screens.HistoryScreen
import com.rohan.lockin.ui.screens.HomeScreen
import com.rohan.lockin.ui.screens.SettingsScreen
import com.rohan.lockin.ui.screens.SmokeScreen
import com.rohan.lockin.ui.theme.Black0
import com.rohan.lockin.ui.theme.LockInTheme

class MainActivity : ComponentActivity() {

    private val openAudit = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        openAudit.value = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_AUDIT, false) == true
        setContent {
            LockInTheme { LockInRoot(openAudit) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Notifications.EXTRA_OPEN_AUDIT, false)) openAudit.value = true
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("trade", "Trade", Icons.Filled.Home),
    Tab("smoke", "Smoke-free", Icons.Filled.Favorite),
    Tab("history", "History", Icons.Filled.DateRange),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

@Composable
private fun LockInRoot(openAudit: MutableState<Boolean>) {
    val vm: LockInViewModel = viewModel(factory = LockInViewModel.Factory)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    NotificationPermissionRequest(onGranted = vm::refreshStatus)

    Box {
        Scaffold(
            containerColor = Black0,
            bottomBar = {
                PillNavBar(current) { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        ) { padding ->
            NavHost(nav, startDestination = "trade", modifier = Modifier.padding(padding)) {
                composable("trade") { HomeScreen(ui, vm) }
                composable("smoke") { SmokeScreen(ui, vm) }
                composable("history") { HistoryScreen(ui) }
                composable("settings") { SettingsScreen(ui, vm) }
            }
        }

        if (openAudit.value && !ui.isLocked) {
            AuditDialog(
                onSubmit = { oneLot, sl, calm ->
                    openAudit.value = false
                    vm.submitAudit(oneLot, sl, calm)
                },
                onDismiss = { openAudit.value = false },
            )
        }
        if (ui.isLocked) TiltDialog(ui, onBreathe = vm::startBreathing)
        when (ui.overlay) {
            Overlay.Breathing -> BreathingScreen(onDone = vm::closeOverlay)
            Overlay.Grounding -> GroundingScreen(onDone = vm::closeOverlay)
            Overlay.None -> Unit
        }
    }
}

/** Floating pill navigation: hairline capsule, selected tab gets an emerald tint. */
@Composable
private fun PillNavBar(current: String?, onSelect: (String) -> Unit) {
    val outer = RoundedCornerShape(26.dp)
    val inner = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(outer)
            .background(Surface1)
            .border(1.dp, Line, outer)
            .padding(6.dp),
    ) {
        TABS.forEach { tab ->
            val selected = current == tab.route
            val tint = if (selected) Emerald else Muted
            Column(
                Modifier
                    .weight(1f)
                    .clip(inner)
                    .background(if (selected) Emerald.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(tab.route) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(20.dp))
                Text(tab.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}

@Composable
private fun NotificationPermissionRequest(onGranted: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    LaunchedEffect(Unit) {
        if (!Notifications.canPost(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
