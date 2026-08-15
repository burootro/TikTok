package com.ahmed.tikdown

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

@Composable
fun VideoPlayerDialog(
    url: String,
    title: String,
    onClose: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardBg)
                .border(1.dp, Color(0xFF2C2C3A), RoundedCornerShape(22.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 12.sp,
                color = Color(0xFFBFBFCC),
                maxLines = 2,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(url))
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener { mp: MediaPlayer ->
                                loading = false
                                mp.isLooping = true
                                start()
                            }
                            setOnErrorListener { _, _, _ ->
                                loading = false
                                failed = true
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (loading) {
                    CircularProgressIndicator(color = Pink)
                }
                if (failed) {
                    Text(
                        "مقدرتش أشغّل الفيديو",
                        color = Color(0xFFFF8FA3),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            GradientButton(
                text = "إغلاق",
                small = true,
                modifier = Modifier.fillMaxWidth()
            ) { onClose() }
        }
    }
}
