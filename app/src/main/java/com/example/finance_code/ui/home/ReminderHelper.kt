package com.example.finance_code.ui.home

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.finance_code.NotificationReceiver
import com.example.finance_code.R
import java.util.Calendar
import java.util.TimeZone

object ReminderHelper {

    const val PAGOS_CHANNEL_ID = "pagos_channel_id"
    const val METAS_CHANNEL_ID = "metas_channel_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val namePagos = "Recordatorios de Pagos"
            val descriptionTextPagos = "Canal para notificaciones de pagos pendientes."
            val importancePagos = NotificationManager.IMPORTANCE_HIGH
            val channelPagos = NotificationChannel(PAGOS_CHANNEL_ID, namePagos, importancePagos).apply {
                description = descriptionTextPagos
            }

            val nameMetas = "Recordatorios de Metas"
            val descriptionTextMetas = "Canal para notificaciones de metas de ahorro."
            val importanceMetas = NotificationManager.IMPORTANCE_HIGH
            val channelMetas = NotificationChannel(METAS_CHANNEL_ID, nameMetas, importanceMetas).apply {
                description = descriptionTextMetas
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channelPagos)
            notificationManager.createNotificationChannel(channelMetas)
        }
    }

    fun scheduleNotifications(
        context: Context,
        nombreResponsabilidad: String,
        fechaPago: Calendar,
        fechaFormateada: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val mensaje = "El $fechaFormateada debes pagar '$nombreResponsabilidad'"
        val titulo = "Recordatorio de Pago"

        val dayInMillis = 1000L * 60 * 60 * 24
        val now = System.currentTimeMillis()

        val triggerCalendar = fechaPago.clone() as Calendar

        triggerCalendar.set(Calendar.HOUR_OF_DAY, 9)
        triggerCalendar.set(Calendar.MINUTE, 0)
        triggerCalendar.set(Calendar.SECOND, 0)
        var triggerTimeMillis = triggerCalendar.timeInMillis

        val today = Calendar.getInstance()
        if (triggerTimeMillis <= now &&
            fechaPago.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            fechaPago.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {

            triggerTimeMillis = now + 60000L
        }


        val tiempos = listOf(
            triggerTimeMillis - (7 * dayInMillis),
            triggerTimeMillis - (3 * dayInMillis),
            triggerTimeMillis - dayInMillis,
            triggerTimeMillis
        )

        val tiemposValidos = tiempos.filter { it > now }

        var alarmaProgramada = false

        tiemposValidos.forEach { triggerAtMillis ->
            val requestCode = (triggerAtMillis).toInt()

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_MESSAGE", mensaje)
                putExtra("EXTRA_TITLE", titulo)
                putExtra("EXTRA_CHANNEL_ID", PAGOS_CHANNEL_ID)
                putExtra("EXTRA_NOTIFICATION_ID", requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                alarmaProgramada = true
            } catch (se: SecurityException) {
                Log.e("ReminderHelper", "Fallo de seguridad al programar alarma", se)
            }
        }

        if (alarmaProgramada) {
            Toast.makeText(context, "Recordatorio guardado.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "No se programaron recordatorios (posiblemente la fecha ya pasó).", Toast.LENGTH_SHORT).show()
        }
    }

    fun scheduleWeeklyMetaNotification(
        context: Context,
        nombreMeta: String,
        fechaCreacionMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val mensaje = "¡Ánimo! Sigue trabajando en tu meta: '$nombreMeta'"
        val titulo = "Recordatorio de Meta Semanal"

        val triggerCalendar = Calendar.getInstance()
        triggerCalendar.timeInMillis = fechaCreacionMillis
        triggerCalendar.add(Calendar.MINUTE, 1)

        val triggerAtMillis = triggerCalendar.timeInMillis
        val intervalMillis = AlarmManager.INTERVAL_DAY * 7

        val requestCode = (fechaCreacionMillis).toInt()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", mensaje)
            putExtra("EXTRA_TITLE", titulo)
            putExtra("EXTRA_CHANNEL_ID", METAS_CHANNEL_ID)
            putExtra("EXTRA_NOTIFICATION_ID", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
        } catch (se: SecurityException) {
            Log.e("ReminderHelper", "Fallo de seguridad al programar alarma de meta", se)
            Toast.makeText(context, "No se pudo guardar el recordatorio de meta.", Toast.LENGTH_SHORT).show()
        }
    }

    fun showInstantMetaNotification(context: Context, nombreMeta: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = "¡Meta Creada!"
        val message = "Te recordaremos semanalmente sobre tu meta: '$nombreMeta'."
        val notificationId = (System.currentTimeMillis() % 10000).toInt()

        val notification = NotificationCompat.Builder(context, METAS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metas)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelWeeklyMetaNotification(
        context: Context,
        nombreMeta: String,
        fechaCreacionMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val mensaje = "¡Ánimo! Sigue trabajando en tu meta: '$nombreMeta'"
        val titulo = "Recordatorio de Meta Semanal"
        val requestCode = (fechaCreacionMillis).toInt()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", mensaje)
            putExtra("EXTRA_TITLE", titulo)
            putExtra("EXTRA_CHANNEL_ID", METAS_CHANNEL_ID)
            putExtra("EXTRA_NOTIFICATION_ID", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            Log.d("ReminderHelper", "Alarma cancelada para la meta: $nombreMeta")
        } catch (se: SecurityException) {
            Log.e("ReminderHelper", "Fallo de seguridad al cancelar alarma de meta", se)
        }
    }

    fun createGoogleCalendarEvent(
        context: Context,
        nombre: String,
        fechaMillis: Long
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Recordatorio de Pago: $nombre")
            putExtra(CalendarContract.Events.DESCRIPTION, "No olvides realizar este pago.")

            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fechaMillis + 1000 * 60 * 60 * 24)

            putExtra(
                CalendarContract.Events.EVENT_TIMEZONE,
                TimeZone.getDefault().id
            )
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "ERROR: No se encontró una aplicación de calendario para añadir el evento. Asegúrate de tener una aplicación de calendario instalada.", Toast.LENGTH_LONG).show()
        }
    }

    fun showMetaCollaborationNotification(context: Context, nombreMeta: String, collaboratorEmail: String, action: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val collaboratorName = collaboratorEmail.substringBefore('@').replaceFirstChar { it.uppercase() }

        val title = "Actualización de Meta: $nombreMeta"
        val message = "$collaboratorName ha $action la invitación a la meta '$nombreMeta'."
        val notificationId = (System.currentTimeMillis() % 10000).toInt() + 10000

        val notification = NotificationCompat.Builder(context, METAS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metas)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showAporteNotification(context: Context, nombreMeta: String, collaboratorEmail: String, montoAbsoluto: String, tipoOperacion: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val collaboratorName = collaboratorEmail.substringBefore('@').replaceFirstChar { it.uppercase() }
        val accion = if (tipoOperacion == "APORTE") "agregó" else "retiró"

        val title = "Movimiento en '$nombreMeta'"
        val message = "$collaboratorName $accion $montoAbsoluto de la meta."
        val notificationId = (System.currentTimeMillis() % 10000).toInt() + 20000

        val notification = NotificationCompat.Builder(context, METAS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metas)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showInvitationReceivedNotification(context: Context, nombreMeta: String, inviterEmail: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val inviterName = inviterEmail.substringBefore('@').replaceFirstChar { it.uppercase() }

        val title = "¡Nueva Invitación a Meta!"
        val message = "$inviterName te ha invitado a colaborar en la meta '$nombreMeta'."
        val notificationId = (System.currentTimeMillis() % 10000).toInt() + 40000

        val notification = NotificationCompat.Builder(context, METAS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metas)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}