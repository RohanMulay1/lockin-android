package com.rohan.lockin.guard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.rohan.lockin.domain.BrokerBlock
import com.rohan.lockin.domain.BrokerGuard

/** Plain-View block screen drawn over Kite (Compose needs lifecycle owners a service doesn't have). */
object BlockOverlay {

    private const val BG = 0xF71A0505.toInt()
    private const val CRIMSON = 0xFFEF4444.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val MUTED = 0xB3FFFFFF.toInt()
    private const val DARK = 0xFF2A2A2A.toInt()

    fun build(
        context: Context,
        block: BrokerBlock,
        passesUsed: Int,
        onHome: () -> Unit,
        onExitPass: () -> Unit,
    ): View {
        val density = context.resources.displayMetrics.density
        val pad = (28 * density).toInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(BG)
            setPadding(pad, pad, pad, pad)
            addView(label(context, "🔒 KITE IS LOCKED", 30f, CRIMSON, bold = true))
            addView(label(context, block.title, 22f, WHITE, bold = true))
            addView(label(context, block.detail, 16f, MUTED, bold = false))
            addView(button(context, "GO HOME", CRIMSON, density, onHome))
            addView(
                button(
                    context,
                    "EXIT-ONLY PASS (${BrokerGuard.EXIT_PASS_MIN} MIN) · used today: $passesUsed",
                    DARK,
                    density,
                    onExitPass,
                ),
            )
            addView(label(context, "The pass is only for closing an open position. No new entries.", 13f, MUTED, bold = false))
        }
    }

    private fun label(context: Context, text: String, sizeSp: Float, color: Int, bold: Boolean) =
        TextView(context).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 12, 0, 12)
        }

    private fun button(context: Context, text: String, color: Int, density: Float, onClick: () -> Unit) =
        Button(context).apply {
            this.text = text
            setTextColor(WHITE)
            backgroundTintList = ColorStateList.valueOf(color)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * density).toInt(),
            ).apply { topMargin = (16 * density).toInt() }
        }
}
