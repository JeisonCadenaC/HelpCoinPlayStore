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

class TutorialMovimientos(
    private val activity: Activity,
    private val view: View,
    private val isFabOpen: () -> Boolean,
    private val toggleFabMenu: () -> Unit
) {

    // =========================================================================================
    // LÓGICA DEL TUTORIAL INTERACTIVO EN DOS PARTES
    // =========================================================================================

    // INTERRUPTOR DE PRUEBAS DEL TUTORIAL
    // Cambiar a 'false' al publicar la aplicacion para que solo aparezca la primera vez
    private val MODO_PRUEBAS_TUTORIAL = false
    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit().putBoolean("tutorial_movimientos_completo", false).apply()
        }

        val tutorialCompletado = prefs.getBoolean("tutorial_movimientos_completo", false)
        if (tutorialCompletado) return

        mostrarTutorialMovimientosParte1()
    }

    private fun mostrarTutorialMovimientosParte1() {
        val tvSaldo = view.findViewById<View>(R.id.tvSaldoTotal)
        val btnEdit = view.findViewById<View>(R.id.btnEditProfile)
        val btnHideBalance = view.findViewById<View>(R.id.btnHideBalance)
        val btnDiscreto = view.findViewById<View>(R.id.btnDiscreetModeManual)
        val fabAdd = view.findViewById<View>(R.id.fabAddTransaction)

        if (tvSaldo == null || btnEdit == null || btnHideBalance == null || btnDiscreto == null || fabAdd == null) return

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE

        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para salir del tutorial."

        try {
            TapTargetSequence(activity)
                .targets(
                    TapTarget.forView(
                        tvSaldo,
                        "Saldo Disponible",
                        "Este es tu dinero actual. Puedes tocar esta tarjeta en cualquier momento para filtrar tus movimientos por un bolsillo especifico.$textoSiguiente$textoOmitir"
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
                        .targetRadius(50),

                    TapTarget.forView(
                        btnEdit,
                        "Editar Perfil",
                        "Usa este boton para modificar tu nombre, foto de perfil y acceder a los ajustes de tu cuenta.$textoSiguiente$textoOmitir"
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
                        .targetRadius(40),

                    TapTarget.forView(
                        btnHideBalance,
                        "Ocultar Saldo",
                        "Si solo quieres censurar el monto total de la parte superior, toca este icono.$textoSiguiente$textoOmitir"
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
                        .targetRadius(40),

                    TapTarget.forView(
                        btnDiscreto,
                        "Modo Discreto Total",
                        "Activa este modo para censurar todos los valores de la aplicacion y proteger tu privacidad. Tambien se activa agitando el celular.$textoSiguiente$textoOmitir"
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
                        .targetRadius(40),

                    TapTarget.forView(
                        fabAdd,
                        "Menu de Acciones",
                        "Finalmente, este es el boton principal para registrar dinero. Vamos a abrirlo para ver sus opciones.$textoSiguiente$textoOmitir"
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
                        .targetRadius(45)
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        // 1. Abrimos el menú programáticamente
                        if (!isFabOpen()) toggleFabMenu()

                        // 2. Esperamos a que la animación termine y lanzamos la Parte 2
                        view.postDelayed({
                            mostrarTutorialMovimientosParte2()
                        }, 400)
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        prefs.edit().putBoolean("tutorial_movimientos_completo", true).apply()
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mostrarTutorialMovimientosParte2() {
        val fabManual = view.findViewById<View>(R.id.fabAddManual)
        val fabImportPDF = view.findViewById<View>(R.id.fabImportPDF)

        if (fabManual == null || fabImportPDF == null) return

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE

        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."
        val textoOmitir = "\n• Toca la zona oscura para salir del tutorial."

        try {
            TapTargetSequence(activity)
                .targets(
                    TapTarget.forView(
                        fabManual,
                        "Registro Manual",
                        "Usa esta opcion para añadir un nuevo gasto o ingreso escribiendo los datos paso a paso.$textoSiguiente$textoOmitir"
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
                        .targetRadius(60),

                    TapTarget.forView(
                        fabImportPDF,
                        "Importar Extracto PDF",
                        "Sube el extracto mensual de tu banco en PDF y HelpCoin extraera y organizara todos tus movimientos automaticamente.\n\n• Toca el circulo para finalizar."
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
                        .targetRadius(60)
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        if (isFabOpen()) toggleFabMenu() // Cerramos el menú
                        prefs.edit().putBoolean("tutorial_movimientos_completo", true).apply()
                        if (MODO_PRUEBAS_TUTORIAL) Toast.makeText(activity, "Tutorial completo (Modo Pruebas)", Toast.LENGTH_SHORT).show()
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        if (isFabOpen()) toggleFabMenu() // Cerramos el menú
                        prefs.edit().putBoolean("tutorial_movimientos_completo", true).apply()
                    }
                })
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}