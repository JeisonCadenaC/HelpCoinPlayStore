package com.example.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finance_code.R
import com.example.finance_code.databinding.FragmentCalculadoraAmortizacionBinding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

class CalculadoraAmortizacionFragment : Fragment() {

    private var _binding: FragmentCalculadoraAmortizacionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculadoraAmortizacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolverPagoRapido.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCalcularPagoRapido.setOnClickListener {
            calcularAhorroAmortizacion()
        }
    }

    private fun calcularAhorroAmortizacion() {
        val principalStr = binding.etMontoPrestamo.text.toString()
        val tasaMensualStr = binding.etTasaMensual.text.toString()
        val mesesStr = binding.etPlazoMeses.text.toString()
        val montoExtraStr = binding.etAbonoExtraCapital.text.toString().ifEmpty { "0" }

        if (principalStr.isEmpty() || tasaMensualStr.isEmpty() || mesesStr.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, complete los campos principales.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val principalOriginal = principalStr.toDouble()
            val tasaMensualPorcentaje = tasaMensualStr.toDouble()
            val mesesOriginal = mesesStr.toInt()
            val montoExtra = montoExtraStr.toDouble()

            if (principalOriginal <= 0 || mesesOriginal <= 0 || tasaMensualPorcentaje < 0) {
                Toast.makeText(requireContext(), "El Monto y el Plazo deben ser mayores a cero. La Tasa debe ser positiva.", Toast.LENGTH_SHORT).show()
                return
            }

            val tasaMensual = tasaMensualPorcentaje / 100.0

            if (tasaMensual <= 0.0) {
                mostrarResultados(0.0, 0)
                return
            }

            val numeradorPMT = principalOriginal * tasaMensual
            val denominadorPMT = 1.0 - (1.0 + tasaMensual).pow(-mesesOriginal)
            val cuotaMensualFija = numeradorPMT / denominadorPMT

            var saldo = principalOriginal
            var interesesTotalesOriginal = 0.0

            for (i in 1..mesesOriginal) {
                val interes = saldo * tasaMensual
                interesesTotalesOriginal += interes
                val amortizacion = cuotaMensualFija - interes
                saldo -= amortizacion
                if (saldo <= 0) break
            }

            saldo = principalOriginal
            var interesesTotalesAcelerado = 0.0
            var mesesAcelerado = 0

            while (saldo > 0 && mesesAcelerado < 5000) {
                val interes = saldo * tasaMensual
                interesesTotalesAcelerado += interes

                var pagoTotal = cuotaMensualFija + montoExtra

                if (pagoTotal > saldo + interes) {
                    pagoTotal = saldo + interes
                }

                val amortizacion = pagoTotal - interes
                saldo -= amortizacion
                mesesAcelerado++

                if (saldo <= 0) break
            }

            val ahorroIntereses = interesesTotalesOriginal - interesesTotalesAcelerado
            val mesesAhorrados = mesesOriginal - mesesAcelerado

            mostrarResultados(ahorroIntereses, mesesAhorrados)

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en el formato de los números. Verifique sus entradas.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al realizar el cálculo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarResultados(ahorroIntereses: Double, mesesAhorrados: Int) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        currencyFormat.maximumFractionDigits = 0

        val yearsAhorrados = mesesAhorrados / 12.0

        binding.tvAhorroInteresesTotal.text = currencyFormat.format(ahorroIntereses)

        binding.tvReduccionPlazoTotal.text = if (mesesAhorrados > 0) {
            getString(R.string.meses_ahorrados, mesesAhorrados, yearsAhorrados.toFloat())
        } else {
            "0 meses (0.0 años) ahorrados"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}