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

class TutorialPersonalizacion(
    private val activity: Activity,
    private val view: View
) {

    companion object {
        private const val MODO_PRUEBAS_TUTORIAL = false
    }

    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit { putBoolean("tutorial_personalizacion_completo", false) }
        }

        if (prefs.getBoolean("tutorial_personalizacion_completo", false)) return

        // BUSCAR LOS CAMPOS EN TU XML
        val rvColores = view.findViewById<View>(R.id.cardColorContainer)
        // NOTA: Ajusta el ID del switch AMOLED por el que tengas en tu fragment_customization.xml
        val switchAmoled = view.findViewById<View>(R.id.switchAmoled) ?: view.findViewById<View>(R.id.cardAmoledContainer)
        val btnAplicarTema = view.findViewById<View>(R.id.btnSaveCustomization)

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE
        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para omitir el tutorial."

        val targets = mutableListOf<TapTarget>()

        // 1. Color Aura (Corregido visualmente)
        rvColores?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Tu Color Aura",
                    "Elige el color que más te identifique. Toda la aplicación (botones, menús y tutoriales) se adaptará a este color para darte una experiencia única.$textoSiguiente$textoOmitir"
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
                    .tintTarget(false) // Mantiene la vista real intacta
                    .transparentTarget(false) // Soluciona el bug de la letra escondida por detrás
                    .targetRadius(100) // Un radio más grande para abrazar bien el contenedor
            )
        }

        // 2. Modo AMOLED Oscuro Profundo
        switchAmoled?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Modo AMOLED Oscuro",
                    "¿Tu teléfono tiene pantalla OLED? Activa esta opción para disfrutar de fondos negros puros. ¡Tus ojos descansarán y ahorrarás mucha batería!$textoSiguiente$textoOmitir"
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
                    .targetRadius(55)
            )
        }

        // 3. Botón de Aplicar Cambios
        btnAplicarTema?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "Aplicar Cambios",
                    "Una vez encuentres tu estilo perfecto, presiona aquí para guardar los cambios y ver la magia en toda la app.\n\n• Toca el circulo para finalizar.$textoOmitir"
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
                    .targetRadius(45)
            )
        }

        if (targets.isEmpty()) return

        try {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        prefs.edit { putBoolean("tutorial_personalizacion_completo", true) }
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        prefs.edit { putBoolean("tutorial_personalizacion_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}