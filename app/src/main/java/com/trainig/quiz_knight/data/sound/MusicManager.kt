package com.trainig.quiz_knight.data.sound

import android.content.Context
import android.media.MediaPlayer
import com.trainig.quiz_knight.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all music playback for Quiz Knight.
 *
 * Tracks:
 *  • intro          – played once when the game first starts
 *  • background_1-4 – looped in round-robin order during normal gameplay
 *  • defeat_1/2     – one picked at random, played once on defeat
 *  • victory        – played once on settlement/game victory
 *
 * Background does NOT resume after defeat/victory until resumeBackground() is
 * called explicitly (i.e. when the player returns to the map).
 *
 * Race-condition safety:
 *  • Call prepareEventMusic() from the ViewModel init block (synchronous, main
 *    thread) BEFORE composition begins. This sets mode and stops background so
 *    that MainActivity.onResume() — which fires before LaunchedEffect — sees
 *    the non-BACKGROUND mode and does nothing.
 *  • Then call playVictory() / playDefeat() from a LaunchedEffect to start the
 *    actual clip.
 */
@Singleton
class MusicManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private enum class Mode { BACKGROUND, DEFEAT, VICTORY }

    private val backgroundRes = listOf(
        R.raw.background_1,
        R.raw.background_2,
        R.raw.background_3,
        R.raw.background_4
    )
    private val defeatRes = listOf(R.raw.defeat_1, R.raw.defeat_2)

    private var currentPlayer: MediaPlayer? = null
    private var nextBgIndex = 0
    private var introPlayed = false
    private var enabled = true
    private var mode = Mode.BACKGROUND

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Called from MainActivity.onResume.
     * Only (re)starts background music when in BACKGROUND mode — silently
     * ignored if defeat or victory music is in control.
     */
    fun onResume() {
        if (!enabled) return
        if (mode != Mode.BACKGROUND) return
        if (!introPlayed) {
            playIntro()
        } else {
            resumeOrContinueBackground()
        }
    }

    /** Called from MainActivity.onPause — pause mid-track, preserve position. */
    fun onPause() {
        currentPlayer?.let { if (it.isPlaying) it.pause() }
    }

    /** Called when the music on/off toggle changes. */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            if (mode == Mode.BACKGROUND) resumeOrContinueBackground()
        } else {
            stopCurrent()
        }
    }

    /**
     * STEP 1 — call this synchronously from the ViewModel init block.
     * Sets mode to VICTORY and stops background immediately, so that the
     * subsequent MainActivity.onResume() call (which fires before LaunchedEffect)
     * sees the non-BACKGROUND mode and does nothing.
     */
    fun prepareVictory() {
        mode = Mode.VICTORY
        stopCurrent()
    }

    /**
     * STEP 1 — call this synchronously from the ViewModel init block.
     * Sets mode to DEFEAT and stops background immediately.
     */
    fun prepareDefeat() {
        mode = Mode.DEFEAT
        stopCurrent()
    }

    /**
     * STEP 2 — call this from a LaunchedEffect after the screen is composed.
     * Plays triumph_1 then triumph_2 sequentially, then resumes background.
     * Used for the final Victory screen (all settlements conquered).
     */
    fun playTriumph() {
        if (!enabled) return
        stopCurrent()
        // Chain: triumph_1 → triumph_2 → background
        currentPlayer = buildOneShot(R.raw.triumph_1, onComplete = {
            if (mode == Mode.VICTORY) {
                currentPlayer = buildOneShot(R.raw.triumph_2, onComplete = {
                    // Both triumph tracks done — fall back to background
                    resumeBackground()
                })
                currentPlayer?.start()
            }
        })
        currentPlayer?.start()
    }

    /**
     * STEP 2 — call this from a LaunchedEffect after the screen is composed.
     * Actually starts the victory clip (used for single-settlement conquest on ResultScreen).
     */
    fun playVictory() {
        if (!enabled) return
        // mode is already VICTORY (set by prepareVictory)
        stopCurrent()
        currentPlayer = buildOneShot(R.raw.victory, onComplete = {
            // Stay silent — background resumes only when player returns to map
        })
        currentPlayer?.start()
    }

    /**
     * STEP 2 — call this from a LaunchedEffect after the screen is composed.
     * Actually starts a random defeat clip.
     */
    fun playDefeat() {
        if (!enabled) return
        // mode is already DEFEAT (set by prepareDefeat)
        stopCurrent()
        val res = defeatRes.random()
        currentPlayer = buildOneShot(res, onComplete = {
            // Stay silent — background resumes only when player returns to map
        })
        currentPlayer?.start()
    }

    /**
     * Call when the player navigates back to the map.
     * Switches back to BACKGROUND mode and starts the next track.
     */
    fun resumeBackground() {
        mode = Mode.BACKGROUND
        if (!enabled) return
        startNextBackground()
    }

    /** Stop everything and release resources (app destroyed). */
    fun releaseAll() {
        stopCurrent()
        introPlayed = false
        nextBgIndex = 0
        mode = Mode.BACKGROUND
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun playIntro() {
        stopCurrent()
        introPlayed = true
        currentPlayer = buildOneShot(R.raw.intro, onComplete = {
            if (mode == Mode.BACKGROUND) startNextBackground()
        })
        currentPlayer?.start()
    }

    /** Public helper to start the intro music immediately (used while IntroScreen is visible).
     * Safe to call multiple times; will do nothing if the intro is already playing or music is disabled. */
    fun playIntroNow() {
        if (!enabled) return
        // If intro is already playing, don't interrupt it
        val player = currentPlayer
        if (introPlayed && player != null && player.isPlaying) return
        stopCurrent()
        playIntro()
    }

    private fun resumeOrContinueBackground() {
        val player = currentPlayer
        when {
            player != null && !player.isPlaying -> player.start()
            player == null -> startNextBackground()
        }
    }

    private fun startNextBackground() {
        stopCurrent()
        val res = backgroundRes[nextBgIndex % backgroundRes.size]
        nextBgIndex++
        currentPlayer = buildOneShot(res, onComplete = {
            if (mode == Mode.BACKGROUND) startNextBackground()
        })
        currentPlayer?.start()
    }

    private fun stopCurrent() {
        currentPlayer?.let {
            runCatching { if (it.isPlaying) it.stop() }
            runCatching { it.reset() }
            runCatching { it.release() }
        }
        currentPlayer = null
    }

    private fun buildOneShot(resId: Int, onComplete: () -> Unit): MediaPlayer {
        val mp = MediaPlayer.create(context, resId) ?: return MediaPlayer()
        mp.setVolume(0.7f, 0.7f)
        mp.setOnCompletionListener {
            it.reset()
            it.release()
            if (currentPlayer === it) currentPlayer = null
            onComplete()
        }
        return mp
    }
}
