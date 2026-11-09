package com.rohan.lockin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RiskEngineTest {

    private fun snap(pnl: Int = 0, peak: Int = 0, trades: Int = 0, losses: Int = 0) =
        SessionSnapshot(pnl = pnl, peakPnl = peak, tradesToday = trades, consecutiveLosses = losses)

    @Test
    fun `loss just inside 1R is clear`() {
        assertEquals(Verdict.Clear, RiskEngine.evaluate(snap(pnl = -1_999)))
    }

    @Test
    fun `loss of exactly 1R locks out with max drawdown`() {
        assertEquals(Verdict.Lockout(LockReason.MAX_DRAWDOWN), RiskEngine.evaluate(snap(pnl = -2_000)))
    }

    @Test
    fun `trade count at cap is clear and above cap locks out`() {
        assertEquals(Verdict.Clear, RiskEngine.evaluate(snap(trades = TRADE_CAP)))
        assertEquals(Verdict.Lockout(LockReason.TRADE_CAP), RiskEngine.evaluate(snap(trades = TRADE_CAP + 1)))
    }

    @Test
    fun `giving back half of a protected peak locks out`() {
        assertEquals(Verdict.Clear, RiskEngine.evaluate(snap(pnl = 2_001, peak = 4_000)))
        assertEquals(Verdict.Lockout(LockReason.GIVE_BACK), RiskEngine.evaluate(snap(pnl = 2_000, peak = 4_000)))
    }

    @Test
    fun `small peaks are not protected by give-back rule`() {
        assertEquals(Verdict.Clear, RiskEngine.evaluate(snap(pnl = 0, peak = 999)))
        assertNull(RiskEngine.giveBackFloor(999))
        assertEquals(500, RiskEngine.giveBackFloor(1_000))
    }

    @Test
    fun `two consecutive losses trigger cooldown`() {
        assertEquals(Verdict.Clear, RiskEngine.evaluate(snap(losses = 1)))
        assertEquals(Verdict.Cooldown, RiskEngine.evaluate(snap(losses = 2)))
    }

    @Test
    fun `drawdown takes priority over cooldown`() {
        assertEquals(Verdict.Lockout(LockReason.MAX_DRAWDOWN), RiskEngine.evaluate(snap(pnl = -2_500, losses = 3)))
    }

    @Test
    fun `allowed sl points keep a full loss within 1R`() {
        assertEquals(30.77, RiskEngine.allowedSlPoints(ONE_R, DEFAULT_NIFTY_LOT), 0.01)
        assertEquals(100.0, RiskEngine.allowedSlPoints(ONE_R, DEFAULT_SENSEX_LOT), 0.0)
        assertEquals(0.0, RiskEngine.allowedSlPoints(ONE_R, 0), 0.0)
    }
}
