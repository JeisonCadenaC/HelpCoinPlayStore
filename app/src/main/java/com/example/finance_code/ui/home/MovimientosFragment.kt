package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.widget.ImageButton
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var adapter: MovimientosAdapter

    private var isBalanceVisible = true
    private var currentBalance = 0.0
    private var currentUserName: String = "USUARIO"

    private val PREFS_NAME = "MovimientosFragmentPrefs"
    private val SHAKE_DIALOG_SHOWN_KEY = "shake_dialog_shown"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val userEmail = user?.email
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            return
        }

        var tempName: String? = user.displayName

        if (tempName.isNullOrEmpty()) {
            val prefs = requireContext().getSharedPreferences("${user.uid}_UserProfilePrefs", Context.MODE_PRIVATE)
            tempName = prefs.getString("user_name", "")
        }

        if (tempName.isNullOrEmpty()) {
            tempName = userEmail.substringBefore("@")
        }

        currentUserName = tempName?.uppercase() ?: "USUARIO"

        val database = AppDB.getDatabase(requireContext(), userEmail)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.listMovies)
        val tvSaldoTotal = view.findViewById<TextView>(R.id.tvSaldoTotal)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val btnHideBalance = view.findViewById<ImageButton>(R.id.btnHideBalance)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddTransaction)

        tvUserName.text = currentUserName

        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), addTransaction::class.java)
            startActivity(intent)
        }

        btnHideBalance.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            updateBalanceDisplay(tvSaldoTotal, tvUserName, btnHideBalance)
        }

        adapter = MovimientosAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter.setOnItemLongClickListener { movimiento ->
            mostrarMenuOpciones(movimiento)
        }

        viewModel.movimientos.observe(viewLifecycleOwner) { lista ->
            adapter.setData(lista)
        }

        viewModel.saldoTotal.observe(viewLifecycleOwner) { saldo ->
            currentBalance = saldo ?: 0.0
            updateBalanceDisplay(tvSaldoTotal, tvUserName, btnHideBalance)
        }

        mostrarDialogoModoDiscreto()
    }

    private fun updateBalanceDisplay(tvSaldo: TextView, tvName: TextView, btnIcon: ImageButton) {
        if (isBalanceVisible) {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.maximumFractionDigits = 0
            tvSaldo.text = format.format(currentBalance)
            tvName.text = currentUserName
            btnIcon.setImageResource(R.drawable.ic_visibility)
        } else {
            tvSaldo.text = "$ •••••"
            tvName.text = "••••••"
            btnIcon.setImageResource(R.drawable.ic_visibility_off)
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
        try {
            findNavController().navigate(R.id.eTransactionFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error de navegación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminarMovimiento(movimiento: Movimiento) {
        viewModel.eliminar(movimiento)
        Toast.makeText(requireContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoModoDiscreto() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dialogShown = prefs.getBoolean(SHAKE_DIALOG_SHOWN_KEY, false)

        if (!dialogShown) {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modo_discreto, null)
            val builder = AlertDialog.Builder(requireContext())
            builder.setView(view)
            val dialog = builder.create()

            val btnEntendido = view.findViewById<android.widget.Button>(R.id.btnEntendido)
            btnEntendido.setOnClickListener {
                dialog.dismiss()
                prefs.edit().putBoolean(SHAKE_DIALOG_SHOWN_KEY, true).apply()
            }

            dialog.show()
        }
    }
}