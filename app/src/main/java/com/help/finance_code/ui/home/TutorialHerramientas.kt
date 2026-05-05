package com.help.finance_code.ui.home

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.core.content.edit
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.help.finance_code.R
import com.help.finance_code.utils.ThemeUtils

class TutorialHerramientas(
    private val activity: Activity,
    private val view: View
) {

    companion object {
        private const val MODO_PRUEBAS_TUTORIAL = false
    }

    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit { putBoolean("tutorial_herramientas_completo", false) }
        }

        if (prefs.getBoolean("tutorial_herramientas_completo", false)) return

        val btnAhorro = view.findViewById<View>(R.id.tarjeta_calculadora_ahorro)
        val btnPrestamo = view.findViewById<View>(R.id.tarjeta_calculadora_prestamo)
        val btnPagoRapido = view.findViewById<View>(R.id.tarjeta_pago_deuda_rapido)
        val btnPresupuesto = view.findViewById<View>(R.id.tarjeta_calculadora_presupuesto)
        val btnImpuestos = view.findViewById<View>(R.id.tarjeta_calculadora_impuestos)

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE

        // Textos de ayuda
        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para omitir el tutorial."

        val targets = mutableListOf<TapTarget>()

        // 1. Calculadora de Ahorro (Primera opción)
        btnAhorro?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Proyección de Ahorro",
                    "Ideal para saber cuánto dinero debes guardar semanal o mensualmente para alcanzar una meta en el tiempo que deseas. ¡La matemática a tu favor!$textoSiguiente$textoOmitir"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true) // Ahora se puede cancelar tocando afuera
                    .transparentTarget(true)
                    .targetRadius(60)
            )
        }

        // 2. Calculadora de Préstamo (Segunda opción)
        btnPrestamo?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Calculadora de Préstamos",
                    "¿Pensando en pedir un crédito? Descubre exactamente cuánto terminarás pagando en intereses y planea tus cuotas sin sorpresas desagradables con el banco.$textoSiguiente$textoOmitir"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true) // Ahora se puede cancelar tocando afuera
                    .transparentTarget(true)
                    .targetRadius(60)
            )
        }

        // 3. Pago Rápido de Deuda (Tercera opción)
        btnPagoRapido?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Pago Rápido de Deudas",
                    "Averigua cuál es la mejor estrategia para liquidar tus deudas actuales en tiempo récord y ahorrando dinero en intereses.$textoSiguiente$textoOmitir"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true) // Ahora se puede cancelar tocando afuera
                    .transparentTarget(true)
                    .targetRadius(60)
            )
        }

        // 4. Calculadora de Presupuesto (Cuarta opción)
        btnPresupuesto?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Regla 50/30/20",
                    "¿No sabes cómo dividir tu sueldo? Esta herramienta reparte tus ingresos automáticamente en: Gastos Básicos, Gastos Personales y Ahorro/Inversión.$textoSiguiente$textoOmitir"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true) // Ahora se puede cancelar tocando afuera
                    .transparentTarget(true)
                    .targetRadius(60)
            )
        }

        // 5. Calculadora de Impuestos (Quinta y última opción)
        btnImpuestos?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Cálculo de Impuestos",
                    "Evita dolores de cabeza al hacer cuentas. Calcula rápidamente cuánto dinero se va en IVA u otros impuestos sobre tus ingresos o facturas.\n\n• Toca el circulo para finalizar.$textoOmitir"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(60)
            )
        }

        if (targets.isEmpty()) return

        try {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        prefs.edit { putBoolean("tutorial_herramientas_completo", true) }
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        // Se guarda como completado si el usuario decide omitirlo tocando afuera
                        prefs.edit { putBoolean("tutorial_herramientas_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}