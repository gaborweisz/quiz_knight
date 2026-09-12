package com.trainig.quiz_knight.ui.screens.intro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainig.quiz_knight.R
import com.trainig.quiz_knight.ui.localization.localizedStringResource

// Parchment colours
private val ParchmentLight  = Color(0xFFF5E6C0)
private val ParchmentMid    = Color(0xFFE8C97A)
private val ParchmentDark   = Color(0xFFCFA84A)
private val InkBrown        = Color(0xFF2C1600)
private val InkFaded        = Color(0xFF5C3A10)
private val BgDark          = Color(0xFF1A0F00)
private val Gold            = Color(0xFFD4AF37)

// Each entry is a @StringRes id for one line of the scroll, or null for a blank spacer line.
private val introLineResIds: List<Int?> = listOf(
    R.string.intro_line_1,
    null,
    R.string.intro_line_2,
    R.string.intro_line_3,
    null,
    R.string.intro_line_4,
    R.string.intro_line_5,
    R.string.intro_line_6,
    R.string.intro_line_7,
    R.string.intro_line_8,
    null,
    R.string.intro_line_9,
    R.string.intro_line_10,
    R.string.intro_line_11,
    null,
    R.string.intro_line_12,
    R.string.intro_line_13,
    null,
    R.string.intro_line_14,
    R.string.intro_line_15,
    null,
    R.string.intro_line_16
)

@Composable
fun IntroScreen(onContinue: () -> Unit, onShown: () -> Unit, onSkip: () -> Unit) {

    // Fade in
    val alpha = remember { Animatable(0f) }
    // Subtle parchment sway
    val sway = rememberInfiniteTransition(label = "sway")
    val swayDeg by sway.animateFloat(
        initialValue = -0.4f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOut), RepeatMode.Reverse),
        label = "swayDeg"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(1200))
        // Notify host that the intro is visible (used to start intro music)
        onShown()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .pointerInput(Unit) { detectTapGestures { onContinue() } },
        contentAlignment = Alignment.Center
    ) {

        // ── Decorative background candle-glow vignette ────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55C8900A), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension * 0.85f
                ),
                size = size
            )
        }

        // ── Parchment card ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .alpha(alpha.value)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
                .rotate(swayDeg)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(ParchmentLight, ParchmentMid, ParchmentLight, ParchmentDark)
                    )
                )
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top ornament ──────────────────────────────────────────────
            ParchmentDivider()
            Spacer(Modifier.height(12.dp))

            // ── Title ─────────────────────────────────────────────────────
            Text(
                text = localizedStringResource(R.string.intro_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkBrown,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = localizedStringResource(R.string.intro_subtitle),
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = InkFaded,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            ParchmentDivider()
            Spacer(Modifier.height(16.dp))

            // ── Scroll text ───────────────────────────────────────────────
            introLineResIds.forEach { resId ->
                if (resId == null) {
                    Spacer(Modifier.height(6.dp))
                } else {
                    Text(
                        text = localizedStringResource(resId),
                        fontSize = 13.5.sp,
                        fontFamily = FontFamily.Serif,
                        color = InkBrown,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            ParchmentDivider()
            Spacer(Modifier.height(20.dp))

            // ── Seal ──────────────────────────────────────────────────────
            Text(text = "🔰", fontSize = 32.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = localizedStringResource(R.string.intro_seal_caption),
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                color = InkFaded,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // ── CTA button ────────────────────────────────────────────────
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3D2800),
                    contentColor = Gold
                )
            ) {
                Text(
                    localizedStringResource(R.string.intro_accept_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            // Skip intro button
            Button(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = InkFaded
                )
            ) {
                Text(localizedStringResource(R.string.intro_skip_button), fontSize = 14.sp)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                localizedStringResource(R.string.intro_tap_hint),
                fontSize = 10.sp,
                color = InkFaded,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

/** A hand-drawn looking horizontal divider using Canvas. */
@Composable
private fun ParchmentDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val w = size.width
        val cy = size.height / 2f
        val path = Path().apply {
            moveTo(0f, cy)
            // wavy line via quadratic bezier segments
            var x = 0f
            val step = w / 10f
            var up = true
            while (x < w) {
                val nx = (x + step).coerceAtMost(w)
                val mx = (x + nx) / 2f
                val my = if (up) cy - 4f else cy + 4f
                quadraticTo(mx, my, nx, cy)
                x = nx
                up = !up
            }
        }
        drawPath(path, color = ParchmentDark, style = Stroke(width = 1.8f))

        // Small diamond ornaments at both ends
        val d = 5f
        drawRect(
            color = ParchmentDark,
            topLeft = Offset(-d / 2f, cy - d / 2f),
            size = Size(d, d)
        )
        drawRect(
            color = ParchmentDark,
            topLeft = Offset(w - d / 2f, cy - d / 2f),
            size = Size(d, d)
        )
    }
}
