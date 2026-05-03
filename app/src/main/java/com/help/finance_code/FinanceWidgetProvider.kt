package com.help.finance_code

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.help.finance_code.data.AppDB
import com.help.finance_code.data.EXTRA_WIDGET_TRANSACTION_TYPE
import com.help.finance_code.data.TYPE_EGRESO
import com.help.finance_code.data.TYPE_INGRESO
import com.help.finance_code.ui.login.AuthCheckActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

class FinanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, FinanceWidgetProvider::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    scope.launch {
        val views = RemoteViews(context.packageName, R.layout.finance_widget_layout)
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        val balanceText = if (userEmail != null) {
            try {
                val database = AppDB.getDatabase(context, userEmail)

                val todosLosMovimientos = database.movimientoDao().obtenerTodosSync()

                val balancePorRama = todosLosMovimientos.groupBy { it.parentId ?: it.id }
                    .mapValues { entry ->
                        entry.value.sumOf { if (it.tipo == 1) it.cantidad else -it.cantidad }
                    }
                val disponibleReal = balancePorRama.values.filter { it > 0 }.sum()

                val format = DecimalFormat("$#,###", DecimalFormatSymbols(Locale("es", "CO")))
                format.format(disponibleReal)
            } catch (e: Exception) {
                "Error"
            }
        } else {
            "Iniciar Sesión"
        }

        views.setTextViewText(R.id.widget_balance, balanceText)

        // Configurar botón de Egreso (-)
        val gastoIntent = Intent(context, AuthCheckActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_TRANSACTION_TYPE, TYPE_EGRESO)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val gastoPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId * 10,
            gastoIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        views.setOnClickPendingIntent(R.id.btn_nuevo_gasto, gastoPendingIntent)

        val ingresoIntent = Intent(context, AuthCheckActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_TRANSACTION_TYPE, TYPE_INGRESO)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val ingresoPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 1,
            ingresoIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        views.setOnClickPendingIntent(R.id.btn_nuevo_ingreso, ingresoPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}