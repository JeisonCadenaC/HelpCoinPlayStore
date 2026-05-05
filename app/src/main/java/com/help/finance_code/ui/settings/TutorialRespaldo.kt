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

class TutorialRespaldo(
    private val activity: Activity,
    private val view: View
) {

    companion object {
        private const val MODO_PRUEBAS_TUTORIAL = false
    }

    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit { putBoolean("tutorial_respaldo_completo", false) }
        }

        if (prefs.getBoolean("tutorial_respaldo_completo", false)) return
        
        val cardCrearBackup = view.findViewById<View>(R.id.cardRealizarBackup)
        val cardRestaurarBackup = view.findViewById<View>(R.id.cardRestaurarBackup)
        val cardAutoBackup = view.findViewById<View>(R.id.layoutFrecuencia)

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE

        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para omitir el tutorial."

        val targets = mutableListOf<TapTarget>()

        // 1. Contenedor de Crear Copia de Seguridad (Cuenta + Botón)
        cardCrearBackup?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Respaldo en Google Drive",
                    "¡Tus datos valen oro! Aquí podrás ver tu cuenta conectada y subir de forma segura todo tu historial de movimientos y metas a la nube.$textoSiguiente$textoOmitir"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco) // Blanco puro para alto contraste
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true)
                    .tintTarget(false) // Mantiene los colores originales de tu tarjeta
                    .transparentTarget(false) // Evita que la letra quede por detrás de la tarjeta
                    .targetRadius(120) // Radio bien grande para abrazar todo el contenedor
            )
        }

        // 2. Contenedor de Restaurar Datos
        cardRestaurarBackup?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Restaurar Datos",
                    "¿Cambiaste de celular o reinstalaste la app? Usa esta opción para buscar tu última copia en Google Drive y recuperar todo tal como lo dejaste.$textoSiguiente$textoOmitir"
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
                    .tintTarget(false)
                    .transparentTarget(false)
                    .targetRadius(100)
            )
        }

        // 3. Contenedor de Respaldo Automático
        cardAutoBackup?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Respaldo Automático",
                    "¡Despreocúpate! Al activar esta opción, HelpCoin se encargará de hacer las copias de seguridad por ti periódicamente sin que tengas que mover un dedo.\n\n• Toca el circulo para finalizar.$textoOmitir"
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
                    .tintTarget(false)
                    .transparentTarget(false)
                    .targetRadius(80)
            )
        }

        if (targets.isEmpty()) return

        try {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        prefs.edit { putBoolean("tutorial_respaldo_completo", true) }
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        prefs.edit { putBoolean("tutorial_respaldo_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}