package com.trainig.quiz_knight.ui.screens.victory

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val Gold    = Color(0xFFD4AF37)
private val GoldDim = Color(0xFFAA9977)
private val BgDark  = Color(0xFF1A0F00)

@Composable
fun VictoryScreen(
    onPlayAgain: () -> Unit,
    viewModel: VictoryViewModel = hiltViewModel()
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
        scale.animateTo(
            1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    // Trigger victory music after the screen is fully composed
    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
    }

    // Pulsing glow for the trophy
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, Color(0xFF2C1A00)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .padding(32.dp)
        ) {
            Text(
                text = "👑",
                fontSize = 96.sp,
                modifier = Modifier.scale(scale.value * pulseScale)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Realm Conquered!",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(scale.value)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "You have visited every settlement\nand proven your knowledge\nacross all realms of wisdom!",
                fontSize = 16.sp,
                color = GoldDim,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(40.dp))

            // Stars row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Text("⭐", fontSize = 40.sp, modifier = Modifier.scale(scale.value))
                }
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    viewModel.onPlayAgain()
                    onPlayAgain()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = BgDark
                )
            ) {
                Text("Play Again ⚔️", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
