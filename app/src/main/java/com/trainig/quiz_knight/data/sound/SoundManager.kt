package com.trainig.quiz_knight.data.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Synthesises short sound effects for knight movement using [AudioTrack].
 * No audio files are required — everything is generated in-memory.
 */
@Singleton
class SoundManager @Inject constructor() {

    private val sampleRate = 44100

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /**
     * Plays a galloping hoofbeat sequence for the horse's journey between settlements.
     * [durationMs] should roughly match the on-screen travel animation length.
     */
    suspend fun playGallop(durationMs: Int = 900) = withContext(Dispatchers.IO) {
        playPcm(buildGallopSamples(durationMs))
    }

    /** Plays a short triumphant chime when the knight arrives at a settlement. */
    suspend fun playArrival() = withContext(Dispatchers.IO) {
        playPcm(buildArrivalSamples())
    }

    // ── PCM builders ──────────────────────────────────────────────────────

    /**
     * Galloping hoofbeats: a repeating "stride" of three close, low-pitched impacts
     * (thud shaped by band-limited noise + a low resonance, like a hoof striking dirt)
     * with a short pause between strides — the classic uneven da-da-DUM.. cadence of
     * a cantering/galloping horse, tiled to fill [durationMs].
     */
    private fun buildGallopSamples(durationMs: Int): ShortArray {
        val n = sampleRate * durationMs / 1000
        val samples = ShortArray(n)
        val rng = java.util.Random(7)

        val strideMs = 340
        // (offset within stride in ms, pitch in Hz, relative amplitude)
        val beats = listOf(
            Triple(0, 150f, 0.9f),
            Triple(90, 168f, 0.85f),
            Triple(190, 130f, 0.55f)
        )

        var strideStartMs = 0
        while (strideStartMs < durationMs) {
            for ((offsetMs, pitch, amp) in beats) {
                val beatStartMs = strideStartMs + offsetMs
                if (beatStartMs >= durationMs) continue
                val startSample = sampleRate * beatStartMs / 1000
                val beatDurationMs = 70
                val beatN = sampleRate * beatDurationMs / 1000
                for (j in 0 until beatN) {
                    val idx = startSample + j
                    if (idx >= n) break
                    val t = j.toFloat() / sampleRate
                    // Fast decay envelope for a percussive hoof "thud"
                    val env = exp(-t * 55f)
                    val thud = sin(2 * PI * pitch * t) * 0.6
                    val noise = (rng.nextFloat() * 2f - 1f) * 0.5f
                    val sample = ((thud + noise) * env * amp * Short.MAX_VALUE * 0.7).toInt()
                    val mixed = (samples[idx] + sample).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[idx] = mixed.toShort()
                }
            }
            strideStartMs += strideMs
        }
        return samples
    }

    /**
     * Arrival chime: two ascending pure tones (C5 → E5) each with a smooth
     * attack/decay envelope, giving a gentle "ding-ding" feel.
     */
    private fun buildArrivalSamples(): ShortArray {
        val durationMs = 520
        val n = sampleRate * durationMs / 1000
        val samples = ShortArray(n)

        // Note timings and frequencies (C5=523Hz, E5=659Hz, G5=784Hz)
        val notes = listOf(
            Triple(0, 523.25, 0.18),   // C5 starts at 0 ms
            Triple(160, 659.25, 0.16), // E5 starts at 160 ms
            Triple(300, 784.0, 0.14)   // G5 starts at 300 ms
        )

        for ((startMs, freq, amp) in notes) {
            val startSample = sampleRate * startMs / 1000
            val noteDuration = sampleRate * 300 / 1000
            for (j in 0 until noteDuration) {
                val idx = startSample + j
                if (idx >= n) break
                val t = j.toFloat() / sampleRate
                // Bell-like envelope: quick attack, slow decay
                val attack = (j.toFloat() / (sampleRate * 0.015f)).coerceAtMost(1f)
                val decay = exp(-t * 7.0)
                val env = attack * decay
                val tone = sin(2 * PI * freq * t) +
                        sin(2 * PI * freq * 2.0 * t) * 0.3 +  // 2nd harmonic
                        sin(2 * PI * freq * 3.0 * t) * 0.1    // 3rd harmonic
                val s = (tone * env * amp * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[idx] = (samples[idx] + s)
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return samples
    }

    // ── Playback ──────────────────────────────────────────────────────────

    private fun playPcm(samples: ShortArray) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(samples.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            // Wait for playback to finish before releasing
            val durationMs = (samples.size.toLong() * 1000L / sampleRate) + 50L
            Thread.sleep(durationMs)
        } finally {
            track.stop()
            track.release()
        }
    }
}

