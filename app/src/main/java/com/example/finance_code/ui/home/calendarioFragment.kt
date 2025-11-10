package com.example.finance_code.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
            saveReminder()
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

    private fun saveReminder() {
        val nombre = binding.etNombre.text.toString()

        if (nombre.isBlank()) {
            Toast.makeText(requireContext(), "Por favor, escribe un nombre", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isDateSelected) {
            Toast.makeText(requireContext(), "Por favor, selecciona una fecha", Toast.LENGTH_SHORT).show()
            return
        }

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


        ReminderHelper.createGoogleCalendarEvent(
            requireContext(),
            nombre,
            fechaEnMillis
        )


        findNavController().popBackStack()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}