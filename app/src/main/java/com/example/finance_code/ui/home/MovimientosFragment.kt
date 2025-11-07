package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.ui.transaction.addTransaction
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory

class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var adapter: MovimientosAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // PASO 1: Obtener la base de datos
        val database = AppDB.getDatabase(requireContext())
        val buttonT = view.findViewById<Button>(R.id.button2)

        // PASO 2: Crear el Repository
        val repository = MovimientoRepository(database.movimientoDao())

        // PASO 3: Crear el Factory
        val factory = MovimientoViewModelFactory(repository)

        // PASO 4: Obtener el ViewModel
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]


        //Para abrir el fragment de agregar transaccion
        buttonT.setOnClickListener {
            val intent =
                Intent(requireContext(), addTransaction::class.java)
            startActivity(intent)
        }
        // Referencias a las vistas
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerMovimientos)
        val tvContador = view.findViewById<TextView>(R.id.tvContador)

        // Configurar RecyclerView
        adapter = MovimientosAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter.setOnItemLongClickListener { movimiento ->
            mostrarMenuOpciones(movimiento)
        }
        // Observar cambios en la base de datos
        viewModel.movimientos.observe(viewLifecycleOwner) { lista ->
            // Actualizar el adapter
            adapter.actualizarMovimientos(lista)

            // Actualizar contador
            tvContador.text = "Total: ${lista.size} movimientos"

            // Log para debug
            println("📊 Movimientos cargados: ${lista.size}")
            lista.forEach { mov ->
                println("  - ${mov.descripcion}: $${mov.cantidad}")
            }
        }

        // PASO 6: Observar los cambios
        viewModel.movimientos.observe(viewLifecycleOwner) { lista ->
            Toast.makeText(
                requireContext(),
                "Total: ${lista.size} movimientos",
                Toast.LENGTH_SHORT
            ).show()

            lista.forEach { mov ->
                println("💰 ${mov.descripcion}: $${mov.cantidad}")
            }
        }
    }
    private fun mostrarMenuOpciones(movimiento: Movimiento) {
        val opciones = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(requireContext())
            .setTitle("Acciones para \"${movimiento.descripcion}\"")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> editarMovimiento(movimiento)
                    1 -> eliminarMovimiento(movimiento)
                }
            }
            .show()
    }

    private fun editarMovimiento(movimiento: Movimiento) {
        val bundle = Bundle().apply {
            putParcelable("movimiento", movimiento)
        }
        findNavController().navigate(
            R.id.eTransactionFragment,
            bundle
        )
    }

    private fun eliminarMovimiento(movimiento: Movimiento) {
        viewModel.eliminar(movimiento)
        Toast.makeText(requireContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show()
    }

}


