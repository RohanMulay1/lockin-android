package com.rohan.lockin.ui.overlays

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.mmss
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.blockTouches
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.LockRed

const val HELPLINE_NUMBER = "14416"

/** Systemic lockout: full screen, back blocked, persisted lockUntil so it survives app kills. */
@Composable
fun TiltDialog(ui: UiState, onBreathe: () -> Unit) {
    val context = LocalContext.current
    val reason = ui.lockReason
    BackHandler(enabled = true) {}
    Box(
        Modifier.fillMaxSize().background(LockRed).blockTouches(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Crimson, modifier = Modifier.size(64.dp))
            Text(reason.headline, color = Crimson, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(reason.detail, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            MonoNumber(mmss(ui.lockRemainingMs), color = Color.White, size = 56.sp)
            Text(
                "This screen cannot be dismissed. Trading is closed for the rest of the day.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onBreathe,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Color.White),
            ) {
                Text("START BREATHING (3 MIN)", fontWeight = FontWeight.Black)
            }
            OutlinedButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$HELPLINE_NUMBER"))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Need to talk? Tele-MANAS $HELPLINE_NUMBER", color = Color.White)
            }
        }
    }
}
