package com.trainig.quiz_knight.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val BgDark   = Color(0xFF1A0F00)
private val BgMid    = Color(0xFF2C1A00)
private val Gold     = Color(0xFFD4AF37)
private val GoldDim  = Color(0xFFAA9977)
private val Green    = Color(0xFF4CAF50)
private val Red      = Color(0xFFEF5350)
private val CardBg   = Color(0xFF3D2800)
private val NotDone  = Color(0xFF554433)

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to map",
                    tint = Gold
                )
            }
            Text(
                text = "⚔️  Statistics",
                color = Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // ── Overall summary card ─────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Overall Progress", color = GoldDim, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(label = "Cities visited", value = "${uiState.totalAttempted}")
                    StatPill(label = "Total correct", value = "${uiState.totalCorrect}")
                    StatPill(label = "Accuracy", value = "${uiState.overallPercentage.toInt()}%")
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (uiState.overallPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Gold,
                    trackColor = BgDark
                )
            }
        }

        // ── Per-city list ────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.stats) { stat ->
                CityStatCard(stat = stat)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = GoldDim, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CityStatCard(stat: SettlementStats) {
    val barColor = when {
        !stat.attempted -> NotDone
        stat.percentage >= 70f -> Green
        else -> Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stat.attempted) CardBg else Color(0xFF241500)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = if (stat.settlement.isCompleted) "✅" else if (stat.attempted) "⚔️" else "🔒",
                fontSize = 22.sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            // Name + topic + bar
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stat.settlement.name,
                    color = if (stat.attempted) Gold else GoldDim,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    stat.settlement.topic.displayName,
                    color = GoldDim,
                    fontSize = 11.sp
                )
                if (stat.attempted) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = barColor,
                        trackColor = BgDark
                    )
                }
            }

            // Score
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (stat.attempted) {
                    Text(
                        "${stat.correct}/${stat.total}",
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "${stat.percentage.toInt()}%",
                        color = barColor,
                        fontSize = 12.sp
                    )
                } else {
                    Text("—", color = GoldDim, fontSize = 16.sp)
                }
            }
        }
    }
}

