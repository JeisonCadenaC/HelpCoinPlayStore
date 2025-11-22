package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.Recordatorio
import com.example.finance_code.databinding.FragmentInicioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioViewModel by activityViewModels()
    private lateinit var agendaAdapter: AgendaAdapter
    private var selectedDate: Calendar = Calendar.getInstance()

    private var listaRecordatoriosApp: List<Recordatorio> = emptyList()

    data class EventoCombinado(
        val id: String,
        val titulo: String,
        val horaMillis: Long,
        val fuente: String
    )

    private val requestCalendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            actualizarAgendaCombinada()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()

        viewModel.todosLosRecordatorios.observe(viewLifecycleOwner) { recordatorios ->
            listaRecordatoriosApp = recordatorios
            actualizarAgendaCombinada()
        }

        binding.fabNuevoRecordatorio.setOnClickListener {
            findNavController().navigate(R.id.action_inicioFragment_to_calendarioFragment)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            actualizarAgendaCombinada()
        } else {
            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaAdapter(emptyList()) { evento ->
            confirmarEliminacion(evento)
        }
        binding.rvAgendaCombinada.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAgendaCombinada.adapter = agendaAdapter
    }

    private fun setupCalendar() {
        val today = System.currentTimeMillis()
        binding.calendarViewInicio.minDate = today - 31536000000L

        binding.calendarViewInicio.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            actualizarAgendaCombinada()
        }
    }

    private fun actualizarAgendaCombinada() {
        val eventosDelDia = mutableListOf<EventoCombinado>()
        val startMillis = obtenerInicioDia(selectedDate)
        val endMillis = obtenerFinDia(selectedDate)
        val filtradosApp = listaRecordatoriosApp.filter {
            it.fechaMillis in startMillis..endMillis
        }.map {
            EventoCombinado(it.id.toString(), it.nombre, it.fechaMillis, "Finanzas App")
        }
        eventosDelDia.addAll(filtradosApp)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            eventosDelDia.addAll(obtenerEventosSistema(startMillis, endMillis))
        }

        val listaOrdenada = eventosDelDia.sortedBy { it.horaMillis }
        agendaAdapter.actualizarLista(listaOrdenada)

        if (listaOrdenada.isEmpty()) {
            binding.txtTituloAgenda.text = "No hay eventos para este día"
        } else {
            binding.txtTituloAgenda.text = "Agenda del día"
        }
    }

    private fun obtenerEventosSistema(startMillis: Long, endMillis: Long): List<EventoCombinado> {
        val lista = mutableListOf<EventoCombinado>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )
        val selection = "(( ${CalendarContract.Events.DTSTART} >= ?) AND ( ${CalendarContract.Events.DTSTART} <= ?))"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        try {
            val cursor: Cursor? = requireContext().contentResolver.query(
                CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val dateIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val calNameIdx = it.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) else ""
                    val titulo = if (titleIdx != -1) it.getString(titleIdx) else "Sin título"
                    val fecha = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val fuente = if (calNameIdx != -1) it.getString(calNameIdx) else "Calendario"
                    lista.add(EventoCombinado(id, titulo, fecha, fuente))
                }
            }
        } catch (e: Exception) { }
        return lista
    }

    private fun obtenerInicioDia(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun obtenerFinDia(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun confirmarEliminacion(evento: EventoCombinado) {
        val recordatorio = listaRecordatoriosApp.find { it.id.toString() == evento.id }
        if (recordatorio != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Deseas eliminar '${evento.titulo}'?")
                .setPositiveButton("Eliminar") { _, _ ->
                    viewModel.eliminarRecordatorio(recordatorio)
                    Toast.makeText(requireContext(), "Eliminado", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            Toast.makeText(requireContext(), "Este evento es del calendario del sistema (Google/Samsung), debes borrarlo en su app.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class AgendaAdapter(
        private var eventos: List<EventoCombinado>,
        private val onDeleteClick: (EventoCombinado) -> Unit
    ) : RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>() {

        private var idItemSeleccionado: String? = null

        class AgendaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.txtNombreRecordatorio)
            val tvFecha: TextView = view.findViewById(R.id.txtFechaRecordatorio)
            val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recordatorio, parent, false)
            return AgendaViewHolder(view)
        }

        override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
            val evento = eventos[position]

            holder.tvNombre.text = evento.titulo

            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(evento.horaMillis))
            holder.tvFecha.text = "$hora • ${evento.fuente}"

            val esDeMiApp = evento.fuente == "Finanzas App"

            if (esDeMiApp && evento.id == idItemSeleccionado) {
                holder.btnEliminar.visibility = View.VISIBLE
            } else {
                holder.btnEliminar.visibility = View.GONE
            }

            holder.itemView.setOnLongClickListener {
                if (esDeMiApp) {
                    idItemSeleccionado = if (idItemSeleccionado == evento.id) null else evento.id
                    notifyDataSetChanged()
                    true
                } else {
                    Toast.makeText(holder.itemView.context, "Evento externo (no se puede borrar aquí)", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            holder.itemView.setOnClickListener {
                if (idItemSeleccionado != null) {
                    idItemSeleccionado = null
                    notifyDataSetChanged()
                }
            }

            holder.btnEliminar.setOnClickListener {
                onDeleteClick(evento)
                idItemSeleccionado = null
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = eventos.size

        fun actualizarLista(nuevosEventos: List<EventoCombinado>) {
            eventos = nuevosEventos
            notifyDataSetChanged()
        }
    }
}