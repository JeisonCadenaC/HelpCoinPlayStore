package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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

        metasAdapter = MetasAdapter(onActualizarClick = { meta ->
            mostrarDialogoMeta(meta)
        })

        binding.rvMetas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = metasAdapter
        }

        metaViewModel.allMetas.observe(viewLifecycleOwner) { metas ->
            metasAdapter.setData(metas)
        }

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
        val btnEliminar = dialogView.findViewById<Button>(R.id.btnEliminarMeta)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()

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

        if (metaDBExistente != null) {
            builder.setTitle("Editar meta")
            dialogTitle.text = "Editar meta"

            etNombre.setText(metaDBExistente.nombre)
            etMontoObjetivo.setText(metaDBExistente.montoObjetivo.toLong().toString())
            etMontoActual.setText(metaDBExistente.montoActual.toLong().toString())

            btnEliminar.visibility = View.VISIBLE

        } else {
            builder.setTitle("Nueva meta")
            dialogTitle.text = "Añadir Nueva Meta"
            btnEliminar.visibility = View.GONE
        }

        val dialog = builder.create()
        dialog.show()

        if (metaDBExistente != null) {
            btnEliminar.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que quieres eliminar la meta '${metaDBExistente.nombre}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        metaViewModel.delete(metaDBExistente)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}