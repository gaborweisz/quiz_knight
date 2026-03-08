package com.trainig.quiz_knight.ui.screens.result

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.trainig.quiz_knight.R
import com.trainig.quiz_knight.domain.usecase.GetQuestionsForTopicUseCase

private val BgDark  = Color(0xFF1A0F00)
private val BgMid   = Color(0xFF2C1A00)
private val Gold    = Color(0xFFD4AF37)
private val GoldDim = Color(0xFFAA9977)
private val Green   = Color(0xFF4CAF50)
private val Red     = Color(0xFFEF5350)

@Composable
fun ResultScreen(
    settlementId: String,
    score: Int,
    passed: Boolean,
    onContinue: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val total = GetQuestionsForTopicUseCase.QUESTIONS_PER_QUIZ

    // Track whether the victory video is still playing
    var showVideo by remember { mutableStateOf(passed) }

    SideEffect {
        viewModel.prepare(passed)
    }

    LaunchedEffect(Unit) {
        viewModel.onResultShown(passed)
    }

    // ── Victory video overlay ────────────────────────────────────────────
    if (showVideo) {
        VictoryVideoPlayer(
            onVideoEnded = {
                // Video finished — show the result card, do NOT navigate yet
                showVideo = false
            },
            onSkip = {
                // Player skipped — show the result card, do NOT navigate yet
                showVideo = false
            }
        )
        return  // Don't render the result card while video is playing
    }

    // ── Result card (defeat, or passed after video) ──────────────────────
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Result emoji badge
            Text(
                text = if (passed) "🏆" else "💀",
                fontSize = 80.sp,
                modifier = Modifier.scale(scale.value)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = if (passed) "Victory!" else "Defeated!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (passed) Gold else Red
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (passed)
                    "You have conquered this settlement!"
                else
                    "The quiz was not passed. Try again!",
                fontSize = 15.sp,
                color = GoldDim,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Score card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2800))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your Score", color = GoldDim, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$score / $total",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (passed) Green else Red
                    )
                    Spacer(Modifier.height(8.dp))

                    // Visual score bar
                    val fraction = score.toFloat() / total.toFloat()
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = if (passed) Green else Red,
                        trackColor = Color(0xFF1A0F00)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(fraction * 100).toInt()}%  •  Passing: 70%",
                        color = GoldDim,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.onReturnToMap()
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = BgDark
                )
            ) {
                Text("Return to Map ➜", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Victory video player ─────────────────────────────────────────────────────

@Composable
private fun VictoryVideoPlayer(
    onVideoEnded: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.riding_knight}")
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    // Listen for video end
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap anywhere to skip
            .clickable { onSkip() },
        contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false  // hide default controls — tap-to-skip is enough
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // "Tap to skip" hint
        Text(
            text = "Tap to skip",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
