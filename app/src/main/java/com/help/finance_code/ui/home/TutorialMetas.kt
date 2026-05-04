package com.help.finance_code.ui.home

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.widget.Toast
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.help.finance_code.R
import com.help.finance_code.utils.ThemeUtils

object TutorialMetas {

    // INTERRUPTOR DE PRUEBAS: Cambia a 'false' cuando subas a la Play Store
    private const val MODO_PRUEBAS_TUTORIAL = true

    fun mostrar(activity: Activity, view: View, prefs: SharedPreferences) {

        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit().putBoolean("tutorial_metas_completo", false).apply()
        }

        val tutorialCompletado = prefs.getBoolean("tutorial_metas_completo", false)
        if (tutorialCompletado) return

        // Ubicamos las vistas clave del fragment_metas.xml
        val tvTitulo = view.findViewById<View>(R.id.tvTituloMetas)
        val btnDiscreto = view.findViewById<View>(R.id.btnDiscreetModeManual)
        val fabAdd = view.findViewById<View>(R.id.fabAddMeta)

        if (tvTitulo == null || btnDiscreto == null || fabAdd == null) return

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE

        val textoSiguiente = "\n\n• Toca ESTE RECUADRO iluminado para continuar."
        val textoSiguienteCirculo = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca el fondo oscuro para salir del tutorial."

        try {
            TapTargetSequence(activity)
                .targets(
                    // PASO 1: TÍTULO Y LISTA (Explicando el Drag & Drop)
                    TapTarget.forView(
                        tvTitulo,
                        "Tus Metas de Ahorro",
                        "Aqui podras visualizar el progreso de tus ahorros y metas compartidas.\n\nTip: Puedes mantener presionada una tarjeta para arrastrarla y ordenar tus metas a tu gusto.$textoSiguiente$textoOmitir"
                    )
                        .outerCircleColorInt(colorAura)
                        .targetCircleColorInt(Color.TRANSPARENT) // Círculo invisible para recuadros anchos
                        .dimColor(android.R.color.black)
                        .titleTextSize(22)
                        .titleTextColorInt(colorBlanco)
                        .descriptionTextSize(15)
                        .descriptionTextColorInt(colorBlanco)
                        .cancelable(true)
                        .transparentTarget(true)
                        .tintTarget(false)
                        .targetRadius(150), // Radio grande para cubrir el título y parte de la lista

                    // PASO 2: MODO DISCRETO
                    TapTarget.forView(
                        btnDiscreto,
                        "Privacidad de Ahorros",
                        "Oculta el dinero de tus metas rapidamente tocando este icono o agitando tu dispositivo.$textoSiguienteCirculo$textoOmitir"
                    )
                        .outerCircleColorInt(colorAura)
                        .targetCircleColorInt(colorBlanco) // Círculo normal
                        .dimColor(android.R.color.black)
                        .titleTextSize(22)
                        .titleTextColorInt(colorBlanco)
                        .descriptionTextSize(15)
                        .descriptionTextColorInt(colorBlanco)
                        .cancelable(true)
                        .transparentTarget(true)
                        .tintTarget(false)
                        .targetRadius(40),

                    // PASO 3: FAB AÑADIR META
                    TapTarget.forView(
                        fabAdd,
                        "Crear Nueva Meta",
                        "Toca este boton para crear una meta. Podras asignarle una imagen, un monto objetivo e invitar a otras personas usando su correo electronico para ahorrar juntos.\n\n• Toca el circulo para finalizar."
                    )
                        .outerCircleColorInt(colorAura)
                        .targetCircleColorInt(colorBlanco)
                        .dimColor(android.R.color.black)
                        .titleTextSize(22)
                        .titleTextColorInt(colorBlanco)
                        .descriptionTextSize(15)
                        .descriptionTextColorInt(colorBlanco)
                        .cancelable(true)
                        .transparentTarget(true)
                        .tintTarget(false)
                        .targetRadius(45)
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        prefs.edit().putBoolean("tutorial_metas_completo", true).apply()
                        if (MODO_PRUEBAS_TUTORIAL) Toast.makeText(activity, "Tutorial completado (Modo Pruebas)", Toast.LENGTH_SHORT).show()
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        prefs.edit().putBoolean("tutorial_metas_completo", true).apply()
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}