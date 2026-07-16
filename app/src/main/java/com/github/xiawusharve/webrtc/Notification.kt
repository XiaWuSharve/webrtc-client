package com.github.xiawusharve.webrtc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.xiawusharve.webrtc.backend.RtcClient

class Notification(private val context: Context) {
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var channelId: String
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel(channelId: String, channelName: String, importance: Int): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId, channelName, importance)
            )
            this.channelId = channelId
            return channelId
        } else {
            return null
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun setContentIntent(title: String) {
        val intent = Intent(context, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, RtcClient::class.java)
        snoozeIntent.setAction("answer");
        snoozeIntent.putExtra("answer", 0);
        val snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notificationCompatBuilder = NotificationCompat.Builder(context, channelId)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText("收到一条消息")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // TODO 传入builder自己构造按钮
            .addAction(R.mipmap.ic_launcher, "接通", snoozePendingIntent)
            .setAutoCancel(true)
        this.notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(100, notificationCompatBuilder.build());
    }
}