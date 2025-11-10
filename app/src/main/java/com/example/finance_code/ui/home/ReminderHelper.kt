package com.example.finance_code.ui.home

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import com.example.finance_code.NotificationReceiver
import java.util.Calendar

object ReminderHelper {

    const val PAGOS_CHANNEL_ID = "pagos_channel_id"


    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios de Pagos"
            val descriptionText = "Canal para notificaciones de pagos pendientes."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(PAGOS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    fun createGoogleCalendarEvent(
        context: Context,
        titulo: String,
        fechaMillis: Long
    ) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fechaMillis) // Evento de todo el día
            .putExtra(CalendarContract.Events.TITLE, "Pago: $titulo")
            .putExtra(CalendarContract.Events.DESCRIPTION, "Recordatorio de pago de $titulo generado por HC.")
            .putExtra(CalendarContract.Events.ALL_DAY, true)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró una app de calendario.", Toast.LENGTH_SHORT).show()
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


        val dayInMillis = 1000L * 60 * 60 * 24
        val fechaPagoMillis = fechaPago.timeInMillis
        val now = System.currentTimeMillis()


        val triggerCalendar = fechaPago.clone() as Calendar
        triggerCalendar.set(Calendar.HOUR_OF_DAY, 9)
        triggerCalendar.set(Calendar.MINUTE, 0)
        triggerCalendar.set(Calendar.SECOND, 0)
        val triggerTimeMillis = triggerCalendar.timeInMillis

        val tiempos = listOf(
            now + 5000,
            triggerTimeMillis - (7 * dayInMillis),
            triggerTimeMillis - (3 * dayInMillis),
            triggerTimeMillis - dayInMillis,
            triggerTimeMillis
        )


        val tiemposValidos = tiempos.filter { it > now || it == tiempos[0] }


        tiemposValidos.forEach { triggerAtMillis ->


            val requestCode = (triggerAtMillis).toInt()

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_MESSAGE", mensaje)
                putExtra("EXTRA_NOTIFICATION_ID", requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {

                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        Toast.makeText(context, "Recordatorio guardado.", Toast.LENGTH_LONG).show()
    }
}