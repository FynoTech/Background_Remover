package com.example.background_remove.adsManager


import android.app.Activity
import android.content.IntentSender
import com.example.background_remove.adsManager.enums.UpdateType
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

object InAppUpdate {
    fun Activity.updateApp(
        updateType: UpdateType,
        onCancel: ((onCancel: Boolean) -> Unit)? = null
    ) {
        val type =
            if (updateType == UpdateType.Flexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        val installStateUpdatedListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showCompleteUpdate()
            }
            if (state.installStatus() == InstallStatus.CANCELED) {
                onCancel?.invoke(true)
            }
        }

        appUpdateManager.appUpdateInfo.addOnSuccessListener { result ->
            if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                try {
                    appUpdateManager.startUpdateFlowForResult(result, type, this, 100)
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
            appUpdateManager.registerListener(installStateUpdatedListener)
        }
    }


    private fun Activity.showCompleteUpdate() {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content),
            "New update is ready!",
            Snackbar.LENGTH_INDEFINITE
        )
        snackBar.setAction("Install") {
            val appUpdateManager = AppUpdateManagerFactory.create(this)
            appUpdateManager.completeUpdate()
        }
        snackBar.show()
    }
}
