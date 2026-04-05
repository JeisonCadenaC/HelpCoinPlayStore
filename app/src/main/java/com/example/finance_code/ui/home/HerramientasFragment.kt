package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finance_code.R
import com.example.finance_code.databinding.FragmentHerramientasBinding
import com.example.finance_code.utils.ThemeUtils
import com.google.android.material.button.MaterialButton

class HerramientasFragment : Fragment() {

    private var _binding: FragmentHerramientasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHerramientasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navegación de herramientas
        binding.tarjetaCalculadoraAhorro.setOnClickListener {
            findNavController().navigate(R.id.action_herramientasFragment_to_calculadoraAhorroFragment)
        }

        binding.tarjetaCalculadoraPrestamo.setOnClickListener {
            findNavController().navigate(R.id.action_herramientasFragment_to_calculadoraPrestamoFragment)
        }

        binding.tarjetaPagoDeudaRapido.setOnClickListener {
            findNavController().navigate(R.id.action_herramientasFragment_to_pagoRapidoPrestamo)
        }

        binding.tarjetaCalculadoraPresupuesto.setOnClickListener {
            findNavController().navigate(R.id.action_herramientasFragment_to_calculadoraPresupuestoFragment)
        }

        binding.tarjetaCalculadoraImpuestos.setOnClickListener {
            findNavController().navigate(R.id.action_herramientasFragment_to_calculadoraImpuestosFragment)
        }

        // Botones de información (Disparan el nuevo menú flotante MD3)
        binding.btnInfoAhorro.setOnClickListener {
            mostrarInformacionFlotante(getString(R.string.info_ahorro_titulo), getString(R.string.info_ahorro_mensaje))
        }

        binding.btnInfoPrestamo.setOnClickListener {
            mostrarInformacionFlotante(getString(R.string.info_prestamo_titulo), getString(R.string.info_prestamo_mensaje))
        }

        binding.btnInfoAmortizacion.setOnClickListener {
            mostrarInformacionFlotante(getString(R.string.info_amortizacion_titulo), getString(R.string.info_amortizacion_mensaje))
        }

        binding.btnInfoPresupuesto.setOnClickListener {
            mostrarInformacionFlotante(getString(R.string.info_presupuesto_titulo), getString(R.string.info_presupuesto_mensaje))
        }

        binding.btnInfoImpuestos.setOnClickListener {
            mostrarInformacionFlotante(getString(R.string.info_impuestos_titulo), getString(R.string.info_impuestos_mensaje))
        }
    }

    private fun mostrarInformacionFlotante(titulo: String, mensaje: String) {
        // Inflamos nuestro diseño de tarjeta flotante
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_info_flotante, null)

        // Construimos el AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // 🛑 CRUCIAL: Hacemos el fondo de la ventana transparente para que se vean los bordes curvos de nuestra tarjeta
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Referencias a las vistas del layout
        val tvTitulo = dialogView.findViewById<TextView>(R.id.tvDialogTitulo)
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvDialogMensaje)
        val iconInfo = dialogView.findViewById<ImageView>(R.id.iconDialogInfo)
        val btnEntendido = dialogView.findViewById<MaterialButton>(R.id.btnDialogEntendido)

        // Asignar textos (Procesando el HTML de strings.xml)
        tvTitulo.text = titulo
        tvMensaje.text = Html.fromHtml(mensaje, Html.FROM_HTML_MODE_LEGACY)

        // Extraer el color Aura y aplicarlo para mantener la consistencia en toda la app
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        iconInfo.imageTintList = ColorStateList.valueOf(auraColor)
        btnEntendido.backgroundTintList = ColorStateList.valueOf(auraColor)

        // Acción del botón para cerrar el diálogo flotante
        btnEntendido.setOnClickListener {
            dialog.dismiss()
        }

        // Mostrar la animación de aparición
        dialog.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}