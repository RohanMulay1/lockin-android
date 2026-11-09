package com.rohan.lockin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.UpdateState
import com.rohan.lockin.ui.components.Eyebrow
import com.rohan.lockin.ui.components.GhostButton
import com.rohan.lockin.ui.components.KeyValueRow
import com.rohan.lockin.ui.components.Panel
import com.rohan.lockin.ui.components.PrimaryButton
import com.rohan.lockin.ui.components.ProgressRail
import com.rohan.lockin.ui.components.StatusChip
import com.rohan.lockin.ui.theme.Amber
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

/** Compact banner on the Trade tab when a newer release exists. */
@Composable
fun UpdateBanner(update: UpdateState, onInstall: () -> Unit) {
    Panel(accent = Emerald, glow = true) {
        when (update) {
            is UpdateState.Available -> {
                Eyebrow("Update available · ${update.release.tag}", color = Emerald)
                Text("A newer LockIn is ready. Your data stays.", style = MaterialTheme.typography.bodyMedium, color = OnBg)
                PrimaryButton("Download & install", onClick = onInstall, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Downloading -> DownloadProgress(update)
            UpdateState.Idle, UpdateState.Checking, UpdateState.UpToDate, is UpdateState.Failed -> Unit
        }
    }
}

/** Full update controls for the Settings tab. */
@Composable
fun UpdatesPanel(ui: UiState, onCheck: () -> Unit, onInstall: () -> Unit) {
    val update = ui.update
    Panel {
        Row(Modifier.fillMaxWidth()) {
            Eyebrow("App updates", Modifier.weight(1f))
            when (update) {
                is UpdateState.Available -> StatusChip("New ${update.release.tag}", Emerald, live = true)
                is UpdateState.Downloading -> StatusChip("Downloading", Amber, live = true)
                UpdateState.UpToDate -> StatusChip("Up to date", Emerald)
                UpdateState.Checking -> StatusChip("Checking", Muted, live = true)
                is UpdateState.Failed -> StatusChip("Offline", Crimson)
                UpdateState.Idle -> Unit
            }
        }
        KeyValueRow("Installed", "v1.1.${ui.installedVersion}")
        if (update is UpdateState.Failed) Text(update.message, style = MaterialTheme.typography.bodySmall, color = Crimson)
        if (update is UpdateState.Downloading) DownloadProgress(update)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Check now", onClick = onCheck, modifier = Modifier.weight(1f))
            if (update is UpdateState.Available) PrimaryButton("Install", onClick = onInstall, modifier = Modifier.weight(1f))
        }
        Text(
            "First install asks you to allow LockIn to install apps. Updates keep all your data.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

@Composable
private fun DownloadProgress(update: UpdateState.Downloading) {
    Eyebrow("Downloading ${update.release.tag} · ${(update.progress * 100).toInt()}%", color = Amber)
    ProgressRail(update.progress, Amber)
}
