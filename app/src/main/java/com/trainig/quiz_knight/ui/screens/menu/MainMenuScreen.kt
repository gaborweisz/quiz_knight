package com.trainig.quiz_knight.ui.screens.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    viewModel: MainMenuViewModel = hiltViewModel()
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0F00), Color(0xFF2C1A00))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .padding(32.dp)
        ) {
            Text("⚔️", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Quiz Knight",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )
            Text(
                "A Medieval Knowledge Quest",
                fontSize = 14.sp,
                color = Color(0xFFAA9977),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            MedievalButton(label = "⚔  Begin Quest", onClick = onStartGame)
            Spacer(Modifier.height(16.dp))
            MedievalButton(label = "🗺  Reset Progress", onClick = { viewModel.resetProgress() })

            Spacer(Modifier.height(48.dp))
            Text(
                "Visit every settlement and\nprove your knowledge!",
                fontSize = 13.sp,
                color = Color(0xFF887755),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MedievalButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3D2800),
            contentColor = Color(0xFFD4AF37)
        )
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

