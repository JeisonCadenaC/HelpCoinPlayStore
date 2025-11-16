package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

class CalendarioFragment : Fragment() {

    private var _binding: FragmentCalendarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioViewModel by activityViewModels()

    private var selectedDate: Calendar = Calendar.getInstance()
    private var isDateSelected = false

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                verificarPermisoAlarmaExacta()
            } else {
                Toast.makeText(requireContext(), "Permiso de notificación denegado.", Toast.LENGTH_SHORT).show()
            }
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

        binding.btnSeleccionarFecha.setOnClickListener {
            showDatePicker()
        }

        binding.btnGuardarRecordatorio.setOnClickListener {
            intentarGuardarRecordatorio()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                isDateSelected = true
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaFormateada = sdf.format(selectedDate.time)
                binding.btnSeleccionarFecha.text = "Fecha: $fechaFormateada"
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun intentarGuardarRecordatorio() {
        val nombre = binding.etNombre.text.toString()

        if (nombre.isBlank()) {
            Toast.makeText(requireContext(), "Por favor, escribe un nombre", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isDateSelected) {
            Toast.makeText(requireContext(), "Por favor, selecciona una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        verificarPermisoNotificacion()
    }

    private fun verificarPermisoNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    verificarPermisoAlarmaExacta()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Permiso Requerido")
                        .setMessage("Necesitamos permiso para enviar notificaciones y así poder mostrarte recordatorios.")
                        .setPositiveButton("OK") { _, _ ->
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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
                AlertDialog.Builder(requireContext())
                    .setTitle("Permiso Requerido")
                    .setMessage("Para que los recordatorios funcionen, la app necesita permiso para programar alarmas. Serás llevado a los ajustes del sistema.")
                    .setPositiveButton("Ir a Ajustes") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        } else {
            saveReminder()
        }
    }

    private fun saveReminder() {
        val nombre = binding.etNombre.text.toString()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = sdf.format(selectedDate.time)
        val fechaEnMillis = selectedDate.timeInMillis

        val nuevoRecordatorio = Recordatorio(
            nombre = nombre,
            fechaMillis = fechaEnMillis
        )
        viewModel.insertarRecordatorio(nuevoRecordatorio)

        ReminderHelper.scheduleNotifications(
            requireContext(),
            nombre,
            selectedDate,
            fechaFormateada
        )

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}