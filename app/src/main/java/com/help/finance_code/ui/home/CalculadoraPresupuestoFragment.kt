package com.help.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.help.finance_code.databinding.FragmentCalculadoraPresupuestoBinding
import java.text.NumberFormat
import java.util.Locale

class CalculadoraPresupuestoFragment : Fragment() {

    private var _binding: FragmentCalculadoraPresupuestoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculadoraPresupuestoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolverPresupuesto.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnDistribuirPresupuesto.setOnClickListener {
            distribuirPresupuesto()
        }
    }

    private fun distribuirPresupuesto() {
        val ingresoStr = binding.etIngresoMensualNeto.text.toString()

        if (ingresoStr.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, ingrese el Ingreso Mensual Neto.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val ingresoNeto = ingresoStr.toDouble()

            if (ingresoNeto <= 0) {
                Toast.makeText(requireContext(), "El Ingreso debe ser mayor a cero.", Toast.LENGTH_SHORT).show()
                return
            }

            val necesidades = ingresoNeto * 0.50
            val deseos = ingresoNeto * 0.30
            val ahorroDeuda = ingresoNeto * 0.20

            val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            format.maximumFractionDigits = 0

            binding.tvResultadoNecesidades.text = format.format(necesidades)
            binding.tvResultadoDeseos.text = format.format(deseos)
            binding.tvResultadoAhorroDeuda.text = format.format(ahorroDeuda)

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en el formato del Ingreso. Use solo dígitos.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}