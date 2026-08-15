package com.ahmed.tikdown

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val incomingLink = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        setContent {
            AppTheme { DownloaderScreen(incomingLink) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            TikTokRepo.extractUrl(text)?.let { incomingLink.value = it }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Color(0xFFFE2C55),
        onPrimary = Color.White,
        secondary = Color(0xFF25F4EE),
        background = Color(0xFF0B0B0F),
        surface = Color(0xFF17171D),
        onBackground = Color(0xFFEDEDED),
        onSurface = Color(0xFFEDEDED)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(incomingLink: MutableState<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun load(url: String) {
        if (url.isBlank()) return
        scope.launch {
            loading = true; error = null; info = null
            TikTokRepo.fetch(url)
                .onSuccess { info = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(incomingLink.value) {
        if (incomingLink.value.isNotBlank()) {
            link = incomingLink.value
            load(link)
            incomingLink.value = ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("TikDown", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("الصق رابط التيك توك هنا") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val txt = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        val found = TikTokRepo.extractUrl(txt)
                        if (found != null) { link = found; load(found) }
                        else Toast.makeText(context, "مفيش رابط في الكليب بورد", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("لصق") }

                Button(
                    onClick = { load(link) },
                    enabled = !loading && link.isNotBlank()
                ) { Text(if (loading) "جاري الجلب..." else "جلب الفيديو") }
            }

            Spacer(Modifier.height(20.dp))

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1620)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        it,
                        color = Color(0xFFFF8FA3),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            info?.let { v ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        AsyncImage(
                            model = v.cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("@${v.author}", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(v.title, fontSize = 15.sp, maxLines = 3)
                        Spacer(Modifier.height(6.dp))
                        Text("${v.durationSec} ثانية", fontSize = 12.sp, color = Color.Gray)

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                Downloader.enqueue(context, v.urlNoWatermark, v.author, v.id)
                                Toast.makeText(context, "بدأ التحميل ✅", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تحميل بدون علامة مائية") }

                        v.urlHd?.let { hd ->
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    Downloader.enqueue(context, hd, v.author, "${v.id}_HD")
                                    Toast.makeText(context, "بدأ تحميل HD ✅", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("تحميل جودة HD") }
                        }

                        v.urlMusic?.let { mp3 ->
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    Downloader.enqueue(context, mp3, v.author, v.id, isAudio = true)
                                    Toast.makeText(context, "بدأ تحميل الصوت ✅", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("تحميل الصوت MP3") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "الملفات بتتحفظ في Movies/TikDown",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
