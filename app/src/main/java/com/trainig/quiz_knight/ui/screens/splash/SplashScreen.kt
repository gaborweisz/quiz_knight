package com.trainig.quiz_knight.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    introShown: Boolean = true,
    onNavigateToIntro: () -> Unit = {},
    onNavigateToMenu: () -> Unit
) {
    // Fade + scale animation
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(900))
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        delay(1200)
        if (introShown) {
            onNavigateToMenu()
        } else {
            onNavigateToIntro()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0F00)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            Text(
                text = "⚔️",
                fontSize = 80.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Quiz Knight",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Conquer the realm with knowledge",
                fontSize = 15.sp,
                color = Color(0xFFAA9977),
                textAlign = TextAlign.Center
            )
        }
    }
}
