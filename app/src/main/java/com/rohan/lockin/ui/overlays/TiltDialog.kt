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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.lockin.domain.mmss
import com.rohan.lockin.ui.UiState
import com.rohan.lockin.ui.components.GhostButton
import com.rohan.lockin.ui.components.MonoNumber
import com.rohan.lockin.ui.components.PrimaryButton
import com.rohan.lockin.ui.components.StatusChip
import com.rohan.lockin.ui.components.blockTouches
import com.rohan.lockin.ui.theme.Crimson
import com.rohan.lockin.ui.theme.LockRed
import com.rohan.lockin.ui.theme.Muted
import com.rohan.lockin.ui.theme.OnBg

const val HELPLINE_NUMBER = "14416"

/** Systemic lockout: full screen, back blocked, persisted lockUntil so it survives app kills. */
@Composable
fun TiltDialog(ui: UiState, onBreathe: () -> Unit) {
    val context = LocalContext.current
    val reason = ui.lockReason
    BackHandler(enabled = true) {}
    Box(
        Modifier
            .fillMaxSize()
            .background(LockRed)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Crimson.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.35f),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            }
            .blockTouches()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusChip("Locked", Crimson, live = true)
            Text(reason.headline, style = MaterialTheme.typography.headlineMedium, color = Crimson, textAlign = TextAlign.Center)
            Text(reason.detail, style = MaterialTheme.typography.bodyLarge, color = OnBg, textAlign = TextAlign.Center)
            MonoNumber(mmss(ui.lockRemainingMs), color = OnBg, size = 80.sp)
            Text(
                "This screen cannot be dismissed. Kite is blocked too; use its exit-only pass to close a position.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                "Start breathing · 3 min",
                onClick = onBreathe,
                modifier = Modifier.fillMaxWidth(),
                container = Crimson,
                contentColor = OnBg,
            )
            GhostButton(
                "Need to talk? Tele-MANAS $HELPLINE_NUMBER",
                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$HELPLINE_NUMBER"))) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
