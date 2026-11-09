package com.rohan.lockin.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.rohan.lockin.domain.isTrustedDownloadUrl
import com.rohan.lockin.domain.versionCodeFromTag
import java.io.File
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

data class ReleaseInfo(val tag: String, val versionCode: Int, val apkUrl: String, val notes: String)

/** In-app updates from this repo's GitHub Releases (the app is sideloaded, so there is no Play Store). */
object UpdateChecker {

    private const val LATEST_RELEASE = "https://api.github.com/repos/RohanMulay1/lockin-android/releases/latest"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val APK_MIME = "application/vnd.android.package-archive"

    fun installedVersionCode(context: Context): Long = try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    } catch (e: PackageManager.NameNotFoundException) {
        0L
    }

    /** Latest published release, or null when offline, rate-limited, or the response is malformed. */
    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = open(LATEST_RELEASE).apply { setRequestProperty("Accept", "application/vnd.github+json") }
            try {
                if (conn.responseCode != HttpsURLConnection.HTTP_OK) return@withContext null
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        } catch (e: IOException) {
            null
        } catch (e: JSONException) {
            null
        }
    }

    /** Downloads the APK into the cache, reporting progress in 0..1. */
    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        require(isTrustedDownloadUrl(url)) { "Untrusted download URL" }
        val target = File(File(context.cacheDir, "updates").apply { mkdirs() }, "LockIn-update.apk")
        val conn = open(url)
        try {
            if (conn.responseCode != HttpsURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        target
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(android.net.Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Hands the APK to Android's installer; the same signing key makes it an in-place update. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun parse(json: String): ReleaseInfo? {
        val release = JSONObject(json)
        val tag = release.optString("tag_name")
        val code = versionCodeFromTag(tag) ?: return null
        val assets = release.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?: return null
        val url = apk.optString("browser_download_url")
        if (!isTrustedDownloadUrl(url)) return null
        return ReleaseInfo(tag = tag, versionCode = code, apkUrl = url, notes = release.optString("body"))
    }

    private fun open(url: String): HttpsURLConnection =
        (URL(url).openConnection() as HttpsURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
}
