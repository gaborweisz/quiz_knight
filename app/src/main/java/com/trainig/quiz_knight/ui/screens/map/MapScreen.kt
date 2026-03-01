package com.trainig.quiz_knight.ui.screens.map

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.trainig.quiz_knight.R
import com.trainig.quiz_knight.domain.model.MapPosition
import com.trainig.quiz_knight.domain.model.Settlement
import com.trainig.quiz_knight.domain.model.SettlementType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

// ── Colour palette ───────────────────────────────────────────────────────────
private val VillageColor   = Color(0xFF8B6914)
private val CityColor      = Color(0xFFD4AF37)
private val CompletedColor = Color(0xFF4CAF50)
private val TextColor      = Color(0xFF2C1600)
private val ErrorColor     = Color(0xFFFF5722)

// Fraction of screen height used for the map (rest is HUD)
private const val MAP_HEIGHT_FRACTION = 0.82f
// Walk animation duration in ms
private const val WALK_DURATION_MS = 900

@Composable
fun MapScreen(
    onEnterSettlement: (String) -> Unit,
    onVictory: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Load the medieval map SVG as a bitmap once
    val mapBitmap: ImageBitmap? = remember {
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.medieval_map)
            drawable?.toBitmap(1080, 1920)?.asImageBitmap()
        } catch (e: Exception) { null }
    }

    // Track canvas size so we can convert normalised positions → pixels
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Helper: normalised MapPosition → pixel Offset using the canvas size
    fun MapPosition.toPx(): Offset {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat() * MAP_HEIGHT_FRACTION
        return Offset(x * w, y * h)
    }

    // ── Animated knight position ─────────────────────────────────────────
    val knightX = remember { Animatable(0f) }
    val knightY = remember { Animatable(0f) }

    // Bob animation (only active while moving)
    val bobTransition = rememberInfiniteTransition(label = "bob")
    val bobOffset by bobTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(300, easing = EaseInOut), RepeatMode.Reverse),
        label = "bobY"
    )

    // Whenever the canvas is sized and we know the resting settlement, snap to it (no animation)
    LaunchedEffect(uiState.knightSettlementId, canvasSize) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect
        if (uiState.isMoving) return@LaunchedEffect          // don't snap while walking
        val s = uiState.settlements.firstOrNull { it.id == uiState.knightSettlementId }
            ?: return@LaunchedEffect
        val target = s.position.toPx()
        knightX.snapTo(target.x)
        knightY.snapTo(target.y)
    }

    // Whenever a move is requested, smoothly animate to the destination
    LaunchedEffect(uiState.knightFromId, uiState.knightToId) {
        val toId = uiState.knightToId ?: return@LaunchedEffect
        if (canvasSize == IntSize.Zero) return@LaunchedEffect
        val toSettlement = uiState.settlements.firstOrNull { it.id == toId }
            ?: return@LaunchedEffect
        val target = toSettlement.position.toPx()
        // Animate X and Y in parallel
        coroutineScope {
            listOf(
                async { knightX.animateTo(target.x, animationSpec = tween(WALK_DURATION_MS, easing = EaseInOut)) },
                async { knightY.animateTo(target.y, animationSpec = tween(WALK_DURATION_MS, easing = EaseInOut)) }
            ).awaitAll()
        }
        // Animation done — tell the ViewModel
        viewModel.onAnimationFinished()
    }

    LaunchedEffect(uiState.isGameComplete) {
        if (uiState.isGameComplete) onVictory()
    }

    // Open quiz after animation finishes (shouldOpenQuiz set by VM after move succeeds,
    // but we wait until isMoving = false so the knight is visually at the destination)
    LaunchedEffect(uiState.shouldOpenQuiz, uiState.isMoving) {
        if (uiState.shouldOpenQuiz && !uiState.isMoving) {
            viewModel.onQuizOpened()
            onEnterSettlement(uiState.knightSettlementId)
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0F00))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(uiState.settlements, uiState.isMoving) {
                    detectTapGestures { tapOffset ->
                        if (uiState.isMoving) return@detectTapGestures
                        val mapH = size.height * MAP_HEIGHT_FRACTION
                        val tapped = uiState.settlements.firstOrNull { s ->
                            val c = s.position.toOffset(size.width.toFloat(), mapH)
                            (tapOffset - c).getDistance() < nodeRadius(s.type)
                        }
                        tapped?.let { viewModel.onSettlementTapped(it.id) }
                    }
                }
        ) {
            val w = size.width
            val h = size.height * MAP_HEIGHT_FRACTION

            // ── Draw parchment map bitmap ─────────────────────────────────
            if (mapBitmap != null) {
                drawImage(
                    image = mapBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(0, 0),
                    dstSize = IntSize(w.toInt(), h.toInt())
                )
            } else {
                // Fallback plain parchment colour
                drawRect(color = Color(0xFFC8A86B), size = Size(w, h))
            }

            // Settlement nodes (roads are baked into the SVG)
            uiState.settlements.forEach { s ->
                drawSettlement(s, s.position.toOffset(w, h), textMeasurer)
            }

            // Knight
            val bob = if (uiState.isMoving) bobOffset else 0f
            drawKnight(Offset(knightX.value, knightY.value + bob))
        }

        MapHud(
            completedCount = uiState.completedIds.size,
            totalCount = uiState.settlements.size,
            isMoving = uiState.isMoving,
            onReset = { viewModel.resetProgress() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        )

        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = ErrorColor
            ) {
                Text(uiState.errorMessage ?: "", color = Color.White)
            }
        }
    }
}

