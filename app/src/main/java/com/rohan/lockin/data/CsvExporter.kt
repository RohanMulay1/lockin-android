package com.rohan.lockin.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.rohan.lockin.data.db.DailyLogEntity
import com.rohan.lockin.data.db.TradeEntity
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Writes the history to a CSV in the cache dir and opens the share sheet (for the weekly review). */
object CsvExporter {

    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun share(context: Context, logs: List<DailyLogEntity>, trades: List<TradeEntity>) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "lockin-${LocalDate.now()}.csv")
        file.writeText(buildCsv(logs, trades))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Export LockIn log"))
    }

    fun buildCsv(logs: List<DailyLogEntity>, trades: List<TradeEntity>): String {
        val dayRows = logs.joinToString("\n") {
            "${LocalDate.ofEpochDay(it.epochDay)},${it.pnl},${it.trades},${it.violations},${it.lockouts},${it.cigarettes},${it.clean}"
        }
        val tradeRows = trades.joinToString("\n") {
            "${LocalDate.ofEpochDay(it.epochDay)},${fmt(it.entryTs)},${fmt(it.exitTs)},${it.holdMinutes},${it.pnl}"
        }
        return "date,pnl,trades,violations,lockouts,cigarettes,clean\n$dayRows\n\n" +
            "trade_date,entry,exit,hold_min,pnl\n$tradeRows\n"
    }

    private fun fmt(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(timeFormat)
}
