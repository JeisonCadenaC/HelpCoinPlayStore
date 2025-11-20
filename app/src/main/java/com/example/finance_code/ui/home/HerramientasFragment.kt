package com.example.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finance_code.R
import com.example.finance_code.databinding.FragmentHerramientasBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.text.Html

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


        binding.btnInfoAhorro.setOnClickListener {
            mostrarInformacion(getString(R.string.info_ahorro_titulo), getString(R.string.info_ahorro_mensaje))
        }

        binding.btnInfoPrestamo.setOnClickListener {
            mostrarInformacion(getString(R.string.info_prestamo_titulo), getString(R.string.info_prestamo_mensaje))
        }

        binding.btnInfoAmortizacion.setOnClickListener {
            mostrarInformacion(getString(R.string.info_amortizacion_titulo), getString(R.string.info_amortizacion_mensaje))
        }

        binding.btnInfoPresupuesto.setOnClickListener {
            mostrarInformacion(getString(R.string.info_presupuesto_titulo), getString(R.string.info_presupuesto_mensaje))
        }
    }

    private fun mostrarInformacion(titulo: String, mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(Html.fromHtml(mensaje, Html.FROM_HTML_MODE_LEGACY))
            .setIcon(R.drawable.ic_info)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}