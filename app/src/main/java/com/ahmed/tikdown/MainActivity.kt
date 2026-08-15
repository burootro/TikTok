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
const val TELEGRAM = "@pro90qq"

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
                MainScaffold(incomingLink, manualCheck)
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
fun DrawerRow(
    title: String,
    subtitle: String,
    accent: Color = Cyan,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(Color(0xFF12121A))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(4.dp, 30.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8E8F0))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF6A6A7A))
        }
    }
}

@Composable
fun SideDrawer(
    historyCount: Int,
    onHistory: () -> Unit,
    onCheckUpdate: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        drawerContainerColor = CardBg,
        drawerContentColor = Color(0xFFEDEDED),
        modifier = Modifier.width(300.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(
                        "TikDown",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(brush = BrandBrush)
                    )
                    Text("by $DEV_NAME", fontSize = 11.sp, color = Color(0xFF6A6A7A))
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF262633))
            )
            Spacer(Modifier.height(18.dp))

            DrawerRow(
                title = if (historyCount > 0) "سجل التحميلات ($historyCount)" else "سجل التحميلات",
                subtitle = "كل الفيديوهات اللي نزّلتها",
                accent = Pink
            ) {
                onClose()
                onHistory()
            }

            Spacer(Modifier.height(10.dp))

            DrawerRow(
                title = "فحص التحديثات",
                subtitle = "شوف لو فيه إصدار جديد",
                accent = Cyan
            ) {
                onClose()
                onCheckUpdate()
            }

            Spacer(Modifier.height(10.dp))

            DrawerRow(
                title = "تواصل معنا",
                subtitle = "تليجرام $TELEGRAM",
                accent = Color(0xFF2AABEE)
            ) {
                History.openTelegram(context, TELEGRAM)
            }

            Spacer(Modifier.weight(1f))

            DrawerRow(
                title = "الإصدار " + Updater.currentVersion(context),
                subtitle = "TikDown · جميع الحقوق محفوظة",
                accent = Color(0xFF44444F)
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun HistoryDialog(
    onClose: () -> Unit,
    onChanged: () -> Unit,
    onPlay: (String, String) -> Unit
) {
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
                Text(count.toString(), fontSize = 13.sp, color = Color(0xFF6A6A7A))
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
                                    if (h.isAudio) {
                                        History.share(context, h.uri, true)
                                    } else {
                                        onClose()
                                        onPlay(h.uri, h.title)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    incomingLink: MutableState<String>,
    manualCheck: MutableState<Boolean>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showHistory by remember { mutable
