package com.tapflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Application class for TapFlow. Holds the global [AppContainer] and initializes system channels.
 */
class TapFlowApp : Application() {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TapFlow Automations"
            val descriptionText = "Notifications triggered by NFC tag automations"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("tapflow_automations", name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
