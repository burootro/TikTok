package com.ahmed.tikdown

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val id: String,
    val title: String,
    val author: String,
    val cover: String,
    val uri: String,
    val isAudio: Boolean,
    val time: Long
)

object History {

    private const val PREF = "tikdown_prefs"
    private const val KEY = "history"
    private const val MAX = 60

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun load(c: Context): List<HistoryItem> = try {
        val arr = JSONArray(prefs(c).getString(KEY, "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryItem(
                id = o.optString("id"),
                title = o.optString("title"),
                author = o.optString("author"),
                cover = o.optString("cover"),
                uri = o.optString("uri"),
                isAudio = o.optBoolean("isAudio"),
                time = o.optLong("time")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save(c: Context, items: List<HistoryItem>) {
        val arr = JSONArray()
        items.take(MAX).forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("author", it.author)
                put("cover", it.cover)
                put("uri", it.uri)
                put("isAudio", it.isAudio)
                put("time", it.time)
            })
        }
        prefs(c).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(c: Context, item: HistoryItem) = save(c, listOf(item) + load(c))

    fun remove(c: Context, id: String) = save(c, load(c).filterNot { it.id == id })

    fun clear(c: Context) {
        prefs(c).edit().remove(KEY).apply()
    }

    fun share(c: Context, uri: String, isAudio: Boolean): Boolean = try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (isAudio) "audio/mpeg" else "video/mp4"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        c.startActivity(
            Intent.createChooser(send, "مشاركة الفيديو")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: Exception) { false }

    fun openTelegram(c: Context, username: String) {
        val clean = username.trimStart('@')
        try {
            c.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$clean"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            try {
                c.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$clean"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) { }
        }
    }
}
