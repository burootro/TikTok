package com.ahmed.tikdown

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class VideoInfo(
    val id: String,
    val title: String,
    val cover: String,
    val author: String,
    val urlNoWatermark: String,
    val urlHd: String?,
    val urlMusic: String?,
    val durationSec: Int
)

object TikTokRepo {

    private const val HOST = "https://tikwm.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val LINK_REGEX =
        Regex("""https?://[A-Za-z0-9./_@\-?=&%#]*tiktok\.com[A-Za-z0-9./_@\-?=&%#]*""")

    fun extractUrl(text: String): String? = LINK_REGEX.find(text.trim())?.value

    private fun abs(path: String): String =
        if (path.startsWith("http")) path else HOST + path

    suspend fun fetch(rawInput: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        val link = extractUrl(rawInput)
            ?: return@withContext Result.failure(Exception("مفيش رابط تيك توك صالح في النص ده"))

        try {
            val endpoint = "$HOST/api/?url=${URLEncoder.encode(link, "UTF-8")}&hd=1"
            val req = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("السيرفر رد بـ ${res.code}"))
                }
                val body = res.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext Result.failure(Exception("رد فاضي من السيرفر"))
                }

                val json = JSONObject(body)
                if (json.optInt("code", -1) != 0) {
                    val msg = json.optString("msg").ifBlank { "فشل جلب بيانات الفيديو" }
                    return@withContext Result.failure(Exception(msg))
                }

                val d = json.getJSONObject("data")
                val play = d.optString("play")
                if (play.isBlank()) {
                    return@withContext Result.failure(Exception("الفيديو ده مش متاح للتحميل"))
                }

                val hd = d.optString("hdplay").takeIf { it.isNotBlank() }?.let { abs(it) }
                val music = d.optString("music").takeIf { it.isNotBlank() }?.let { abs(it) }

                Result.success(
                    VideoInfo(
                        id = d.optString("id").ifBlank { System.currentTimeMillis().toString() },
                        title = d.optString("title").ifBlank { "TikTok Video" },
                        cover = abs(d.optString("cover")),
                        author = d.optJSONObject("author")?.optString("unique_id").orEmpty()
                            .ifBlank { "tiktok" },
                        urlNoWatermark = abs(play),
                        urlHd = hd,
                        urlMusic = music,
                        durationSec = d.optInt("duration")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "خطأ في الاتصال بالنت"))
        }
    }
}
