package com.ahmed.tikdown

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateGate(
    forceOpen: Boolean = false,
    manualCheck: MutableState<Boolean>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var visible by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var apkFile by remember { mutableStateOf<File?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UpdateWorker.schedule(context)
        delay(2200)
        Updater.fetchLatest().onSuccess { r ->
            release = r
            if (Updater.isNewer(r.version, Updater.currentVersion(context))) visible = true
        }
    }

    LaunchedEffect(forceOpen) {
        if (forceOpen && release != null) visible = true
    }

    LaunchedEffect(manualCheck.value) {
        if (!manualCheck.value) return@LaunchedEffect
        Toast.makeText(context, "بفحص التحديثات...", Toast.LENGTH_SHORT).show()
        Updater.fetchLatest()
            .onSuccess { r ->
                release = r
                if (Updater.isNewer(r.version, Updater.currentVersion(context))) {
                    visible = true
                } else {
                    Toast.makeText(context, "أنت على أحدث إصدار ✅", Toast.LENGTH_SHORT).show()
                }
            }
            .onFailure {
                Toast.makeText(context, it.message ?: "فشل الفحص", Toast.LENGTH_SHORT).show()
            }
        manualCheck.value = false
    }

    val r = release
    if (!visible || r == null) return

    Dialog(onDismissRequest = { if (!downloading) visible = false }) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, Color(0xFF2C2C3A), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Text(
                "تحديث جديد متاح",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(brush = BrandBrush)
            )
            Spacer(Modifier.height(6.dp))
            Text("الإصدار ${r.version}  ·  ${r.sizeMb} ميجا", fontSize = 12.sp, color = Cyan)

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101018))
                    .padding(12.dp)
            ) {
                Text(
                    r.notes,
                    fontSize = 12.sp,
                    color = Color(0xFFBFBFCC),
                    lineHeight = 18.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }

            err?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 12.sp, color = Color(0xFFFF8FA3))
            }

            Spacer(Modifier.height(18.dp))

            when {
                downloading -> {
                    Text(
                        "جاري تحميل التحديث  ${(progress * 100).toInt()}%",
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

                apkFile != null -> {
                    GradientButton(
                        text = "تثبيت التحديث",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!Updater.canInstall(context)) {
                            Toast.makeText(
                                context,
                                "فعّل السماح بتثبيت التطبيقات من TikDown",
                                Toast.LENGTH_LONG
                            ).show()
                            Updater.openInstallPermission(context)
                        } else {
                            Updater.install(context, apkFile!!)
                        }
                    }
                }

                else -> {
                    GradientButton(
                        text = "تحميل الآن",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        scope.launch {
                            downloading = true; progress = 0f; err = null
                            Updater.downloadApk(context, r) { p -> progress = p }
                                .onSuccess { apkFile = it }
                                .onFailure { err = it.message }
                            downloading = false
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    GradientButton(
                        text = "بعدين",
                        filled = false,
                        small = true,
                        modifier = Modifier.fillMaxWidth()
                    ) { visible = false }
                }
            }
        }
    }
}
