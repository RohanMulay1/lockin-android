package com.rohan.lockin.ui.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.rohan.lockin.ui.theme.Emerald
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.Surface1
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.rohan.lockin.ui.components.ToggleRow

/** Opened from the hourly notification. Any "no" sends the user into the tilt protocol. */
@Composable
fun AuditDialog(onSubmit: (oneLot: Boolean, slPlaced: Boolean, calm: Boolean) -> Unit, onDismiss: () -> Unit) {
    var oneLot by remember { mutableStateOf(true) }
    var slPlaced by remember { mutableStateOf(true) }
    var calm by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Hourly tilt & execution audit", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleRow("Sticking to 1 lot", oneLot) { oneLot = it }
                ToggleRow("SL order placed on every trade", slPlaced) { slPlaced = it }
                ToggleRow("Calm — no revenge impulse", calm) { calm = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(oneLot, slPlaced, calm) }) { Text("SUBMIT", color = Emerald) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("LATER", color = Muted) } },
    )
}
