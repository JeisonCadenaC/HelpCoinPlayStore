package com.help.finance_code.ui.settings

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

class TutorialPrivacidad(
    private val activity: Activity,
    private val view: View
) {

    companion object {
        private const val MODO_PRUEBAS_TUTORIAL = false
    }

    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit { putBoolean("tutorial_privacidad_completo", false) }
        }

        if (prefs.getBoolean("tutorial_privacidad_completo", false)) return

        // =====================================================================
        // BUSCAR LOS CAMPOS (¡Ajusta estos IDs a los tuyos del XML!)
        // =====================================================================
        val switchBiometrico = view.findViewById<View>(R.id.switchBiometric) ?: view.findViewById<View>(R.id.cardBiometric)
        val switchCapturas = view.findViewById<View>(R.id.switchSecureScreen) ?: view.findViewById<View>(R.id.cardSecureScreen)
        val switchDiscretoInicial = view.findViewById<View>(R.id.switchHideBalances) ?: view.findViewById<View>(R.id.cardHideBalances)
        val switchAgitar = view.findViewById<View>(R.id.switchShake) ?: view.findViewById<View>(R.id.cardShake)

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE
        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para omitir el tutorial."

        val targets = mutableListOf<TapTarget>()

        // 1. Bloqueo Biométrico
        switchBiometrico?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Bloqueo Biométrico",
                    "Activa esta opción para exigir tu huella dactilar o reconocimiento facial cada vez que se abra la app. Máxima seguridad para tus finanzas.$textoSiguiente$textoOmitir"
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

        // 2. Bloquear Capturas
        switchCapturas?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Bloquear Capturas",
                    "Mantén tu información blindada. Al activar esto, evitarás que otras apps o personas puedan tomar capturas de pantalla o grabar la app mientras la usas.$textoSiguiente$textoOmitir"
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

        // 3. Modo Discreto Inicial
        switchDiscretoInicial?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Modo Discreto Inicial",
                    "¿Aves abres la app en público? Con esta opción activada, tus saldos y movimientos siempre iniciarán ocultos (con asteriscos) por defecto.$textoSiguiente$textoOmitir"
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

        // 4. Agitar para Ocultar
        switchAgitar?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Agitar para Ocultar",
                    "Un atajo rápido de privacidad: agita tu teléfono en cualquier momento para activar o desactivar el Modo Discreto al instante.\n\n• Toca el circulo para finalizar.$textoOmitir"
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
                        prefs.edit { putBoolean("tutorial_privacidad_completo", true) }
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        // Se guarda como completado si el usuario decide omitirlo
                        prefs.edit { putBoolean("tutorial_privacidad_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}