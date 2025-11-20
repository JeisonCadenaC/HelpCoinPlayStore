package com.example.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finance_code.databinding.FragmentCalculadoraPrestamoBinding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

class CalculadoraPrestamoFragment : Fragment() {

    private var _binding: FragmentCalculadoraPrestamoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculadoraPrestamoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolverPrestamo.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCalcularPrestamo.setOnClickListener {
            calcularCuotaTotal()
        }
    }

    private fun calcularCuotaTotal() {
        val principalStr = binding.etMontoPrestamo.text.toString()
        val tasaMensualStr = binding.etTasaMensual.text.toString()
        val mesesStr = binding.etPlazoMeses.text.toString()
        val cuotaManejoStr = binding.etCuotaManejo.text.toString().ifEmpty { "0" }

        if (principalStr.isEmpty() || tasaMensualStr.isEmpty() || mesesStr.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, complete los campos principales.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val principal = principalStr.toDouble()
            val tasaMensualPorcentaje = tasaMensualStr.toDouble()
            val meses = mesesStr.toInt()
            val cuotaManejo = cuotaManejoStr.toDouble()

            if (principal <= 0 || meses <= 0 || tasaMensualPorcentaje < 0) {
                Toast.makeText(requireContext(), "El Monto y el Plazo deben ser mayores a cero. La Tasa debe ser positiva.", Toast.LENGTH_SHORT).show()
                return
            }

            val tasaMensual = tasaMensualPorcentaje / 100.0

            val cuotaAmortizacion: Double

            if (tasaMensual > 0.0) {
                val numerador = principal * tasaMensual
                val denominador = 1.0 - (1.0 + tasaMensual).pow(-meses)
                cuotaAmortizacion = numerador / denominador
            } else {
                cuotaAmortizacion = principal / meses
            }

            var saldoIteracion = principal
            var interesesTotales = 0.0

            for (i in 1..meses) {
                if (saldoIteracion <= 0) break
                val interesDelMes = saldoIteracion * tasaMensual
                interesesTotales += interesDelMes

                val amortizacionPrincipal = cuotaAmortizacion - interesDelMes
                saldoIteracion -= amortizacionPrincipal
            }

            val totalCuotaManejo = cuotaManejo * meses.toDouble()
            val pagoTotalFinal = principal + interesesTotales + totalCuotaManejo

            val cuotaTotal = cuotaAmortizacion + cuotaManejo
            val interesPrimerMes = principal * tasaMensual


            val formatTotal = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            formatTotal.maximumFractionDigits = 0

            val formatInteres = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            formatInteres.maximumFractionDigits = 2

            binding.tvCuotaTotal.text = formatTotal.format(cuotaTotal)
            binding.tvInteresEstimado.text = formatInteres.format(interesPrimerMes)

            binding.tvTotalIntereses.text = formatTotal.format(interesesTotales)
            binding.tvPagoTotalFinal.text = formatTotal.format(pagoTotalFinal)

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en el formato de los números. Verifique sus entradas.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}