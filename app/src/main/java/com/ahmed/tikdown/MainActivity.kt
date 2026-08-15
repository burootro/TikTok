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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val DEV_NAME = "AHMED"

val Pink = Color(0xFFFF2D55)
val Cyan = Color(0xFF25F4EE)
val BgDark = Color(0xFF0B0B0F)
val CardBg = Color(0xFF15151D)
val BrandBrush = Brush.horizontalGradient(listOf(Pink, Cyan))

class MainActivity : ComponentActivity() {

    private val incomingLink = mutableStateOf("")
    private val openUpdate = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        if (intent?.action == UpdateWorker.ACTION_OPEN_UPDATE) openUpdate.value = true
        setContent { AppRoot(incomingLink, openUpdate) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
        if (intent.action == UpdateWorker.ACTION_OPEN_UPDATE) openUpdate.value = true
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
        primary = Pink,
        onPrimary = Color.White,
        secondary = Cyan,
        background = BgDark,
        surface = CardBg,
        onBackground = Color(0xFFEDEDED),
        onSurface = Color(0xFFEDEDED)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun AppRoot(incomingLink: MutableState<String>, openUpdate: MutableState<Boolean>) {
    AppTheme {
        var showSplash by remember { mutableStateOf(true) }
        val manualCheck = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(1900)
            showSplash = false
        }

        Box(Modifier.fillMaxSize().background(BgDark)) {
            AnimatedGlow()

            AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(500))) {
                SplashScreen()
            }
            AnimatedVisibility(
                visible = !showSplash,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 8 }
            ) {
                DownloaderScreen(incomingLink, manualCheck)
            }

            if (!showSplash) UpdateGate(forceOpen = openUpdate.value, manualCheck = manualCheck)
        }
    }
}

@Composable
fun AnimatedGlow() {
    val t = rememberInfiniteTransition(label = "glow")
    val p by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "p"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(340.dp)
                .offset(x = (-90).dp, y = (40 + p * 60).dp)
                .alpha(0.28f)
                .background(Brush.radialGradient(listOf(Pink, Color.Transparent)), CircleShape)
        )
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = (-40 - p * 60).dp)
                .alpha(0.20f)
                .background(Brush.radialGradient(listOf(Cyan, Color.Transparent)), CircleShape)
        )
    }
}

@Composable
fun SplashScreen() {
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { start = true }

    val scale by animateFloatAsState(
        targetValue = if (start) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val fade by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(900, delayMillis = 250),
        label = "fade"
    )
    val pulse = rememberInfiniteTransition(label = "pulse")
    val halo by pulse.animateFloat(
        0.30f, 0.75f,
        infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(210.dp)
                        .alpha(halo * 0.45f)
                        .background(Brush.radialGradient(listOf(Pink, Color.Transparent)), CircleShape)
                )
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).scale(scale)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "TikDown",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(brush = BrandBrush),
                modifier = Modifier.alpha(fade)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "by $DEV_NAME",
                fontSize = 13.sp,
                color = Color(0xFF8A8A99),
                modifier = Modifier.alpha(fade)
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    small: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (filled && enabled) Modifier.background(BrandBrush)
                else Modifier
                    .background(Color(0xFF16161E))
                    .border(1.dp, Color(0xFF33333F), RoundedCornerShape(14.dp))
            )
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(vertical = if (small) 11.dp else 15.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled) Color.White else Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 13.sp else 15.sp
        )
    }
}

@Composable
fun LoadingPulse() {
    val t = rememberInfiniteTransition(label = "load")
    val s by t.animateFloat(
        0.82f, 1.12f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(52.dp).scale(s)
        )
        Spacer(Modifier.height(12.dp))
        Text("بجيب الفيديو...", color = Color(0xFF9A9AAB), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.6f).clip(RoundedCornerShape(50)),
            color = Pink,
            trackColor = Color(0xFF23232E)
        )
    }
}

