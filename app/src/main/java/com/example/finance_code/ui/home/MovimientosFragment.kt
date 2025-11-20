package com.example.finance_code.ui.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
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
import com.example.finance_code.DiscreetModeManager
import com.example.finance_code.ShakeDetector
import java.text.NumberFormat
import java.util.Locale
import androidx.core.content.ContextCompat

import com.example.finance_code.ui.home.MovimientosAdapter


class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var adapter: MovimientosAdapter

    private var isBalanceVisible = true
    private var currentBalance = 0.0
    private var currentUserName: String = "USUARIO"

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

    private val PREFS_FILE = "DiscreetModePrefs"
    private val HAS_SEEN_INFO_KEY = "has_seen_discreet_info"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DiscreetModeManager.initialize(requireContext().applicationContext)

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
            updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)
        }

        setupSensors()

        adapter = MovimientosAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter.setOnItemLongClickListener { movimiento ->
            mostrarMenuOpciones(movimiento)
        }

        DiscreetModeManager.modeChangeListener = {
            updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)
        }

        updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)

        viewModel.movimientos.observe(viewLifecycleOwner) { lista ->
            adapter.setData(lista)
            updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)
        }

        viewModel.saldoTotal.observe(viewLifecycleOwner) { saldo ->
            currentBalance = saldo ?: 0.0
            updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)
        }

        showDiscreetModeInfo()
    }

    private fun showDiscreetModeInfo() {
        val prefs = requireActivity().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val hasSeenInfo = prefs.getBoolean(HAS_SEEN_INFO_KEY, false)

        if (!hasSeenInfo) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_modo_discreto, null)
            val btnEntendido = dialogView.findViewById<Button>(R.id.btnEntendido)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()

            btnEntendido.setOnClickListener {
                prefs.edit().putBoolean(HAS_SEEN_INFO_KEY, true).apply()
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun setupSensors() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)

        shakeDetector = ShakeDetector {
            DiscreetModeManager.toggleMode()

            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }
            }

            val mensaje = if (DiscreetModeManager.isDiscreetModeActive) "Modo Discreto Activado" else "Modo Visible Activado"
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAllUI(tvSaldo: TextView, tvName: TextView, btnIcon: ImageButton) {
        val isGlobalDiscreet = DiscreetModeManager.isDiscreetModeActive

        val shouldCensorCard = isGlobalDiscreet || !isBalanceVisible

        if (!shouldCensorCard) {
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

        adapter.updateDiscreetMode()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager?.registerListener(shakeDetector, accel, SensorManager.SENSOR_DELAY_UI)
        }

        val tvSaldoTotal = requireView().findViewById<TextView>(R.id.tvSaldoTotal)
        val tvUserName = requireView().findViewById<TextView>(R.id.tvUserName)
        val btnHideBalance = requireView().findViewById<ImageButton>(R.id.btnHideBalance)

        DiscreetModeManager.modeChangeListener = {
            updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)
        }
        updateAllUI(tvSaldoTotal, tvUserName, btnHideBalance)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
        DiscreetModeManager.modeChangeListener = null
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
}