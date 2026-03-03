package com.platisa.app.core.common.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages Google Play In-App Updates using the Flexible flow.
 */
class InAppUpdateManager(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    // Tracks if an update is fully downloaded and ready to be installed
    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val installStateUpdatedListener = object : InstallStateUpdatedListener {
        override fun onStateUpdate(state: com.google.android.play.core.install.InstallState) {
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()
                    Timber.d("InAppUpdate: Downloading - $bytesDownloaded / $totalBytesToDownload")
                }
                InstallStatus.DOWNLOADED -> {
                    Timber.d("InAppUpdate: Downloaded successfully")
                    _isUpdateDownloaded.value = true
                }
                InstallStatus.FAILED -> {
                    Timber.e("InAppUpdate: Download failed")
                }
                InstallStatus.CANCELED -> {
                    Timber.d("InAppUpdate: Download canceled")
                }
                InstallStatus.INSTALLED -> {
                    Timber.d("InAppUpdate: Installed")
                    appUpdateManager.unregisterListener(this)
                }
                else -> {}
            }
        }
    }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    /**
     * Checks for an update and starts the flexible flow if available.
     * Should be called in onCreate or similar initialization point.
     */
    fun checkForUpdate(activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activityResultLauncher,
                        options
                    )
                } catch (e: Exception) {
                    Timber.e(e, "InAppUpdate: Failed to start update flow")
                }
            } else {
                Timber.d("InAppUpdate: No update available or flexible not allowed")
            }
        }.addOnFailureListener { e ->
            Timber.w(e, "InAppUpdate: Failed to check for update (likely not installed from Play Store)")
        }
    }

    /**
     * Checks if an update was already downloaded but hasn't been installed yet.
     * Should be called in onResume to catch edge cases where app was backgrounded during download.
     */
    fun resumeUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Timber.d("InAppUpdate: Resuming and found downloaded update")
                _isUpdateDownloaded.value = true
            }
        }
    }

    /**
     * Completes the update process (triggers an app restart to install the update).
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun onDestroy() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