@Composable
fun HistoryDialog(onClose: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(History.load(context)) }
    val count = items.size

    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CardBg)
                .border(1.dp, Color(0xFF2C2C3A), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "سجل التحميلات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(brush = BrandBrush)
                )
                Text(
                    count.toString(),
                    fontSize = 13.sp,
                    color = Color(0xFF6A6A7A)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (items.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لسه مفيش تحميلات", color = Color(0xFF5E5E70), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { h ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF101018))
                                .clickable {
                                    if (!History.open(context, h.uri, h.isAudio)) {
                                        Toast.makeText(
                                            context, "الملف مش موجود", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = h.cover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "@" + h.author,
                                    color = Cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    h.title,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFFB8B8C6),
                                    lineHeight = 15.sp
                                )
                                if (h.isAudio) {
                                    Spacer(Modifier.height(3.dp))
                                    Text("MP3", fontSize = 10.sp, color = Pink)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "مشاركة",
                                    fontSize = 11.sp,
                                    color = Cyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { History.share(context, h.uri, h.isAudio) }
                                        .padding(4.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "حذف",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8FA3),
                                    modifier = Modifier
                                        .clickable {
                                            History.remove(context, h.id)
                                            items = History.load(context)
                                            onChanged()
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (items.isNotEmpty()) {
                    GradientButton(
                        text = "مسح الكل",
                        filled = false,
                        small = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        History.clear(context)
                        items = emptyList()
                        onChanged()
                    }
                }
                GradientButton(
                    text = "إغلاق",
                    small = true,
                    modifier = Modifier.weight(1f)
                ) { onClose() }
            }
        }
    }
}

@Composable
fun DownloaderScreen(
    incomingLink: MutableState<String>,
    manualCheck: MutableState<Boolean>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var saved by remember { mutableStateOf<SavedFile?>(null) }
    var savedIsAudio by remember { mutableStateOf(false) }

    var showHistory by remember { mutableStateOf(false) }
    var historyCount by remember { mutableIntStateOf(History.load(context).size) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= 28) {
            storagePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun load(url: String) {
        if (url.isBlank()) return
        scope.launch {
            loading = true; error = null; info = null; saved = null
            TikTokRepo.fetch(url)
                .onSuccess { info = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    fun startDownload(v: VideoInfo, url: String, idSuffix: String, isAudio: Boolean) {
        if (downloading) return
        scope.launch {
            downloading = true; progress = 0f; saved = null; error = null
            Downloader.download(context, url, v.author, v.id + idSuffix, isAudio) { p ->
                progress = p
            }
                .onSuccess { sf ->
                    saved = sf
                    savedIsAudio = isAudio
                    History.add(
                        context,
                        HistoryItem(
                            id = v.id + idSuffix + "-" + System.currentTimeMillis(),
                            title = v.title,
                            author = v.author,
                            cover = v.cover,
                            uri = sf.uri.toString(),
                            isAudio = isAudio,
                            time = System.currentTimeMillis()
                        )
                    )
                    historyCount = History.load(context).size
                    Toast.makeText(context, "اتحفظ في " + sf.folder + " ✅", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error = it.message }
            downloading = false
        }
    }

    LaunchedEffect(incomingLink.value) {
        if (incomingLink.value.isNotBlank()) {
            link = incomingLink.value
            load(link)
            incomingLink.value = ""
        }
    }

    if (showHistory) {
        HistoryDialog(
            onClose = { showHistory = false },
            onChanged = { historyCount = History.load(context).size }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 44.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "TikDown",
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(brush = BrandBrush)
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("حمّل أي فيديو بدون علامة مائية", fontSize = 12.sp, color = Color(0xFF77778A))

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("الصق رابط التيك توك هنا") },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pink,
                unfocusedBorderColor = Color(0xFF2C2C38),
                focusedLabelColor = Cyan,
                cursorColor = Pink,
                focusedContainerColor = Color(0xFF12121A),
                unfocusedContainerColor = Color(0xFF12121A)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton(text = "لصق", filled = false, modifier = Modifier.weight(1f)) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val txt = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                val found = TikTokRepo.extractUrl(txt)
                if (found != null) { link = found; load(found) }
                else Toast.makeText(context, "مفيش رابط في الكليب بورد", Toast.LENGTH_SHORT).show()
            }

            GradientButton(
                text = if (loading) "..." else "جلب الفيديو",
                enabled = !loading && link.isNotBlank(),
                modifier = Modifier.weight(1.6f)
            ) { load(link) }
        }

        Spacer(Modifier.height(22.dp))

        AnimatedVisibility(visible = loading, enter = fadeIn(), exit = fadeOut()) {
            LoadingPulse()
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF33131F))
                    .border(1.dp, Color(0xFF5C1E2E), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(error ?: "", color = Color(0xFFFF8FA3), fontSize = 13.sp)
            }
        }

        val v = info
        AnimatedVisibility(
            visible = v != null,
            enter = fadeIn(tween(450)) +
                    slideInVertically(tween(450)) { it / 5 } +
                    scaleIn(tween(450), initialScale = 0.93f),
            exit = fadeOut()
        ) {
            if (v != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .border(1.dp, Color(0xFF262633), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        AsyncImage(
                            model = v.cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 78.dp, height = 104.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "@" + v.author,
                                color = Cyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                v.title,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFFCFCFDA),
                                lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                v.durationSec.toString() + " ثانية",
                                fontSize = 11.sp,
                                color = Color(0xFF6A6A7A)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val percent = (progress * 100).toInt()

                    when {
                        downloading -> {
                            Text(
                                "جاري التحميل  " + percent + "%",
                                fontSize = 12.sp,
                                color = Cyan
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Pink,
                                trackColor = Color(0xFF23232E)
                            )
                        }

                        saved != null -> {
                            val sf = saved!!
                            Text(
                                "✅ اتحفظ في " + sf.folder,
                                fontSize = 11.sp,
                                color = Color(0xFF6BD98F)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                GradientButton(
                                    text = "فتح",
                                    small = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (!History.open(context, sf.uri.toString(), savedIsAudio)) {
                                        Toast.makeText(
                                            context, "مفيش تطبيق يفتح الملف", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                GradientButton(
                                    text = "مشاركة",
                                    filled = false,
                                    small = true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    History.share(context, sf.uri.toString(), savedIsAudio)
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            GradientButton(
                                text = "تحميل نسخة تانية",
                                filled = false,
                                small = true,
                                modifier = Modifier.fillMaxWidth()
                            ) { saved = null }
                        }

                        else -> {
                            GradientButton(
                                text = "تحميل بدون علامة مائية",
                                modifier = Modifier.fillMaxWidth()
                            ) { startDownload(v, v.urlNoWatermark, "", false) }

                            Row(
                                Modifier.fillMaxWidth().padding(top = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                v.urlHd?.let { hd ->
                                    GradientButton(
                                        text = "HD",
                                        filled = false,
                                        small = true,
                                        modifier = Modifier.weight(1f)
                                    ) { startDownload(v, hd, "_HD", false) }
                                }
                                v.urlMusic?.let { mp3 ->
                                    GradientButton(
                                        text = "صوت MP3",
                                        filled = false,
                                        small = true,
                                        modifier = Modifier.weight(1f)
                                    ) { startDownload(v, mp3, "_audio", true) }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        val historyLabel = if (historyCount > 0) "السجل (" + historyCount + ")" else "السجل"

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GradientButton(
                text = historyLabel,
                filled = false,
                small = true,
                modifier = Modifier.weight(1f)
            ) { showHistory = true }

            GradientButton(
                text = "فحص التحديثات",
                filled = false,
                small = true,
                modifier = Modifier.weight(1f)
            ) { manualCheck.value = true }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "الإصدار " + Updater.currentVersion(context),
            fontSize = 10.sp,
            color = Color(0xFF44444F)
        )
    }
}
