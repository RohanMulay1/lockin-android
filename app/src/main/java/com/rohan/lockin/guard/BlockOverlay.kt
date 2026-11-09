package com.rohan.lockin.guard

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.rohan.lockin.R
import com.rohan.lockin.domain.BrokerBlock
import com.rohan.lockin.domain.BrokerGuard

/** Plain-View block screen drawn over Kite (Compose needs lifecycle owners a service doesn't have). */
object BlockOverlay {

    private const val BG = 0xFA0D0505.toInt()
    private const val CRIMSON = 0xFFEF4444.toInt()
    private const val CRIMSON_TINT = 0x33EF4444
    private const val WHITE = 0xFFFAFAFA.toInt()
    private const val MUTED = 0xFF8F8F8F.toInt()
    private const val LINE = 0xFF2A2A2A.toInt()
    private const val PANEL = 0xFF111111.toInt()

    fun build(
        context: Context,
        block: BrokerBlock,
        passesUsed: Int,
        onHome: () -> Unit,
        onExitPass: () -> Unit,
    ): View {
        val dp = context.resources.displayMetrics.density
        val mono = ResourcesCompat.getFont(context, R.font.geist_mono_semibold) ?: Typeface.MONOSPACE
        val sans = ResourcesCompat.getFont(context, R.font.geist_regular) ?: Typeface.DEFAULT
        val sansBold = ResourcesCompat.getFont(context, R.font.geist_semibold) ?: Typeface.DEFAULT_BOLD
        val pad = (28 * dp).toInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(BG)
            setPadding(pad, pad, pad, pad)
            addView(chip(context, "● KITE LOCKED", mono, dp))
            addView(label(context, block.title, 26f, CRIMSON, sansBold, topDp = 20, dp = dp))
            addView(label(context, block.detail, 16f, WHITE, sans, topDp = 10, dp = dp))
            addView(action(context, "GO HOME", mono, filled = true, dp = dp, onClick = onHome))
            addView(
                action(
                    context,
                    "EXIT-ONLY PASS · ${BrokerGuard.EXIT_PASS_MIN} MIN · USED $passesUsed",
                    mono,
                    filled = false,
                    dp = dp,
                    onClick = onExitPass,
                ),
            )
            addView(label(context, "The pass is only for closing an open position. No new entries.", 13f, MUTED, sans, topDp = 14, dp = dp))
        }
    }

    private fun chip(context: Context, text: String, face: Typeface, dp: Float) = TextView(context).apply {
        this.text = text
        typeface = face
        textSize = 12f
        letterSpacing = 0.12f
        setTextColor(CRIMSON)
        setPadding((14 * dp).toInt(), (7 * dp).toInt(), (14 * dp).toInt(), (7 * dp).toInt())
        background = GradientDrawable().apply {
            cornerRadius = 100 * dp
            setColor(CRIMSON_TINT)
            setStroke((1 * dp).toInt(), CRIMSON)
        }
    }

    private fun label(context: Context, text: String, sizeSp: Float, color: Int, face: Typeface, topDp: Int, dp: Float) =
        TextView(context).apply {
            this.text = text
            textSize = sizeSp
            typeface = face
            setTextColor(color)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = (topDp * dp).toInt() }
        }

    private fun action(context: Context, text: String, face: Typeface, filled: Boolean, dp: Float, onClick: () -> Unit) =
        TextView(context).apply {
            this.text = text
            typeface = face
            textSize = 13f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setTextColor(if (filled) WHITE else WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 16 * dp
                setColor(if (filled) CRIMSON else PANEL)
                setStroke((1 * dp).toInt(), if (filled) CRIMSON else LINE)
            }
            isClickable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (56 * dp).toInt())
                .apply { topMargin = (if (filled) 28 else 12).let { (it * dp).toInt() } }
        }
}
