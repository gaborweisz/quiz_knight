package com.trainig.quiz_knight.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val BgDark       = Color(0xFF1A0F00)
private val BgMid        = Color(0xFF2C1A00)
private val Gold         = Color(0xFFD4AF37)
private val GoldDim      = Color(0xFFAA9977)
private val CorrectGreen = Color(0xFF388E3C)
private val WrongRed     = Color(0xFFC62828)
private val NeutralCard  = Color(0xFF3D2800)

@Composable
fun QuizScreen(
    settlementId: String,
    onQuizFinished: (score: Int, passed: Boolean) -> Unit,
    onBackToMap: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    LaunchedEffect(settlementId) { viewModel.init(settlementId) }

    val state by viewModel.uiState.collectAsState()

    // Confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Navigate away when quiz is finished
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onQuizFinished(state.score, state.passed)
    }

    // Intercept system back button with the same confirmation dialog
    androidx.activity.compose.BackHandler {
        showExitDialog = true
    }

    // ── Abandon confirmation dialog ───────────────────────────────────────
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color(0xFF2C1A00),
            titleContentColor = Gold,
            textContentColor = GoldDim,
            title = { Text("Abandon Quest?", fontWeight = FontWeight.Bold) },
            text = { Text("Your progress in this quiz will be lost.\nAre you sure you want to return to the map?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBackToMap()
                }) {
                    Text("Leave", color = WrongRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Keep Playing", color = Gold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgMid)))
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Gold)
            }
            state.questions.isEmpty() -> {
                Text("No questions available.", color = GoldDim, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                val question = state.questions[state.currentIndex]
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(32.dp))

                    // ── Header row with back button ───────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back to map button — enlarged and now navigates immediately so user can return to map quickly
                        IconButton(
                            onClick = { onBackToMap() },
                            modifier = Modifier.size(80.dp)
                        ) {
                            Text("🗺", fontSize = 48.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.settlementName, color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("📜 ${state.topicName}", color = GoldDim, fontSize = 13.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        // Invisible spacer to balance the row
                        Spacer(Modifier.size(48.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Progress bar ──────────────────────────────────────
                    QuizProgressBar(current = state.currentIndex + 1, total = state.questions.size, score = state.score)
                    Spacer(Modifier.height(24.dp))

                    // ── Question card ─────────────────────────────────────
                    AnimatedContent(
                        targetState = state.currentIndex,
                        transitionSpec = {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut())
                        },
                        label = "question"
                    ) { idx ->
                        val q = state.questions[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NeutralCard)
                        ) {
                            Text(
                                text = q.text,
                                color = Color(0xFFFFF8E1),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Answer options ────────────────────────────────────
                    question.options.forEachIndexed { index: Int, option: String ->
                        AnswerOption(
                            text = option,
                            index = index,
                            selectedIndex = state.selectedOptionIndex,
                            correctIndex = question.correctIndex,
                            isRevealed = state.isAnswerRevealed,
                            onClick = { viewModel.selectOption(index) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Next button (shown after answer) ──────────────────
                    AnimatedVisibility(visible = state.isAnswerRevealed) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = BgDark)
                        ) {
                            val isLast = state.currentIndex == state.questions.size - 1
                            Text(
                                if (isLast) "See Results ➜" else "Next Question ➜",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun QuizProgressBar(current: Int, total: Int, score: Int) {
    val progress by animateFloatAsState(
        targetValue = current.toFloat() / total.toFloat(),
        animationSpec = tween(400),
        label = "progress"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question $current / $total", color = GoldDim, fontSize = 13.sp)
            Text("Score: $score", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Gold,
            trackColor = NeutralCard
        )
    }
}

@Composable
private fun AnswerOption(
    text: String,
    index: Int,
    selectedIndex: Int?,
    correctIndex: Int,
    isRevealed: Boolean,
    onClick: () -> Unit
) {
    val borderColor: Color
    val bgColor: Color
    val textColor: Color

    when {
        !isRevealed -> {
            borderColor = GoldDim.copy(alpha = 0.4f)
            bgColor = NeutralCard
            textColor = Color(0xFFFFF8E1)
        }
        index == correctIndex -> {
            borderColor = CorrectGreen
            bgColor = CorrectGreen.copy(alpha = 0.2f)
            textColor = Color(0xFFA5D6A7)
        }
        index == selectedIndex -> {
            borderColor = WrongRed
            bgColor = WrongRed.copy(alpha = 0.2f)
            textColor = Color(0xFFEF9A9A)
        }
        else -> {
            borderColor = GoldDim.copy(alpha = 0.2f)
            bgColor = NeutralCard.copy(alpha = 0.5f)
            textColor = GoldDim.copy(alpha = 0.5f)
        }
    }

    val prefix = listOf("A", "B", "C", "D")[index]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !isRevealed, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$prefix.", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.width(28.dp))
            Text(text, color = textColor, fontSize = 15.sp)
        }
    }
}
