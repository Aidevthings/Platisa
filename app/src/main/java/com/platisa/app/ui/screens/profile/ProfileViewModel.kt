package com.platisa.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.common.BaseViewModel
import com.platisa.app.core.domain.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.platisa.app.R
import javax.inject.Inject

data class SplashOption(
    val id: String,
    val drawableRes: Int
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager
) : BaseViewModel() {

    private val _userName = MutableStateFlow(secureStorage.getUserName())
    val userName = _userName.asStateFlow()

    private val _avatarPath = MutableStateFlow(secureStorage.getAvatarPath())
    val avatarPath = _avatarPath.asStateFlow()

    private val _celebrationImagePath = MutableStateFlow(secureStorage.getCelebrationImagePath())
    val celebrationImagePath = _celebrationImagePath.asStateFlow()

    private val _splashScreenStyle = MutableStateFlow(preferenceManager.splashScreenStyle)
    val splashScreenStyle = _splashScreenStyle.asStateFlow()

    // Predefined avatars (resource IDs as strings)
    val predefinedAvatars = listOf(
        "avatar_3", "avatar_4",
        "avatar_5", "avatar_6", "avatar_7", "avatar_8", "avatar_9", "avatar_10"
    )

    val celebrationImages = listOf(
        "celebration_4", "celebration_dance", "macka", "celebration_2", "celebration_3", 
        "celebration_5", "celebration_6"
    )

    val splashOptions = listOf(
        SplashOption("LIGHT", R.drawable.platisa_greetings_image_light),
        SplashOption("DARK", R.drawable.splash_background),
        SplashOption("SPLASH_1", R.drawable.splash_option_1),
        SplashOption("SPLASH_2", R.drawable.splash_option_2),
        SplashOption("SPLASH_3", R.drawable.splash_option_3),
        SplashOption("SPLASH_4", R.drawable.splash_option_4)
    )

    fun setUserName(name: String) {
        viewModelScope.launch {
            secureStorage.setUserName(name)
            _userName.value = name
        }
    }

    fun setAvatarFromPredefined(avatarName: String) {
        viewModelScope.launch {
            secureStorage.setAvatarPath("predefined:$avatarName")
            _avatarPath.value = "predefined:$avatarName"
        }
    }

    fun setAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                // Copy image to internal storage
                val avatarsDir = File(context.filesDir, "avatars")
                if (!avatarsDir.exists()) {
                    avatarsDir.mkdirs()
                }
                
                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                val destFile = File(avatarsDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val path = "custom:${destFile.absolutePath}"
                secureStorage.setAvatarPath(path)
                _avatarPath.value = path
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCelebrationImage(imageName: String) {
        viewModelScope.launch {
            secureStorage.setCelebrationImagePath("predefined:$imageName")
            _celebrationImagePath.value = "predefined:$imageName"
        }
    }

    fun resetAvatar() {
        viewModelScope.launch {
            secureStorage.setAvatarPath(null)
            _avatarPath.value = null
        }
    }

    fun resetCelebrationImage() {
        viewModelScope.launch {
            secureStorage.setCelebrationImagePath(null)
            _celebrationImagePath.value = null
        }
    }

    fun setSplashScreenStyle(style: String) {
        viewModelScope.launch {
            preferenceManager.splashScreenStyle = style
            _splashScreenStyle.value = style
        }
    }
}

