package com.trainig.quiz_knight.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.trainig.quiz_knight.domain.model.AppLanguage
import com.trainig.quiz_knight.domain.model.MapPosition
import com.trainig.quiz_knight.domain.model.QuizTopic
import com.trainig.quiz_knight.domain.model.Settlement
import com.trainig.quiz_knight.domain.model.SettlementType
import com.trainig.quiz_knight.ui.localization.localizedStringResource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
// Duration of one gallop stride cycle (leg swing + bounce), in ms
private const val GALLOP_STRIDE_MS = 260
// Duration of the heading (turn-to-face-travel-direction) animation, in ms
private const val TURN_DURATION_MS = 200

@Composable
fun MapScreen(
    onEnterSettlement: (String) -> Unit,
    onVictory: () -> Unit,
    onStatistics: () -> Unit = {},
    onReplayIntro: () -> Unit = {},
    onQuit: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Resolve all topic names once per composition (in composable scope — the Canvas draw
    // lambda below is a plain DrawScope, not @Composable, so localizedStringResource can't
    // be called from inside it). Recomputes automatically when the selected language changes.
    val topicNames: Map<QuizTopic, String> = buildMap {
        QuizTopic.entries.forEach { topic -> put(topic, localizedStringResource(topic.displayNameRes)) }
    }

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

    // Create a remembered per-edge smooth noise grid. Each edge gets a small grid of noise
    // values (-1..1). We'll sample this grid smoothly for any t in [0,1] using smoothstep
    // interpolation to produce coherent, natural-looking curves.
    val edgeNoiseGrid = remember(uiState.settlements) {
        val rnd = kotlin.random.Random(System.currentTimeMillis())
        val map = mutableMapOf<Pair<String, String>, List<Float>>()
        val gridSize = 8
        uiState.settlements.forEach { s ->
            s.connectedTo.forEach { toId ->
                val edge = if (s.id < toId) s.id to toId else toId to s.id
                if (!map.containsKey(edge)) {
                    val grid = List(gridSize + 1) { rnd.nextFloat() * 2f - 1f } // -1..1
                    map[edge] = grid
                }
            }
        }
        map
    }

    // ── Animated knight position ─────────────────────────────────────────
    val knightX = remember { Animatable(0f) }
    val knightY = remember { Animatable(0f) }

    // Heading: direction the horse is currently facing, in degrees (0 = up/north,
    // clockwise positive), so the sprite always faces the direction of travel.
    val heading = remember { Animatable(0f) }

    // Gallop cycle: drives the leg stride and body bounce while the knight is moving.
    val gallopTransition = rememberInfiniteTransition(label = "gallop")
    val gallopPhase by gallopTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GALLOP_STRIDE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "gallopPhase"
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
        val toSettlement = uiState.settlements.firstOrNull { it.id == toId }
            ?: return@LaunchedEffect
        val target = toSettlement.position.toPx()

        // Turn to face the direction of travel, taking the shortest rotation path
        val dx = target.x - knightX.value
        val dy = target.y - knightY.value
        val targetHeading = if (dx != 0f || dy != 0f) {
            val rawDeg = atan2(dx, -dy) * 180f / PI.toFloat()
            val delta = ((rawDeg - heading.value + 540f) % 360f) - 180f
            heading.value + delta
        } else heading.value

        // Animate X, Y and heading in parallel
        coroutineScope {
            listOf(
                async { knightX.animateTo(target.x, animationSpec = tween(WALK_DURATION_MS, easing = EaseInOut)) },
                async { knightY.animateTo(target.y, animationSpec = tween(WALK_DURATION_MS, easing = EaseInOut)) },
                async { heading.animateTo(targetHeading, animationSpec = tween(TURN_DURATION_MS, easing = EaseInOut)) }
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

    // Replay dialog: shown after knight arrives at a completed settlement via long-press
    val replaySettlement = uiState.replaySettlementId?.let { id ->
        uiState.settlements.firstOrNull { it.id == id }
    }
    if (replaySettlement != null && !uiState.isMoving) {
        AlertDialog(
            onDismissRequest = { viewModel.onReplayDismissed() },
            containerColor = Color(0xFF2C1A00),
            titleContentColor = Color(0xFFD4AF37),
            textContentColor = Color(0xFFAA9977),
            title = { Text(localizedStringResource(R.string.replay_dialog_title, replaySettlement.name), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    localizedStringResource(
                        R.string.replay_dialog_body,
                        topicNames.getValue(replaySettlement.topic)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onReplayConfirmed() }) {
                    Text(localizedStringResource(R.string.replay_confirm_button), color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onReplayDismissed() }) {
                    Text(localizedStringResource(R.string.action_cancel), color = Color(0xFFAA9977))
                }
            }
        )
    }

    val textMeasurer = rememberTextMeasurer()

    // Precompute and remember road paths so they're not rebuilt on every recomposition/draw.
    // Recompute when settlements, canvasSize, or noise grids change.
    val rememberedRoadPaths by remember(uiState.settlements, canvasSize, edgeNoiseGrid) {
        mutableStateOf(run {
            if (canvasSize == IntSize.Zero) return@run emptyMap<Pair<String, String>, Path>()
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat() * MAP_HEIGHT_FRACTION
            val map = mutableMapOf<Pair<String, String>, Path>()
            val settlementMap = uiState.settlements.associateBy { it.id }
            for (s in uiState.settlements) {
                val from = s.position.toOffset(w, h)
                for (toId in s.connectedTo) {
                    val edge = if (s.id < toId) s.id to toId else toId to s.id
                    if (map.containsKey(edge)) continue
                    val toPos = settlementMap[toId]?.position ?: continue
                    val to = toPos.toOffset(w, h)

                    val dx = to.x - from.x
                    val dy = to.y - from.y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val px = -dy / len
                    val py = dx / len

                    val grid = edgeNoiseGrid[edge]
                    // fewer sample points -> fewer local bends
                    val sampleCount = 12
                    val pts = mutableListOf<Offset>()
                    pts.add(from)
                    for (i in 1..sampleCount) {
                        val t = i.toFloat() / (sampleCount + 1)
                        val baseX = from.x + dx * t
                        val baseY = from.y + dy * t
                        val falloff = sin(PI * t).toFloat()

                        val nFractal = grid?.let { sampleFractalNoise(it, t, octaves = 3) } ?: 0f
                        // reduce perpendicular jitter to make roads less bendy
                        var perpJitter = nFractal * len * 0.16f * falloff
                        val maxPerp = len * 0.30f
                        if (perpJitter > maxPerp) perpJitter = maxPerp
                        if (perpJitter < -maxPerp) perpJitter = -maxPerp

                        val n2 = grid?.let { sampleFractalNoise(it, (t + 0.17f) % 1f, octaves = 2) } ?: 0f
                        // reduce along-track jitter and angular variation for smoother roads
                        val alongJitter = n2 * len * 0.04f * falloff

                        val angle = n2 * 0.07f
                        val cosA = cos(angle)
                        val sinA = sin(angle)
                        val rpx = px * cosA - py * sinA
                        val rpy = px * sinA + py * cosA

                        val sampleX = baseX + rpx * perpJitter + (dx / len) * alongJitter
                        val sampleY = baseY + rpy * perpJitter + (dy / len) * alongJitter
                        pts.add(Offset(sampleX, sampleY))
                    }
                    pts.add(to)

                    // fewer Chaikin iterations -> preserves more of the simplified shape (less small bends)
                    val smoothPts = chaikin(pts)
                    val p = buildSmoothPath(smoothPts)
                    map[edge] = p
                }
            }
            map.toMap()
        }) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0F00))
    ) {
        // Prominent Replay Intro button (top-left) so users can always replay the intro
        IconButton(
            onClick = onReplayIntro,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(44.dp)
        ) {
            Text("🔁", fontSize = 20.sp)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(uiState.settlements, uiState.isMoving) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (uiState.isMoving) return@detectTapGestures
                            val mapH = size.height * MAP_HEIGHT_FRACTION
                            val tapped = uiState.settlements.firstOrNull { s ->
                                val c = s.position.toOffset(size.width.toFloat(), mapH)
                                (tapOffset - c).getDistance() < nodeRadius(s.type)
                            }
                            tapped?.let { viewModel.onSettlementTapped(it.id) }
                        },
                        onLongPress = { tapOffset ->
                            if (uiState.isMoving) return@detectTapGestures
                            val mapH = size.height * MAP_HEIGHT_FRACTION
                            val tapped = uiState.settlements.firstOrNull { s ->
                                val c = s.position.toOffset(size.width.toFloat(), mapH)
                                (tapOffset - c).getDistance() < nodeRadius(s.type)
                            }
                            tapped?.let { viewModel.onSettlementLongPressed(it.id) }
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height * MAP_HEIGHT_FRACTION

            // ── Draw parchment map bitmap across the full canvas height ───
            if (mapBitmap != null) {
                drawImage(
                    image = mapBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(0, 0),
                    dstSize = IntSize(w.toInt(), size.height.toInt())
                )
            } else {
                drawRect(color = Color(0xFFC8A86B), size = Size(w, size.height))
            }

            // Draw precomputed road paths (stable and faster)
            val drawnEdges = mutableSetOf<Pair<String, String>>()
            for (s in uiState.settlements) {
                for (toId in s.connectedTo) {
                    val edge = if (s.id < toId) s.id to toId else toId to s.id
                    if (drawnEdges.add(edge)) {
                        val path = rememberedRoadPaths[edge]
                        if (path != null) {
                            // Outer shadow
                            drawPath(
                                path,
                                color = Color(0xFF3B1F00).copy(alpha = 0.45f),
                                style = Stroke(width = 18f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            // Road base
                            drawPath(
                                path,
                                color = Color(0xFFB08858),
                                style = Stroke(width = 13f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            // Surface
                            drawPath(
                                path,
                                color = Color(0xFFCFA878).copy(alpha = 0.75f),
                                style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            // Settlement nodes
            uiState.settlements.forEach { s ->
                drawSettlement(s, s.position.toOffset(w, h), textMeasurer, topicNames.getValue(s.topic))
            }

            // Knight — a horse and rider seen from above, facing the direction of travel
            drawHorseAndRider(
                basePos = Offset(knightX.value, knightY.value),
                headingDeg = heading.value,
                moving = uiState.isMoving,
                phase = gallopPhase
            )
        }

        MapHud(
            completedCount = uiState.completedIds.size,
            totalCount = uiState.settlements.size,
            isMoving = uiState.isMoving,
            musicEnabled = uiState.musicEnabled,
            onToggleMusic = { viewModel.toggleMusic() },
            language = uiState.language,
            onToggleLanguage = { viewModel.toggleLanguage() },
            onReset = { viewModel.resetProgress() },
            onStatistics = onStatistics,
            onReplayIntro = onReplayIntro,
            onQuit = onQuit,
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
                        Text(localizedStringResource(R.string.action_ok), color = Color.White)
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

private fun DrawScope.drawSettlement(settlement: Settlement, center: Offset, textMeasurer: TextMeasurer, topicLabel: String) {
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
        AnnotatedString(topicLabel),
        style = TextStyle(fontSize = 9.sp, color = TextColor.copy(alpha = 0.75f), fontWeight = FontWeight.Normal)
    )
    drawText(topicLayout, topLeft = Offset(center.x - topicLayout.size.width / 2f, center.y + radius + 6f + nameLayout.size.height))
}

/**
 * Draws a horse and rider as seen from above, always oriented to face [headingDeg]
 * (0° = up/north, clockwise positive). While [moving] the legs and tail cycle through
 * a galloping stride driven by [phase] (0..1, one full stride per cycle) and the whole
 * horse lifts slightly off its ground shadow at the peak of each stride.
 */
private fun DrawScope.drawHorseAndRider(basePos: Offset, headingDeg: Float, moving: Boolean, phase: Float) {
    val cx = basePos.x
    // Raise the whole horse above the settlement marker at basePos so it reads as
    // standing next to/above the node instead of directly on top of it.
    val cy = basePos.y - 46f

    val stride = if (moving) sin(phase * 2f * PI.toFloat()) else 0f
    val bounce = if (moving) abs(stride) * 6f else 0f

    // Ground shadow — stays flat on the map, unaffected by the gallop bounce
    rotate(degrees = headingDeg, pivot = Offset(cx, cy)) {
        drawOval(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(cx - 15f, cy + 22f),
            size = Size(30f, 12f)
        )
    }

    // Everything below is drawn "facing up" (forward = -y) then rotated to headingDeg,
    // and lifted by `bounce` to sell the galloping motion.
    val by = cy - bounce
    rotate(degrees = headingDeg, pivot = Offset(cx, cy)) {

        // Glow halo
        drawCircle(color = Color(0xFF90CAF9).copy(alpha = 0.22f), radius = 38f, center = Offset(cx, by + 6f))

        // ── Legs (drawn first so the body covers the joints) ──────────────
        val frontSwing = stride * 13f
        val backSwing = -stride * 13f
        val legColor = Color(0xFF3E2723)
        val hoofColor = Color(0xFF1A0F00)
        val legs = listOf(
            Offset(cx - 8f, by - 4f) to Offset(cx - 10f, by - 4f + frontSwing),   // front-left
            Offset(cx + 8f, by - 4f) to Offset(cx + 10f, by - 4f + frontSwing),   // front-right
            Offset(cx - 8f, by + 30f) to Offset(cx - 10f, by + 30f + backSwing),  // back-left
            Offset(cx + 8f, by + 30f) to Offset(cx + 10f, by + 30f + backSwing)   // back-right
        )
        legs.forEach { (start, end) ->
            drawLine(color = legColor, start = start, end = end, strokeWidth = 3f, cap = StrokeCap.Round)
            drawCircle(color = hoofColor, radius = 2f, center = end)
        }

        // Tail — sways opposite the stride
        val tailPath = Path().apply {
            moveTo(cx, by + 34f)
            quadraticTo(cx + stride * 10f, by + 44f, cx + stride * 6f, by + 54f)
        }
        drawPath(tailPath, color = legColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Horse body
        drawOval(color = Color(0xFF8D5A34), topLeft = Offset(cx - 11f, by - 6f), size = Size(22f, 46f))
        drawOval(color = Color(0xFF6D4426), topLeft = Offset(cx - 11f, by - 6f), size = Size(22f, 46f), style = Stroke(width = 1.5f))

        // Neck & head
        drawOval(color = Color(0xFF6D4426), topLeft = Offset(cx - 6f, by - 34f), size = Size(12f, 20f))
        // Ears
        drawLine(color = legColor, start = Offset(cx - 3f, by - 33f), end = Offset(cx - 5f, by - 41f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = legColor, start = Offset(cx + 3f, by - 33f), end = Offset(cx + 5f, by - 41f), strokeWidth = 2f, cap = StrokeCap.Round)
        // Mane
        drawLine(color = legColor, start = Offset(cx, by - 32f), end = Offset(cx, by - 4f), strokeWidth = 3f, cap = StrokeCap.Round)

        // ── Rider ───────────────────────────────────────────────────────
        val ry = by + 4f
        // Cape flares out behind the rider
        val capePath = Path().apply {
            moveTo(cx - 6f, ry)
            cubicTo(cx - 11f, ry + 10f, cx - 8f, ry + 20f, cx - 3f, ry + 22f)
            lineTo(cx + 3f, ry + 22f)
            cubicTo(cx + 8f, ry + 20f, cx + 11f, ry + 10f, cx + 6f, ry)
            close()
        }
        drawPath(capePath, color = Color(0xFF8B0000))
        // Shoulders / torso
        drawOval(color = Color(0xFF90A4AE), topLeft = Offset(cx - 7f, ry - 3f), size = Size(14f, 14f))
        drawOval(color = Color(0xFF546E7A), topLeft = Offset(cx - 7f, ry - 3f), size = Size(14f, 14f), style = Stroke(width = 1.2f))
        // Helmet
        drawCircle(color = Color(0xFFB0BEC5), radius = 6.5f, center = Offset(cx, ry - 9f))
        drawCircle(color = Color(0xFF546E7A), radius = 6.5f, center = Offset(cx, ry - 9f), style = Stroke(width = 1.2f))
        drawLine(color = Color(0xFF1A1A2E), start = Offset(cx - 4f, ry - 11f), end = Offset(cx + 4f, ry - 11f), strokeWidth = 2f)
        // Plume
        val plumePath = Path().apply {
            moveTo(cx, ry - 15f)
            cubicTo(cx + 4f, ry - 25f, cx + 1f, ry - 30f, cx, ry - 28f)
            cubicTo(cx - 1f, ry - 30f, cx - 4f, ry - 25f, cx, ry - 15f)
        }
        drawPath(plumePath, color = Color(0xFFFFD54F))
        // Shield (left)
        drawRect(color = Color(0xFF1565C0), topLeft = Offset(cx - 13f, ry - 2f), size = Size(6f, 10f))
        drawRect(color = Color(0xFFD4AF37), topLeft = Offset(cx - 13f, ry - 2f), size = Size(6f, 10f), style = Stroke(width = 1f))
        // Sword (right)
        drawLine(color = Color(0xFFCFD8DC), start = Offset(cx + 9f, ry - 4f), end = Offset(cx + 15f, ry + 8f), strokeWidth = 2f)
        drawLine(color = Color(0xFFD4AF37), start = Offset(cx + 7f, ry - 2f), end = Offset(cx + 12f, ry - 5f), strokeWidth = 2.5f)
    }
}

// ── HUD ──────────────────────────────────────────────────────────────────────

private enum class HudMode { COLLAPSED, REDUCED, FULL }

@Composable
private fun MapHud(
    completedCount: Int,
    totalCount: Int,
    isMoving: Boolean,
    musicEnabled: Boolean,
    onToggleMusic: () -> Unit,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onReset: () -> Unit,
    onStatistics: () -> Unit,
    onReplayIntro: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    // Start collapsed — first tap shows reduced, second shows full, third collapses again
    var hudMode by remember { mutableStateOf(HudMode.REDUCED) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF2C1A00),
            titleContentColor = Color(0xFFD4AF37),
            textContentColor = Color(0xFFAA9977),
            title = { Text(localizedStringResource(R.string.reset_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(localizedStringResource(R.string.reset_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false; onReset() }) {
                    Text(localizedStringResource(R.string.reset_confirm_button), color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(localizedStringResource(R.string.action_cancel), color = Color(0xFFD4AF37))
                }
            }
        )
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            containerColor = Color(0xFF2C1A00),
            titleContentColor = Color(0xFFD4AF37),
            textContentColor = Color(0xFFAA9977),
            title = { Text(localizedStringResource(R.string.quit_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(localizedStringResource(R.string.quit_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { showQuitDialog = false; onQuit() }) {
                    Text(localizedStringResource(R.string.quit_confirm_button), color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text(localizedStringResource(R.string.action_cancel), color = Color(0xFFD4AF37))
                }
            }
        )
    }

    Surface(
        modifier = modifier.clickable {
            hudMode = if (hudMode == HudMode.FULL) HudMode.REDUCED else HudMode.FULL
        },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC1A0F00)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Always visible: title + chevron ──────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    localizedStringResource(R.string.hud_title),
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (hudMode == HudMode.FULL) "▲" else "▼",
                    color = Color(0xFFAA9977),
                    fontSize = 11.sp
                )
            }

            // ── Reduced + Full: progress row ─────────────────────────────
            AnimatedVisibility(
                visible = hudMode == HudMode.REDUCED || hudMode == HudMode.FULL,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        localizedStringResource(R.string.hud_settlements_progress, completedCount, totalCount),
                        color = Color(0xFFAA9977),
                        fontSize = 12.sp
                    )
                    if (isMoving) Text(
                        localizedStringResource(R.string.hud_knight_marching),
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp
                    )
                }
            }

            // ── Full only: all menu buttons ──────────────────────────────
            AnimatedVisibility(
                visible = hudMode == HudMode.FULL,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(4.dp))
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onStatistics,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(localizedStringResource(R.string.hud_statistics_button), color = Color(0xFFD4AF37), fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = onToggleMusic,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (musicEnabled) localizedStringResource(R.string.hud_music_on) else localizedStringResource(R.string.hud_music_off),
                                color = if (musicEnabled) Color(0xFFD4AF37) else Color(0xFF887755),
                                fontSize = 11.sp
                            )
                        }
                        TextButton(
                            onClick = onToggleLanguage,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (language == AppLanguage.ENGLISH) localizedStringResource(R.string.hud_language_english) else localizedStringResource(R.string.hud_language_hungarian),
                                color = Color(0xFFD4AF37),
                                fontSize = 11.sp
                            )
                        }
                    }
                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onReplayIntro,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(localizedStringResource(R.string.hud_replay_intro_button), color = Color(0xFFD4AF37), fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { showResetDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(localizedStringResource(R.string.hud_reset_progress_button), color = Color(0xFF887755), fontSize = 11.sp)
                        }
                    }
                    // Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showQuitDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(localizedStringResource(R.string.hud_quit_game_button), color = Color(0xFFEF5350), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// Helper: build a smooth Path passing through the provided points using a Catmull–Rom to Bezier conversion
private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    // For each segment between P_i and P_{i+1}, compute two cubic control points
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrNull(i - 1) ?: points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrNull(i + 2) ?: points[i + 1]

        // Catmull-Rom to Bezier conversion (tension = 0.5 approximated by factor 1/6)
        val cp1x = p1.x + (p2.x - p0.x) * (1f / 6f)
        val cp1y = p1.y + (p2.y - p0.y) * (1f / 6f)
        val cp2x = p2.x - (p3.x - p1.x) * (1f / 6f)
        val cp2y = p2.y - (p3.y - p1.y) * (1f / 6f)

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
    return path
}

// Sample a noise grid of values (length N+1) at position t ∈ [0,1] using smoothstep interpolation
private fun sampleNoiseGrid(grid: List<Float>, t: Float): Float {
    if (grid.isEmpty()) return 0f
    val n = grid.size - 1
    if (n <= 0) return grid.first()
    val pos = (t * n).coerceIn(0f, n.toFloat())
    val i = pos.toInt().coerceAtMost(n - 1)
    val local = pos - i
    val u = local
    val fade = u * u * (3f - 2f * u) // smoothstep
    val a = grid[i]
    val b = grid[i + 1]
    return a * (1f - fade) + b * fade
}

// Fractal noise: combine several frequencies of the base sampled grid to produce
// multi-scale coherent variation without bringing in an external noise library.
private fun sampleFractalNoise(grid: List<Float>, t: Float, octaves: Int = 3, lacunarity: Float = 2f, persistence: Float = 0.5f): Float {
    if (grid.isEmpty() || octaves <= 0) return 0f
    var amplitude = 1f
    var frequency = 1f
    var sum = 0f
    var maxAmp = 0f
    repeat(octaves) {
        val sample = sampleNoiseGrid(grid, (t * frequency) % 1f)
        sum += sample * amplitude
        maxAmp += amplitude
        amplitude *= persistence
        frequency *= lacunarity
    }
    return if (maxAmp == 0f) 0f else sum / maxAmp
}

// Chaikin subdivision (corner cutting) to smooth a polyline. Uses a fixed small number of iterations for stability.
private fun chaikin(points: List<Offset>): List<Offset> {
     val iterations = 2
     if (points.size < 2) return points
     var pts = points
     repeat(iterations) {
         val result = mutableListOf<Offset>()
         result.add(pts.first())
         for (i in 0 until pts.size - 1) {
             val p0 = pts[i]
             val p1 = pts[i + 1]
             val qx = 0.75f * p0.x + 0.25f * p1.x
             val qy = 0.75f * p0.y + 0.25f * p1.y
             val rx = 0.25f * p0.x + 0.75f * p1.x
             val ry = 0.25f * p0.y + 0.75f * p1.y
             result.add(Offset(qx, qy))
             result.add(Offset(rx, ry))
         }
         result.add(pts.last())
         pts = result
     }
     return pts
}
