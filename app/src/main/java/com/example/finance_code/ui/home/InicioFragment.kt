package com.example.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance_code.R
import com.example.finance_code.databinding.FragmentInicioBinding

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioViewModel by activityViewModels()


    private lateinit var recordatorioAdapter: RecordatorioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        val root: View = binding.root


        binding.cardCalendario.setOnClickListener {
            findNavController().navigate(R.id.action_inicioFragment_to_calendarioFragment)
        }


        setupRecyclerView()


        viewModel.todosLosRecordatorios.observe(viewLifecycleOwner) { listaDeRecordatorios ->

            recordatorioAdapter.submitList(listaDeRecordatorios)
        }

        return root
    }

    private fun setupRecyclerView() {

        recordatorioAdapter = RecordatorioAdapter { recordatorioParaEliminar ->

            viewModel.eliminarRecordatorio(recordatorioParaEliminar)
        }


        binding.rvRecordatorios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordatorioAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}