package com.github.xiawusharve.webrtc

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

fun startForegroundService(context: Context): Intent {
    val intent = Intent(context, ForegroundService::class.java)
    ContextCompat.startForegroundService(context, intent)
    return intent
} // TODO 如何stop？

class ForegroundService: Service() {
    companion object {
        const val TAG = "ForegroundService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        super.onCreate()
        val myNotification = MyNotification(this)
        myNotification.createNotificationChannel(
            "foreground_service", "前台服务",
            NotificationManager.IMPORTANCE_HIGH
        )
        startForeground(1024, myNotification.createNotification("守护进程", "后台服务正在运行"))
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }
}