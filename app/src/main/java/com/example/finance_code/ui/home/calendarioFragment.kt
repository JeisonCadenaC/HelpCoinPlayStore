package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.finance_code.data.Recordatorio
import com.example.finance_code.databinding.FragmentCalendarioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.finance_code.R

class CalendarioFragment : Fragment() {

    private var _binding: FragmentCalendarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioViewModel by activityViewModels()
    private var selectedDate: Calendar = Calendar.getInstance()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) verificarPermisoAlarmaExacta()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDateSummary(selectedDate)

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateDateSummary(selectedDate)
        }

        binding.btnGuardarRecordatorio.setOnClickListener { intentarGuardarRecordatorio() }
    }

    private fun updateDateSummary(date: Calendar) {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("es", "ES"))
        binding.tvSelectedDateSummary.text = sdf.format(date.time).replaceFirstChar { it.uppercase() }
    }

    private fun intentarGuardarRecordatorio() {
        if (binding.etNombre.text.toString().isBlank()) {
            Toast.makeText(requireContext(), "Escribe un nombre", Toast.LENGTH_SHORT).show()
            return
        }
        verificarPermisoNotificacion()
    }

    private fun verificarPermisoNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                verificarPermisoAlarmaExacta()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            verificarPermisoAlarmaExacta()
        }
    }

    private fun verificarPermisoAlarmaExacta() {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                saveReminder()
            } else {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        } else {
            saveReminder()
        }
    }

    private fun saveReminder() {
        val nombre = binding.etNombre.text.toString()
        val fechaMillis = selectedDate.timeInMillis
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val calendarTitle = "[Help Coin] $nombre"

        viewModel.insertarRecordatorio(Recordatorio(nombre = nombre, fechaMillis = fechaMillis))
        ReminderHelper.scheduleNotifications(requireContext(), nombre, selectedDate, sdf.format(selectedDate.time))

        try {
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, calendarTitle) // Usamos el título marcado
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fechaMillis + 3600000)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir la app de calendario nativo.", Toast.LENGTH_LONG).show()
        }

        Toast.makeText(requireContext(), "Guardado", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}