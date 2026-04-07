package com.help.finance_code

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.help.finance_code.ui.home.ReminderHelper
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTIFICATION_RECEIVED = "com.help.finance_code.NOTIFICATION_RECEIVED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Tienes un recordatorio."
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Recordatorio"
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)
        val channelId = intent.getStringExtra("EXTRA_CHANNEL_ID") ?: "pagos_channel_id"

        val targetFragment = intent.getStringExtra(ReminderHelper.EXTRA_TARGET_FRAGMENT)

        val localIntent = Intent(ACTION_NOTIFICATION_RECEIVED).apply {
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_TITLE", title)
            putExtra(ReminderHelper.EXTRA_TARGET_FRAGMENT, targetFragment)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)

        val launchIntent = Intent(context, com.help.finance_code.ui.home.HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (targetFragment != null) {
                putExtra(ReminderHelper.EXTRA_TARGET_FRAGMENT, targetFragment)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}