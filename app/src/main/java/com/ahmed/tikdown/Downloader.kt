package com.ahmed.tikdown

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class SavedFile(
    val uri: Uri,
    val name: String,
    val folder: String
)

object Downloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun safeName(raw: String): String =
        raw.replace(Regex("""[\\/:*?"<>|\n\r\t]"""), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_', '.')
            .take(40)
            .ifBlank { "tiktok" }

    suspend fun download(
        context: Context,
        url: String,
        author: String,
        id: String,
        isAudio: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Result<SavedFile> = withContext(Dispatchers.IO) {

        val ext = if (isAudio) "mp3" else "mp4"
        val stamp = System.currentTimeMillis() % 100000
        val fileName = "${safeName(author)}_${safeName(id)}_$stamp.$ext"
        val mime = if (isAudio) "audio/mpeg" else "video/mp4"
        val folder = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        try {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                .header("Referer", "https://www.tiktok.com/")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("السيرفر رفض التحميل (${res.code})"))
                }
                val body = res.body
                    ?: return@withContext Result.failure(Exception("مفيش بيانات في الرد"))

                val total = body.contentLength()
                var written = 0L
                val buf = ByteArray(64 * 1024)

                if (Build.VERSION.SDK_INT >= 29) {
                    val resolver = context.contentResolver
                    val collection = if (isAudio)
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/TikDown")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(collection, values)
                        ?: return@withContext Result.failure(Exception("مقدرتش أنشئ الملف على التخزين"))

                    val out = resolver.openOutputStream(uri)
                        ?: return@withContext Result.failure(Exception("مقدرتش أفتح الملف للكتابة"))

                    out.use { stream ->
                        body.byteStream().use { input ->
                            while (true) {
                                val n = input.read(buf)
                                if (n == -1) break
                                stream.write(buf, 0, n)
                                written += n
                                if (total > 0) onProgress(written.toFloat() / total)
                            }
                            stream.flush()
                        }
                    }

                    if (written == 0L) {
                        resolver.delete(uri, null, null)
                        return@withContext Result.failure(Exception("الملف نزل فاضي، جرّب تاني"))
                    }

                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    onProgress(1f)
                    Result.success(SavedFile(uri, fileName, "$folder/TikDown"))
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(folder), "TikDown")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, fileName)

                    FileOutputStream(file).use { stream ->
                        body.byteStream().use { input ->
                            while (true) {
                                val n = input.read(buf)
                                if (n == -1) break
                                stream.write(buf, 0, n)
                                written += n
                                if (total > 0) onProgress(written.toFloat() / total)
                            }
                            stream.flush()
                        }
                    }

                    if (written == 0L) {
                        file.delete()
                        return@withContext Result.failure(Exception("الملف نزل فاضي، جرّب تاني"))
                    }

                    MediaScannerConnection.scanFile(
                        context, arrayOf(file.absolutePath), arrayOf(mime), null
                    )
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    onProgress(1f)
                    Result.success(SavedFile(uri, fileName, "$folder/TikDown"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "فشل التحميل، اتأكد من النت"))
        }
    }
}
