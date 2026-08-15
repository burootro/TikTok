package com.ahmed.tikdown

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val sizeMb: String
)

object Updater {

    private const val OWNER = "burootro"
    private const val REPO = "TikTok"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun currentVersion(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (e: Exception) { "0" }

    fun isNewer(remote: String, current: String): Boolean {
        fun parts(s: String) = s.trim().trimStart('v', 'V')
            .split(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }

        val r = parts(remote)
        val c = parts(current)
        if (r.isEmpty()) return false
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun fetchLatest(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TikDown-App")
                .build()

            client.newCall(req).execute().use { res ->
                if (res.code == 404) {
                    return@withContext Result.failure(Exception("مفيش إصدارات منشورة لسه"))
                }
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub رد بـ ${res.code}"))
                }

                val json = JSONObject(res.body?.string().orEmpty())
                val tag = json.optString("tag_name")
                if (tag.isBlank()) {
                    return@withContext Result.failure(Exception("رد غير متوقع من GitHub"))
                }

                val assets = json.optJSONArray("assets")
                var apkUrl = ""
                var size = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk", true)) {
                            apkUrl = a.optString("browser_download_url")
                            size = a.optLong("size")
                            break
                        }
                    }
                }
                if (apkUrl.isBlank()) {
                    return@withContext Result.failure(Exception("الإصدار ده مفيهوش ملف APK"))
                }

                Result.success(
                    ReleaseInfo(
                        version = tag,
                        notes = json.optString("body").trim()
                            .ifBlank { "تحسينات وإصلاحات عامة." }
                            .take(400),
                        apkUrl = apkUrl,
                        sizeMb = String.format("%.1f", size / 1024.0 / 1024.0)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "مقدرتش أوصل لـ GitHub"))
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.getExternalFilesDir(null), "updates")
            if (!dir.exists()) dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }

            val file = File(dir, "TikDown-${release.version}.apk")

            val req = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "TikDown-App")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل التحميل (${res.code})"))
                }
                val body = res.body
                    ?: return@withContext Result.failure(Exception("مفيش بيانات"))

                val total = body.contentLength()
                var written = 0L
                val buf = ByteArray(64 * 1024)

                FileOutputStream(file).use { out ->
                    body.byteStream().use { input ->
                        while (true) {
                            val n = input.read(buf)
                            if (n == -1) break
                            out.write(buf, 0, n)
                            written += n
                            if (total > 0) onProgress(written.toFloat() / total)
                        }
                        out.flush()
                    }
                }

                if (written < 10_000) {
                    file.delete()
                    return@withContext Result.failure(Exception("الملف نزل ناقص، جرّب تاني"))
                }
                onProgress(1f)
                Result.success(file)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "فشل تحميل التحديث"))
        }
    }

    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 26) context.packageManager.canRequestPackageInstalls()
        else true

    fun openInstallPermission(context: Context) {
        val i = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
