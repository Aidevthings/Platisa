package com.example.platisa

import android.content.Context
import android.content.res.Configuration
import androidx.fragment.app.FragmentActivity

/**
 * Base Activity that caps font scaling to prevent layouts from breaking
 * when users have extreme accessibility settings enabled.
 * 
 * This ensures the app respects user preferences up to 1.3x, beyond which
 * layouts would become unusable due to text overflow and element overlap.
 */
abstract class BaseActivity : FragmentActivity() {
    companion object {
        /**
         * Maximum allowed font scale factor.
         * - 1.0 = normal
         * - 1.3 = 130% (our cap)
         * - System can go up to 2.0+ on some devices
         */
        const val MAX_FONT_SCALE = 1.3f
    }
    
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        // Cap font scaling at MAX_FONT_SCALE to preserve layout integrity
        config.fontScale = config.fontScale.coerceAtMost(MAX_FONT_SCALE)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
