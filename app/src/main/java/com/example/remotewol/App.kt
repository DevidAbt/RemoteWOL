package com.example.remotewol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build

class App : Application(){
     private val notificationChannelId = "1"

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MyNotificationChannel";
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(notificationChannelId, name, importance)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        startService(Intent(this, WolService::class.java))
    }
}