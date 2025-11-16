package com.example.finance_code

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Tienes un recordatorio."
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Recordatorio"
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)
        val channelId = intent.getStringExtra("EXTRA_CHANNEL_ID") ?: "pagos_channel_id"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}