package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance_code.R
import com.example.finance_code.data.MetaDB
import com.example.finance_code.databinding.FragmentMetasBinding
import com.example.finance_code.viewmodel.MetaViewModel
import com.example.finance_code.viewmodel.MetaViewModelFactory

class MetasFragment : Fragment() {

    private var _binding: FragmentMetasBinding? = null
    private val binding get() = _binding!!

    private val metaViewModel: MetaViewModel by viewModels {
        MetaViewModelFactory(requireActivity().application)
    }

    private lateinit var metasAdapter: MetasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetasBinding.inflate(inflater, container, false)

        // Configurar RecyclerView
        metasAdapter = MetasAdapter(onActualizarClick = { meta ->
            mostrarDialogoMeta(meta)
        })

        binding.rvMetas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = metasAdapter
        }

        // Observar datos desde ViewModel
        metaViewModel.allMetas.observe(viewLifecycleOwner) { metas ->
            metasAdapter.setData(metas)
        }

        // Botón flotante para agregar nueva meta
        binding.fabAddMeta.setOnClickListener {
            mostrarDialogoMeta(null)
        }

        return binding.root
    }

    private fun mostrarDialogoMeta(metaDBExistente: MetaDB?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_meta, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreMeta)
        val etMontoObjetivo = dialogView.findViewById<EditText>(R.id.etMontoObjetivo)
        val etMontoActual = dialogView.findViewById<EditText>(R.id.etMontoActual)

        // Mostrar datos si ya existe la meta
        if (metaDBExistente != null) {
            etNombre.setText(metaDBExistente.nombre)

            // ✅ Evitar notación científica
            etMontoObjetivo.setText(metaDBExistente.montoObjetivo.toLong().toString())
            etMontoActual.setText(metaDBExistente.montoActual.toLong().toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (metaDBExistente == null) "Nueva meta" else "Editar meta")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()

                // ✅ Convertir correctamente evitando errores
                val montoObjetivo = etMontoObjetivo.text.toString()
                    .replace(",", "")
                    .toDoubleOrNull() ?: 0.0

                val montoActual = etMontoActual.text.toString()
                    .replace(",", "")
                    .toDoubleOrNull() ?: 0.0

                if (nombre.isNotEmpty() && montoObjetivo > 0) {
                    if (metaDBExistente == null) {
                        val nuevaMetaDB = MetaDB(
                            nombre = nombre,
                            montoObjetivo = montoObjetivo,
                            montoActual = montoActual
                        )
                        metaViewModel.insert(nuevaMetaDB)
                    } else {
                        val actualizada = metaDBExistente.copy(
                            nombre = nombre,
                            montoObjetivo = montoObjetivo,
                            montoActual = montoActual
                        )
                        metaViewModel.update(actualizada)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}