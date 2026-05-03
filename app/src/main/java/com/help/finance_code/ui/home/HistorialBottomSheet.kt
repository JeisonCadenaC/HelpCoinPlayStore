package com.help.finance_code.ui.home

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.help.finance_code.R
import com.help.finance_code.data.AppDB
import com.help.finance_code.data.Movimiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistorialBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvMovimientos: RecyclerView
    private lateinit var tvTotalIngresos: TextView
    private lateinit var tvTotalEgresos: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnClose: ImageButton

    // Filtros
    private lateinit var cgTipos: ChipGroup
    private lateinit var cgTiempo: ChipGroup
    private lateinit var chipCustomDate: Chip

    private lateinit var database: AppDB
    private var todosLosMovimientos: List<Movimiento> = emptyList()
    private lateinit var adapter: MovimientosAdapter

    private var fechaInicioCustom: Date? = null
    private var fechaFinCustom: Date? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // CORRECCIÓN: Inflamos el layout NUEVO, no el viejo
        return inflater.inflate(R.layout.layout_historial_movimientos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Vistas
        rvMovimientos = view.findViewById(R.id.rvMovimientosFiltrados)
        tvTotalIngresos = view.findViewById(R.id.tvTotalIngresosFiltro)
        tvTotalEgresos = view.findViewById(R.id.tvTotalEgresosFiltro)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        btnClose = view.findViewById(R.id.btnClose)

        cgTipos = view.findViewById(R.id.cgTipos)
        cgTiempo = view.findViewById(R.id.cgTiempo)
        chipCustomDate = view.findViewById(R.id.chipCustomDate)

        rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        adapter = MovimientosAdapter(emptyList())
        rvMovimientos.adapter = adapter

        btnClose.setOnClickListener { dismiss() }

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail != null) {
            database = AppDB.getDatabase(requireContext(), userEmail)
            cargarTodosLosMovimientos()
        }

        cgTipos.setOnCheckedStateChangeListener { _, _ -> aplicarFiltros() }

        cgTiempo.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.contains(R.id.chipCustomDate)) {
                mostrarSelectorFechas()
            } else {
                fechaInicioCustom = null
                fechaFinCustom = null
                chipCustomDate.text = "Elegir fechas..."
                aplicarFiltros()
            }
        }
    }

    private fun cargarTodosLosMovimientos() {
        lifecycleScope.launch(Dispatchers.IO) {
            todosLosMovimientos = database.movimientoDao().obtenerTodosSync()
            withContext(Dispatchers.Main) {
                aplicarFiltros()
            }
        }
    }

    private fun aplicarFiltros() {
        var listaFiltrada = todosLosMovimientos

        when (cgTipos.checkedChipId) {
            R.id.chipIngresos -> listaFiltrada = listaFiltrada.filter { it.tipo == 1 }
            R.id.chipEgresos -> listaFiltrada = listaFiltrada.filter { it.tipo == 0 }
        }

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoy = Calendar.getInstance()

        when (cgTiempo.checkedChipId) {
            R.id.chipMesActual -> {
                val mesActualStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(hoy.time)
                listaFiltrada = listaFiltrada.filter { it.fecha.startsWith(mesActualStr) }
            }
            R.id.chipUltimos7 -> {
                val hace7Dias = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                listaFiltrada = listaFiltrada.filter {
                    try {
                        val fechaMov = format.parse(it.fecha)
                        fechaMov != null && !fechaMov.before(hace7Dias)
                    } catch (e: Exception) { false }
                }
            }
            R.id.chipCustomDate -> {
                if (fechaInicioCustom != null && fechaFinCustom != null) {
                    listaFiltrada = listaFiltrada.filter {
                        try {
                            val fechaMov = format.parse(it.fecha)
                            fechaMov != null && !fechaMov.before(fechaInicioCustom) && !fechaMov.after(fechaFinCustom)
                        } catch (e: Exception) { false }
                    }
                }
            }
        }

        actualizarUI(listaFiltrada)
    }

    private fun actualizarUI(lista: List<Movimiento>) {
        if (lista.isEmpty()) {
            rvMovimientos.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvMovimientos.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE

            val itemsFinales = mutableListOf<MovimientoListItem>()
            val formatDia = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CO"))
            var currentHeader = ""

            lista.sortedByDescending { it.fecha }.forEach { mov ->
                val dateStr = try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(mov.fecha)
                    formatDia.format(date!!)
                } catch (e: Exception) { mov.fecha }

                if (dateStr != currentHeader) {
                    itemsFinales.add(MovimientoListItem.Header(dateStr.replaceFirstChar { it.uppercase() }))
                    currentHeader = dateStr
                }
                itemsFinales.add(MovimientoListItem.Item(mov))
            }
            val mapaBolsillos = todosLosMovimientos
                .filter { it.parentId == null }
                .associate { it.id to it.descripcion }

            adapter.setData(itemsFinales, mapaBolsillos)
        }

        val ingresos = lista.filter { it.tipo == 1 }.sumOf { it.cantidad }
        val egresos = lista.filter { it.tipo == 0 }.sumOf { it.cantidad }

        val formatter = DecimalFormat("$ #,###", DecimalFormatSymbols(Locale("es", "CO")))
        tvTotalIngresos.text = formatter.format(ingresos)
        tvTotalEgresos.text = formatter.format(egresos)
    }

    private fun mostrarSelectorFechas() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, anioInicio, mesInicio, diaInicio ->
            val calInicio = Calendar.getInstance().apply { set(anioInicio, mesInicio, diaInicio, 0, 0, 0) }
            fechaInicioCustom = calInicio.time

            DatePickerDialog(requireContext(), { _, anioFin, mesFin, diaFin ->
                val calFin = Calendar.getInstance().apply { set(anioFin, mesFin, diaFin, 23, 59, 59) }
                fechaFinCustom = calFin.time

                val formatUI = SimpleDateFormat("dd/MMM", Locale.getDefault())
                chipCustomDate.text = "${formatUI.format(fechaInicioCustom!!)} - ${formatUI.format(fechaFinCustom!!)}"
                aplicarFiltros()

            }, anioInicio, mesInicio, diaInicio).apply {
                datePicker.minDate = calInicio.timeInMillis
                setTitle("Fecha de Fin")
            }.show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Fecha de Inicio")
        }.show()
    }
}