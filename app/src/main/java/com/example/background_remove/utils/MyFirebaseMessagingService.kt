package com.example.background_remove.utils

import android.app.LauncherActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.background_remove.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage



class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.notification != null) {
            showNotification(
                remoteMessage.notification!!.title,
                remoteMessage.notification!!.body
            )
        }
    }

    private fun showNotification(
        title: String?,
        message: String?
    ) {
        try {
            val intent = Intent(this, LauncherActivity::class.java)
            val channel_id = "notification_channel"
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(applicationContext, channel_id)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .setVibrate(
                        longArrayOf(
                            1000, 1000, 1000,
                            1000, 1000
                        )
                    )
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

            if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O
            ) {
                val notificationChannel = NotificationChannel(
                    channel_id, "web_app",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(
                    notificationChannel
                )
            }

            notificationManager.notify(0, builder.build())

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}