// ── Drawing helpers ──────────────────────────────────────────────────────────

private fun nodeRadius(type: SettlementType) = if (type == SettlementType.CITY) 52f else 40f

private fun MapPosition.toOffset(w: Float, h: Float) = Offset(x * w, y * h)

private fun DrawScope.drawSettlement(settlement: Settlement, center: Offset, textMeasurer: TextMeasurer) {
    val radius = nodeRadius(settlement.type)
    val fillColor = when {
        settlement.isCompleted -> CompletedColor
        settlement.type == SettlementType.CITY -> CityColor
        else -> VillageColor
    }
    // Drop shadow
    drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = radius + 4f, center = center.copy(y = center.y + 4f))
    drawCircle(color = fillColor, radius = radius, center = center)
    drawCircle(
        color = if (settlement.type == SettlementType.CITY) Color(0xFFFFF8E1) else Color(0xFFD4AF37),
        radius = radius, center = center,
        style = Stroke(width = if (settlement.type == SettlementType.CITY) 3f else 2f)
    )
    if (settlement.isCompleted) {
        val layout = textMeasurer.measure(
            AnnotatedString("✓"),
            style = TextStyle(fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
        )
        drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
    } else {
        val icon = if (settlement.type == SettlementType.CITY) "🏰" else "🏘"
        val layout = textMeasurer.measure(AnnotatedString(icon), style = TextStyle(fontSize = 16.sp))
        drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f + 2f))
    }
    // Name label — dark ink colour on parchment
    val nameLayout = textMeasurer.measure(
        AnnotatedString(settlement.name),
        style = TextStyle(fontSize = 11.sp, color = TextColor, fontWeight = FontWeight.Bold)
    )
    drawText(nameLayout, topLeft = Offset(center.x - nameLayout.size.width / 2f, center.y + radius + 6f))
    // Topic label
    val topicLayout = textMeasurer.measure(
        AnnotatedString(settlement.topic.displayName),
        style = TextStyle(fontSize = 9.sp, color = TextColor.copy(alpha = 0.75f), fontWeight = FontWeight.Normal)
    )
    drawText(topicLayout, topLeft = Offset(center.x - topicLayout.size.width / 2f, center.y + radius + 6f + nameLayout.size.height))
}

