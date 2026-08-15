package com.ahmed.tikdown

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object Downloader {

    private fun safeName(raw: String): String =
        raw.replace(Regex("""[\\/:*?"<>|\n\r]"""), "_")
            .trim()
            .take(60)
            .ifBlank { "tiktok" }

    fun enqueue(
        context: Context,
        url: String,
        author: String,
        id: String,
        isAudio: Boolean = false
    ): Long {
        val ext = if (isAudio) "mp3" else "mp4"
        val fileName = "${safeName(author)}_${safeName(id)}.$ext"
        val dir = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("جاري التحميل من TikDown")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(dir, "TikDown/$fileName")
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
            .addRequestHeader("Referer", "https://www.tiktok.com/")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}
