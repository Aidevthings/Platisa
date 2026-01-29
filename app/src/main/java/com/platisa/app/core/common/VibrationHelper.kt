package com.platisa.app.core.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.platisa.app.core.data.preferences.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrationHelper @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrate(type: HapticType = HapticType.LIGHT) {
        if (!preferenceManager.hapticEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (type) {
                // Boosted durations since <50ms is often imperceptible on some motors/emulators
                HapticType.LIGHT -> VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE)   // Was 15
                HapticType.SUCCESS -> VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE) // Was 30
                HapticType.HEAVY -> VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE)  // Was 70
                HapticType.ERROR -> VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)  // Was 150
            }
            vibrator.vibrate(effect)
        } else {
            // Fallback for older devices
            @Suppress("DEPRECATION")
            val duration = when (type) {
                HapticType.LIGHT -> 10L
                HapticType.HEAVY -> 50L
                HapticType.ERROR -> 100L // Double pulse simulation needed, but simple for now
                HapticType.SUCCESS -> 20L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    enum class HapticType {
        LIGHT, HEAVY, ERROR, SUCCESS
    }
}
