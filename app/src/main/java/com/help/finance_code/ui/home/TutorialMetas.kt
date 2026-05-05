package com.help.finance_code.ui.home

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.edit
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.help.finance_code.R
import com.help.finance_code.utils.ThemeUtils

class TutorialMetas(
    private val activity: Activity,
    private val view: View
) {

    companion object {
        private const val MODO_PRUEBAS_TUTORIAL = false
    }
    private val prefs: SharedPreferences = activity.getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)

    fun start() {
        if (MODO_PRUEBAS_TUTORIAL) {
            prefs.edit { putBoolean("tutorial_metas_completo", false) }
        }

        if (prefs.getBoolean("tutorial_metas_completo", false)) return

        // =====================================================================
        // FASE 1: INYECTAR LA TARJETA DE META (CLON PERFECTO)
        // =====================================================================
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        val fakeCardView = LayoutInflater.from(activity).inflate(R.layout.item_meta, rootView, false)

        try {
            fakeCardView.findViewById<TextView>(R.id.tvNombreMeta)?.text = "Viaje a San Andrés"
            fakeCardView.findViewById<TextView>(R.id.tvMontos)?.text = "$500,000 / $2,000,000"
            fakeCardView.findViewById<ProgressBar>(R.id.progressMeta)?.progress = 25

            // Forzar porcentaje visual
            val tvPorcentaje = fakeCardView.findViewById<TextView>(R.id.tvPorcentaje)
            tvPorcentaje?.text = "25%"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(40, 350, 40, 0)
        fakeCardView.layoutParams = layoutParams

        rootView.addView(fakeCardView)

        val fabAddMeta = view.findViewById<View>(R.id.fabAddMeta)

        val colorAura = ThemeUtils.getAuraColor(activity)
        val colorBlanco = Color.WHITE
        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."

        val targets = mutableListOf<TapTarget>()

        // 1. Explicar la tarjeta (CORREGIDO: Arrastrar en lugar de deslizar + Mejor Contraste)
        targets.add(
            TapTarget.forView(
                fakeCardView,
                "Tu Progreso Visual",
                "Aquí verás tus metas. La barra y el porcentaje crecerán automáticamente cuando le abones dinero a este objetivo.\n\nMantén presionada la tarjeta para arrastrarla y organizar tus metas como prefieras.\n\n¿Le adjuntaste una foto? ¡Tócala para expandir la imagen y ver los detalles!$textoSiguiente"
            )
                .outerCircleColorInt(colorAura)
                .targetCircleColorInt(colorBlanco)
                .dimColor(android.R.color.black)
                .titleTextSize(24)
                .titleTextColorInt(colorBlanco)
                .descriptionTextSize(16) // Letra más grande
                .descriptionTextColorInt(colorBlanco) // Blanco puro para máximo contraste
                .textTypeface(Typeface.SANS_SERIF)
                .drawShadow(true)
                .cancelable(false)
                .tintTarget(false)
                .transparentTarget(false)
                .targetRadius(140)
        )

        // 2. Botón de añadir meta
        fabAddMeta?.let {
            targets.add(
                TapTarget.forView(
                    it,
                    "¡Crea tu primera Meta!",
                    "El primer paso es empezar. Toca aquí para ver cómo definir qué quieres lograr y para cuándo lo necesitas.$textoSiguiente"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(24)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(false)
                    .tintTarget(false)
                    .targetRadius(45)
            )
        }

        if (targets.isEmpty()) {
            rootView.removeView(fakeCardView)
            return
        }

        try {
            TapTargetSequence(activity)
                .targets(targets)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        rootView.removeView(fakeCardView)
                        mostrarTutorialNuevaMeta(rootView, colorAura, colorBlanco)
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        rootView.removeView(fakeCardView)
                        prefs.edit { putBoolean("tutorial_metas_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            rootView.removeView(fakeCardView)
            e.printStackTrace()
        }
    }

    // =====================================================================
    // FASE 2: INYECTAR EL DIÁLOGO FANTASMA Y EXPLICAR BOTONES COMPLETOS
    // =====================================================================
    private fun mostrarTutorialNuevaMeta(rootView: ViewGroup, colorAura: Int, colorBlanco: Int) {

        val fakeDialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_meta, rootView, false)

        val dialogParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        dialogParams.setMargins(40, 150, 40, 0)
        fakeDialogView.layoutParams = dialogParams

        fakeDialogView.setBackgroundColor(Color.parseColor("#1E1E1E"))
        fakeDialogView.elevation = 16f

        rootView.addView(fakeDialogView)

        // BUSCAR LOS CAMPOS DENTRO DEL DIÁLOGO
        val etNombre = fakeDialogView.findViewById<View>(R.id.etNombreMeta) ?: fakeDialogView.findViewById<View>(R.id.etNombre)
        val etMonto = fakeDialogView.findViewById<View>(R.id.etMontoObjetivo) ?: fakeDialogView.findViewById<View>(R.id.etMonto)
        val etMontoInicial = fakeDialogView.findViewById<View>(R.id.etMontoActual)
        val btnFecha: View? = null
        val btnImagen = fakeDialogView.findViewById<View>(R.id.btnSeleccionarImagen) ?: fakeDialogView.findViewById<View>(R.id.ivImagenMeta)
        val btnColaboradores = fakeDialogView.findViewById<View>(R.id.etEmailCompartido) ?: fakeDialogView.findViewById<View>(R.id.emailCompartidoLayout)
        val btnGuardar = fakeDialogView.findViewById<View>(R.id.btnSave) ?: fakeDialogView.findViewById<View>(R.id.btnGuardar)

        val targetsDialogo = mutableListOf<TapTarget>()
        val textoSiguiente = "\n\n• Toca el circulo iluminado para continuar."

        etNombre?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "Ponle un nombre a tu sueño",
                    "Escribe algo que te motive cada vez que lo leas. Por ejemplo: 'Viaje a la playa', 'Computador nuevo' o 'Fondo de emergencias'.$textoSiguiente"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco) // Blanco puro
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(false)
                    .transparentTarget(true)
                    .targetRadius(50)
            )
        }

        etMonto?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "¿Cuánto cuesta lograrlo?",
                    "Escribe aquí la cantidad total que necesitas ahorrar. HelpCoin usará este número para calcular tu porcentaje de progreso automáticamente.$textoSiguiente"
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(22)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco) // Blanco puro
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(false)
                    .transparentTarget(true)
                    .targetRadius(50)
            )
        }

        etMontoInicial?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "Abono Inicial (Opcional)",
                    "¿Ya tienes algo ahorrado bajo el colchón o en el banco para esto? Regístralo aquí para que tu barra de progreso no empiece desde cero.$textoSiguiente"
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
                    .cancelable(false)
                    .transparentTarget(true)
                    .targetRadius(50)
            )
        }

        btnFecha?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "Ponte una Fecha Límite",
                    "Ponerle fecha de caducidad a un sueño lo convierte en una meta real. Selecciona para cuándo planeas haber reunido el dinero.$textoSiguiente"
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
                    .cancelable(false)
                    .transparentTarget(true)
                    .targetRadius(50)
            )
        }

        btnImagen?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "Sube una Imagen",
                    "Una imagen vale más que mil palabras. Sube una foto de ese viaje o esa moto que quieres comprar para mantenerte 100% motivado al entrar a la app.$textoSiguiente"
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
                    .cancelable(false)
                    .transparentTarget(true)
                    .targetRadius(45)
            )
        }

        btnColaboradores?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "Ahorra en Equipo",
                    "¿Es una meta compartida? Toca aquí para invitar a tus amigos, pareja o familia. ¡Todos podrán sumar dinero a esta meta y ver cómo crece en equipo!$textoSiguiente"
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
                    .cancelable(false)
                    .transparentTarget(true)
                    .targetRadius(45)
            )
        }

        btnGuardar?.let {
            targetsDialogo.add(
                TapTarget.forView(
                    it,
                    "¡A por ello!",
                    "Cuando llenes los datos, presiona Guardar. Tu meta aparecerá en la lista principal lista para que empieces a abonarle.\n\n• Toca el circulo para finalizar el tutorial."
                )
                    .outerCircleColorInt(colorAura)
                    .targetCircleColorInt(colorBlanco)
                    .dimColor(android.R.color.black)
                    .titleTextSize(24)
                    .titleTextColorInt(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColorInt(colorBlanco)
                    .textTypeface(Typeface.SANS_SERIF)
                    .drawShadow(true)
                    .cancelable(true)
                    .tintTarget(false)
                    .targetRadius(45)
            )
        }

        if (targetsDialogo.isEmpty()) {
            rootView.removeView(fakeDialogView)
            prefs.edit { putBoolean("tutorial_metas_completo", true) }
            return
        }

        try {
            TapTargetSequence(activity)
                .targets(targetsDialogo)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        rootView.removeView(fakeDialogView)
                        prefs.edit { putBoolean("tutorial_metas_completo", true) }
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        rootView.removeView(fakeDialogView)
                        prefs.edit { putBoolean("tutorial_metas_completo", true) }
                    }
                })
                .start()
        } catch (e: Exception) {
            rootView.removeView(fakeDialogView)
            e.printStackTrace()
        }
    }
}