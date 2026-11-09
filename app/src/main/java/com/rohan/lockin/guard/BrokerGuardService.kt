package com.rohan.lockin.guard

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.rohan.lockin.domain.BrokerBlock
import com.rohan.lockin.domain.BrokerGuard
import com.rohan.lockin.haptics.Haptics
import com.rohan.lockin.lockInApp
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Watches which app is in front. When Kite opens while a lockout, cooldown, closed session,
 * no-trade day or the 10:30–11:15 window is active, it covers Kite with a block screen.
 * It never reads, taps or changes anything inside Kite.
 */
class BrokerGuardService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var shownBlock: BrokerBlock? = null
    private var brokerInFront = false

    private val periodicCheck = object : Runnable {
        override fun run() {
            if (brokerInFront) evaluate()
            handler.postDelayed(this, RECHECK_MS)
        }
    }

    override fun onServiceConnected() {
        handler.postDelayed(periodicCheck, RECHECK_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        when {
            pkg in BrokerGuard.BROKER_PACKAGES -> {
                brokerInFront = true
                evaluate()
            }
            pkg == packageName || isTransientUi(pkg) -> Unit
            else -> {
                brokerInFront = false
                hideOverlay()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }

    private fun evaluate() {
        scope.launch {
            val state = lockInApp.container.store.state.first()
            val block = BrokerGuard.blockFor(state, System.currentTimeMillis(), LocalDateTime.now())
            when {
                block == null -> hideOverlay()
                brokerInFront -> showOverlay(block, state.exitPassesToday)
            }
        }
    }

    private fun showOverlay(block: BrokerBlock, passesUsed: Int) {
        if (overlay != null && shownBlock == block) return
        hideOverlay()
        val view = BlockOverlay.build(this, block, passesUsed, onHome = ::goHome, onExitPass = ::grantExitPass)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        getSystemService(WindowManager::class.java)?.addView(view, params)
        overlay = view
        shownBlock = block
        Haptics.alarm(this)
    }

    private fun hideOverlay() {
        overlay?.let { getSystemService(WindowManager::class.java)?.removeView(it) }
        overlay = null
        shownBlock = null
    }

    private fun goHome() {
        brokerInFront = false
        hideOverlay()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun grantExitPass() {
        scope.launch {
            lockInApp.container.store.update { BrokerGuard.grantExitPass(it, System.currentTimeMillis()) }
            hideOverlay()
            handler.postDelayed({ if (brokerInFront) evaluate() }, BrokerGuard.EXIT_PASS_MIN * 60_000L + 1_000L)
        }
    }

    /** Notification shade, keyboards and similar don't mean the user left Kite. */
    private fun isTransientUi(pkg: String): Boolean =
        pkg == "com.android.systemui" || "inputmethod" in pkg || "keyboard" in pkg

    companion object {
        private const val RECHECK_MS = 30_000L

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val component = ComponentName(context, BrokerGuardService::class.java)
            val names = setOf(component.flattenToString(), component.flattenToShortString())
            return enabled.split(':').any { entry -> names.any { it.equals(entry, ignoreCase = true) } }
        }
    }
}
