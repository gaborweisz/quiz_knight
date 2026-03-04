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

    /** Plays a metallic clank / footstep sound (knight marching in armour). */
    suspend fun playFootstep() = withContext(Dispatchers.IO) {
        playPcm(buildFootstepSamples())
    }

    /** Plays a short triumphant chime when the knight arrives at a settlement. */
    suspend fun playArrival() = withContext(Dispatchers.IO) {
        playPcm(buildArrivalSamples())
    }

    // ── PCM builders ──────────────────────────────────────────────────────

    /**
     * Metallic clank: short burst of noise with a sharp metallic resonance at ~900 Hz,
     * shaped by a fast exponential decay envelope.
     */
    private fun buildFootstepSamples(): ShortArray {
        val durationMs = 220
        val n = sampleRate * durationMs / 1000
        val samples = ShortArray(n)
        val rng = java.util.Random(42)
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            // Fast decay envelope
            val env = exp(-t * 28f)
            // Metallic resonance (two close frequencies create beating)
            val tone = sin(2 * PI * 880.0 * t) * 0.5 + sin(2 * PI * 912.0 * t) * 0.3
            // White noise component for impact "thud"
            val noise = (rng.nextFloat() * 2f - 1f) * 0.4f
            val sample = ((tone + noise) * env * Short.MAX_VALUE * 0.6).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            samples[i] = sample.toShort()
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

