package com.help.finance_code.ui.home // Cambia esto si lo pones en otra carpeta

import android.app.Activity
import android.content.SharedPreferences
import android.view.View
import android.widget.Toast
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.help.finance_code.R

object TutorialMovimientos {

    fun mostrar(activity: Activity, view: View, prefs: SharedPreferences) {
        // Cambia a 'false' si quieres probarlo varias veces mientras programas
        val tutorialCompletado = prefs.getBoolean("tutorial_movimientos_completo", false)

        if (tutorialCompletado) return

        // 1. Buscamos las vistas clave en tu fragment_movimientos.xml
        val cardSaldo = view.findViewById<View>(R.id.cardSaldoContainer)
        val btnDiscreto = view.findViewById<View>(R.id.btnDiscreetModeManual)
        val btnFiltrar = view.findViewById<View>(R.id.btnFiltrarFechas)
        val fabAdd = view.findViewById<View>(R.id.fabAddTransaction)

        // Verificamos que las vistas existan para evitar crasheos
        if (cardSaldo == null || btnDiscreto == null || btnFiltrar == null || fabAdd == null) return

        val colorPrimario = R.color.colorPrimary
        val colorBlanco = android.R.color.white

        // 2. Iniciamos la secuencia del tutorial
        TapTargetSequence(activity)
            .targets(
                // --- PASO 1: LA BILLETERA / BOLSILLOS ---
                TapTarget.forView(
                    cardSaldo,
                    "Tu Billetera y Bolsillos",
                    "Aquí puedes ver tu saldo total. ¡Toca esta tarjeta en cualquier momento para filtrar todos tus movimientos por un Bolsillo específico!"
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .titleTextSize(22)
                    .titleTextColor(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(80), // Radio amplio para cubrir la tarjeta

                // --- PASO 2: MODO DISCRETO ---
                TapTarget.forView(
                    btnDiscreto,
                    "Modo Discreto (Privacidad)",
                    "Toca este ojo para ocultar tu saldo y montos si estás en público.\n\n💡 Tip Pro: ¡También puedes activarlo simplemente agitando tu celular!"
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(40),

                // --- PASO 3: FILTROS ---
                TapTarget.forView(
                    btnFiltrar,
                    "Filtros Avanzados",
                    "¿Buscas un gasto antiguo? Usa este botón para filtrar por fechas o agrupar tus movimientos por día, mes, año o bolsillos."
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(50),

                // --- PASO 4: BOTÓN DE ACCIÓN (FAB) ---
                TapTarget.forView(
                    fabAdd,
                    "Registrar e Importar",
                    "Toca el botón + para abrir el menú. Puedes registrar un movimiento manualmente o importar tus Extractos Bancarios en PDF de forma automática."
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(40)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    prefs.edit().putBoolean("tutorial_movimientos_completo", true).apply()
                    Toast.makeText(activity, "¡Tutorial completado!", Toast.LENGTH_SHORT).show()
                }

                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}

                override fun onSequenceCanceled(lastTarget: TapTarget?) {
                    // El usuario tocó afuera para saltarse el tutorial
                    prefs.edit().putBoolean("tutorial_movimientos_completo", true).apply()
                    Toast.makeText(activity, "Tutorial omitido", Toast.LENGTH_SHORT).show()
                }
            })
            .start()
    }
}