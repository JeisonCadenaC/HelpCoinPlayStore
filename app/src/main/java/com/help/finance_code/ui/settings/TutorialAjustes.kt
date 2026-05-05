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

class TutorialAjustes(
    private val activity: Activity,
    private val view: View
) {

    companion object {
        private const val MODO_PRUEBAS_TUTORIAL = false
    }

    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit { putBoolean("tutorial_ajustes_completo", false) }
        }

        if (prefs.getBoolean("tutorial_ajustes_completo", false)) return
        val btnPersonalizacion = view.findViewById<View>(R.id.cardMenuCustomization)
        val btnRespaldos = view.findViewById<View>(R.id.cardMenuBackup)
        val btnPrivacidad = view.findViewById<View>(R.id.cardMenuPrivacy)
        val btnCerrarSesion = view.findViewById<View>(R.id.btnLogout)

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE

        // Textos de ayuda
        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para omitir el tutorial."

        val targets = mutableListOf<TapTarget>()

        // 1. Personalización
        btnPersonalizacion?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Dale tu toque personal",
                    "Aquí puedes cambiar la apariencia de HelpCoin. Ajusta el tema visual, los colores y haz que la aplicación se adapte totalmente a tu estilo.$textoSiguiente$textoOmitir"
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
                    .cancelable(true) // Se puede omitir
                    .transparentTarget(true)
                    .targetRadius(60)
            )
        }

        // 2. Copias de Seguridad (Backup)
        btnRespaldos?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Protege tu información",
                    "¡Que no se pierdan tus datos! En esta sección puedes guardar tus movimientos y metas en la nube, y restaurarlos si cambias de celular.$textoSiguiente$textoOmitir"
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

        // 3. Privacidad y Seguridad
        btnPrivacidad?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Tu seguridad es primero",
                    "Ajusta tus opciones de privacidad, configura el bloqueo por huella y administra cómo funciona el Modo Discreto para proteger tus finanzas de miradas curiosas.$textoSiguiente$textoOmitir"
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

        // 4. Cerrar Sesión
        btnCerrarSesion?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Cerrar Sesión",
                    "Si compartes este dispositivo o simplemente quieres salir de tu cuenta, usa este botón para cerrar sesión de forma segura.\n\n• Toca el circulo para finalizar.$textoOmitir"
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
                    .targetRadius(50)
            )
        }

        if (targets.isEmpty()) return

        try {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        prefs.edit { putBoolean("tutorial_ajustes_completo", true) }
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        prefs.edit { putBoolean("tutorial_ajustes_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}