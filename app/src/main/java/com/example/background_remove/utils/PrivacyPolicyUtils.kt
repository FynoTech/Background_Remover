package com.example.background_remove.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.photo.editor.background.eraser.bgeraser.R

object PrivacyPolicyUtils {

    fun isValidHttpOrHttpsUrl(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return false
        val uri = try {
            Uri.parse(s)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        return !uri.host.isNullOrBlank()
    }

    /**
     * Opens the Remote Config privacy URL when enabled and valid.
     * @return true if a browser/view intent was started
     */
    fun openPrivacyPolicyIfPossible(activity: Activity): Boolean {
        if (!RemoteConfigManager.isPrivacyPolicyEnabled()) return false
        val url = RemoteConfigManager.getPrivacyPolicyUrl()
        if (!isValidHttpOrHttpsUrl(url)) {
            Toast.makeText(
                activity,
                activity.getString(R.string.privacy_policy_unavailable),
                Toast.LENGTH_SHORT,
            ).show()
            return false
        }
        return try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())))
            true
        } catch (_: Exception) {
            Toast.makeText(
                activity,
                activity.getString(R.string.privacy_policy_unavailable),
                Toast.LENGTH_SHORT,
            ).show()
            false
        }
    }
}