private fun DrawScope.drawKnight(center: Offset) {
    val kx = center.x
    val ky = center.y - 48f

    // Glow halo
    drawCircle(color = Color(0xFF90CAF9).copy(alpha = 0.25f), radius = 36f, center = Offset(kx, ky))

    // Cape
    val capePath = androidx.compose.ui.graphics.Path().apply {
        moveTo(kx - 10f, ky + 2f)
        cubicTo(kx - 18f, ky + 20f, kx - 14f, ky + 38f, kx - 4f, ky + 42f)
        lineTo(kx + 4f, ky + 42f)
        cubicTo(kx + 14f, ky + 38f, kx + 18f, ky + 20f, kx + 10f, ky + 2f)
        close()
    }
    drawPath(capePath, color = Color(0xFF8B0000))

    // Body
    val bodyPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(kx - 9f, ky + 2f); lineTo(kx + 9f, ky + 2f)
        lineTo(kx + 11f, ky + 24f); lineTo(kx - 11f, ky + 24f); close()
    }
    drawPath(bodyPath, color = Color(0xFF90A4AE))
    drawPath(bodyPath, color = Color(0xFF546E7A), style = Stroke(width = 1.5f))
    drawLine(color = Color(0xFF546E7A), start = Offset(kx, ky + 4f), end = Offset(kx, ky + 22f), strokeWidth = 1.2f)

    // Helmet
    val helmetPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(kx - 10f, ky + 2f); lineTo(kx - 11f, ky - 6f)
        arcTo(androidx.compose.ui.geometry.Rect(kx - 11f, ky - 18f, kx + 11f, ky + 4f), 180f, 180f, false)
        lineTo(kx + 11f, ky - 6f); lineTo(kx + 10f, ky + 2f); close()
    }
    drawPath(helmetPath, color = Color(0xFFB0BEC5))
    drawPath(helmetPath, color = Color(0xFF546E7A), style = Stroke(width = 1.5f))
    drawLine(color = Color(0xFF1A1A2E), start = Offset(kx - 7f, ky - 5f), end = Offset(kx + 7f, ky - 5f), strokeWidth = 2.5f)

    // Plume
    val plumePath = androidx.compose.ui.graphics.Path().apply {
        moveTo(kx, ky - 18f)
        cubicTo(kx + 6f, ky - 30f, kx + 2f, ky - 36f, kx, ky - 34f)
        cubicTo(kx - 2f, ky - 36f, kx - 6f, ky - 30f, kx, ky - 18f)
    }
    drawPath(plumePath, color = Color(0xFFFFD54F))

    // Sword
    drawLine(color = Color(0xFFCFD8DC), start = Offset(kx + 13f, ky + 6f), end = Offset(kx + 20f, ky + 32f), strokeWidth = 2f)
    drawLine(color = Color(0xFFD4AF37), start = Offset(kx + 10f, ky + 8f), end = Offset(kx + 17f, ky + 5f), strokeWidth = 3f)
    drawLine(color = Color(0xFF8D6E63), start = Offset(kx + 13f, ky + 6f), end = Offset(kx + 15f, ky + 12f), strokeWidth = 3.5f)

    // Shield
    val shieldPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(kx - 12f, ky + 4f); lineTo(kx - 22f, ky + 4f); lineTo(kx - 22f, ky + 20f)
        cubicTo(kx - 22f, ky + 28f, kx - 12f, ky + 32f, kx - 12f, ky + 32f)
        cubicTo(kx - 12f, ky + 32f, kx - 12f, ky + 28f, kx - 12f, ky + 20f); close()
    }
    drawPath(shieldPath, color = Color(0xFF1565C0))
    drawPath(shieldPath, color = Color(0xFFD4AF37), style = Stroke(width = 1.5f))
    drawLine(color = Color(0xFFD4AF37), start = Offset(kx - 17f, ky + 11f), end = Offset(kx - 17f, ky + 22f), strokeWidth = 2f)
    drawLine(color = Color(0xFFD4AF37), start = Offset(kx - 21f, ky + 15f), end = Offset(kx - 13f, ky + 15f), strokeWidth = 2f)
}

// ── HUD ──────────────────────────────────────────────────────────────────────

@Composable
private fun MapHud(
    completedCount: Int,
    totalCount: Int,
    isMoving: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF2C1A00),
            titleContentColor = Color(0xFFD4AF37),
            textContentColor = Color(0xFFAA9977),
            title = { Text("Reset Progress?", fontWeight = FontWeight.Bold) },
            text = { Text("All completed settlements and scores will be erased.\nAre you sure?") },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false; onReset() }) {
                    Text("Reset", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color(0xFFD4AF37))
                }
            }
        )
    }

    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Color(0xCC1A0F00)) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚔️  Quiz Knight", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Settlements: $completedCount / $totalCount completed", color = Color(0xFFAA9977), fontSize = 12.sp)
            if (isMoving) Text("⚔️ Knight is marching…", color = Color(0xFF90CAF9), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { showResetDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text("🔄 Reset Progress", color = Color(0xFF887755), fontSize = 11.sp)
            }
        }
    }
}
