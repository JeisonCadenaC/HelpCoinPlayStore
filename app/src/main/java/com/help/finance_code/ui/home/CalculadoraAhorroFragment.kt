package com.help.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.help.finance_code.databinding.FragmentCalculadoraAhorroBinding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

class CalculadoraAhorroFragment : Fragment() {

    private var _binding: FragmentCalculadoraAhorroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculadoraAhorroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVolverAhorro.setOnClickListener {
            findNavController().navigateUp()
        }

        // Configurar botón de calcular
        binding.btnCalcularAhorro.setOnClickListener {
            calcularAhorro()
        }
    }

    private fun calcularAhorro() {
        val capitalInicialStr = binding.etCapitalInicial.text.toString()
        val aporteMensualStr = binding.etAporteMensual.text.toString()
        val tasaAnualStr = binding.etTasaAnual.text.toString()
        val anosStr = binding.etPeriodoAnos.text.toString()
        if (capitalInicialStr.isEmpty() || aporteMensualStr.isEmpty() || tasaAnualStr.isEmpty() || anosStr.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val capitalInicial = capitalInicialStr.toDouble()
            val aporteMensual = aporteMensualStr.toDouble()
            val tasaAnual = tasaAnualStr.toDouble()
            val anos = anosStr.toInt()
            val tasaMensual = tasaAnual / 100 / 12
            val totalMeses = anos * 12

            val parteCapital = capitalInicial * (1 + tasaMensual).pow(totalMeses)

            val parteAportes = if (tasaMensual > 0) {
                aporteMensual * (((1 + tasaMensual).pow(totalMeses) - 1) / tasaMensual)
            } else {
                aporteMensual * totalMeses
            }

            val resultadoFinal = parteCapital + parteAportes

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            currencyFormat.maximumFractionDigits = 0
            binding.tvResultadoAhorro.text = currencyFormat.format(resultadoFinal)

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Ingrese valores numéricos válidos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}