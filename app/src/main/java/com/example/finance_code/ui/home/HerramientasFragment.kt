package com.example.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finance_code.R
import com.example.finance_code.databinding.FragmentHerramientasBinding

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}