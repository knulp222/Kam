package com.example.service

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Real-time audio processor tailored for bedtime listening:
 * 1. Attenuates sharp brass / high frequencies (trompettes / bruits métalliques) via a tailored EQ curve.
 * 2. Softens sudden intro trumpet fanfares during the first seconds of an episode.
 * 3. Preserves soft, intelligible dialogue tones.
 */
class NightAudioProcessor {

    private var equalizer: Equalizer? = null
    private var isEnabled = true

    fun attach(audioSessionId: Int, enabled: Boolean) {
        this.isEnabled = enabled
        release()
        if (!enabled || audioSessionId == 0) return

        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true

            val numBands = eq.numberOfBands.toInt()
            val (minLevel, maxLevel) = eq.bandLevelRange.let { it[0] to it[1] }

            for (i in 0 until numBands) {
                val band = i.toShort()
                val centerFreqHz = eq.getCenterFreq(band) / 1000

                // Attenuate harsh frequencies (brass fanfare / cymbal / clash above 1500 Hz)
                val targetDb = when {
                    centerFreqHz < 300 -> 0 // Neutral deep bass
                    centerFreqHz in 300..1000 -> +1 // Subtle voice presence boost
                    centerFreqHz in 1000..3000 -> -3 // Mild brass attenuation
                    centerFreqHz in 3000..8000 -> -6 // Significant trumpet / sharpness cut
                    else -> -8 // Very soft ultra-highs
                }

                val millibels = (targetDb * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                eq.setBandLevel(band, millibels)
            }

            equalizer = eq
        } catch (e: Exception) {
            Log.w("NightAudioProcessor", "Equalizer setup not supported on this device/session", e)
            equalizer = null
        }
    }

    /**
     * Calculates intro softening factor for trumpet / fanfare reduction at start of track.
     * During the first 12 seconds, subtly dampens peak volume if enabled.
     */
    fun calculateIntroFactor(positionMs: Long): Float {
        if (!isEnabled) return 1.0f
        return when {
            positionMs < 2000L -> 0.35f
            positionMs < 10000L -> 0.45f
            positionMs < 13000L -> {
                // Smooth transition back to 1.0f
                val progress = (positionMs - 10000L) / 3000f
                0.45f + (progress * 0.55f)
            }
            else -> 1.0f
        }
    }

    fun setEnabled(enabled: Boolean, audioSessionId: Int) {
        this.isEnabled = enabled
        if (enabled) {
            if (equalizer == null && audioSessionId != 0) {
                attach(audioSessionId, true)
            } else {
                try { equalizer?.enabled = true } catch (_: Exception) {}
            }
        } else {
            try { equalizer?.enabled = false } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
    }
}
