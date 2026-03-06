package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.data.Recordatorio
import com.example.finance_code.databinding.FragmentInicioBinding
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioViewModel by activityViewModels()
    private lateinit var movimientoViewModel: MovimientoViewModel

    private lateinit var agendaAdapter: AgendaAdapter
    private var selectedDate: Calendar = Calendar.getInstance()

    private var listaRecordatoriosApp: List<Recordatorio> = emptyList()
    private var listaMovimientosReal: List<Movimiento> = emptyList()

    private var isChartExpandedGastos = false
    private var isChartExpandedIngresos = false

    private var filtroInicio: Long = 0L
    private var filtroFin: Long = Long.MAX_VALUE

    private var fechasIngresosFormat = mutableListOf<String>()

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

        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        if (userEmail.isNotEmpty()) {
            val database = AppDB.getDatabase(requireContext(), userEmail)
            val repository = MovimientoRepository(database.movimientoDao())
            val factory = MovimientoViewModelFactory(repository)
            movimientoViewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]
        }

        setupRecyclerView()
        setupCalendar()

        setupPieChartGastos(view)
        setupAnalisisGastosLayout(view)

        setupLineChartIngresos(view)
        setupAnalisisIngresosLayout(view)

        if (::movimientoViewModel.isInitialized) {
            movimientoViewModel.movimientos.observe(viewLifecycleOwner) { movimientos ->
                listaMovimientosReal = movimientos
                procesarGraficoGastos(view)
                procesarGraficoIngresos(view)
            }
        }

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

    private fun setupAnalisisGastosLayout(view: View) {
        val layoutCardHeader = view.findViewById<RelativeLayout>(R.id.layoutCardHeader)
        val layoutColapsable = view.findViewById<LinearLayout>(R.id.layoutColapsable)
        val imgExpandir = view.findViewById<ImageView>(R.id.imgExpandir)
        val btnFiltrarFechas = view.findViewById<MaterialButton>(R.id.btnFiltrarFechas)

        layoutCardHeader.setOnClickListener {
            isChartExpandedGastos = !isChartExpandedGastos
            if (isChartExpandedGastos) {
                layoutColapsable.visibility = View.VISIBLE
                imgExpandir.animate().rotation(180f).setDuration(250).start()
                view.findViewById<PieChart>(R.id.pieChartResumen).animateY(1000)
            } else {
                layoutColapsable.visibility = View.GONE
                imgExpandir.animate().rotation(0f).setDuration(250).start()
                view.findViewById<LinearLayout>(R.id.layoutDetalleSlice).visibility = View.GONE
            }
        }

        btnFiltrarFechas.setOnClickListener {
            mostrarDialogoFiltroMeses(view)
        }
    }

    private fun setupAnalisisIngresosLayout(view: View) {
        val layoutCardHeader = view.findViewById<RelativeLayout>(R.id.layoutCardHeaderIngresos)
        val layoutColapsable = view.findViewById<LinearLayout>(R.id.layoutColapsableIngresos)
        val imgExpandir = view.findViewById<ImageView>(R.id.imgExpandirIngresos)
        val btnFiltrarFechas = view.findViewById<MaterialButton>(R.id.btnFiltrarFechasIngresos)

        layoutCardHeader.setOnClickListener {
            isChartExpandedIngresos = !isChartExpandedIngresos
            if (isChartExpandedIngresos) {
                layoutColapsable.visibility = View.VISIBLE
                imgExpandir.animate().rotation(180f).setDuration(250).start()
                view.findViewById<LineChart>(R.id.lineChartIngresos).animateX(1000)
            } else {
                layoutColapsable.visibility = View.GONE
                imgExpandir.animate().rotation(0f).setDuration(250).start()
                view.findViewById<LinearLayout>(R.id.layoutDetalleSliceIngresos).visibility = View.GONE
            }
        }

        btnFiltrarFechas.setOnClickListener {
            mostrarDialogoFiltroMeses(view)
        }
    }

    private fun setupPieChartGastos(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChartResumen)
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.holeRadius = 50f
        pieChart.transparentCircleRadius = 55f
        pieChart.setDrawEntryLabels(false)
        pieChart.setExtraOffsets(0f, 0f, 0f, 0f)

        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.isWordWrapEnabled = true
        legend.textSize = 12f
        legend.form = Legend.LegendForm.CIRCLE
        legend.formSize = 10f
        legend.xEntrySpace = 12f
        legend.yEntrySpace = 8f
        legend.textColor = ContextCompat.getColor(requireContext(), android.R.color.tab_indicator_text)

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e is PieEntry) {
                    val layoutDetalle = view.findViewById<LinearLayout>(R.id.layoutDetalleSlice)
                    val txtCat = view.findViewById<TextView>(R.id.txtCategoriaSeleccionada)
                    val txtMonto = view.findViewById<TextView>(R.id.txtMontoSeleccionado)

                    layoutDetalle.visibility = View.VISIBLE
                    txtCat.text = e.label

                    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                    val porcentaje = String.format(Locale.getDefault(), "%.1f%%", (e.y / pieChart.data.yValueSum) * 100)
                    txtMonto.text = "${format.format(e.value)}\nRepresenta el $porcentaje"
                }
            }
            override fun onNothingSelected() {
                view.findViewById<LinearLayout>(R.id.layoutDetalleSlice).visibility = View.GONE
            }
        })
    }

    private fun setupLineChartIngresos(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChartIngresos)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = ContextCompat.getColor(requireContext(), android.R.color.tab_indicator_text)

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                val index = value.toInt()
                return if (index >= 0 && index < fechasIngresosFormat.size) fechasIngresosFormat[index] else ""
            }
        }

        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(true)
        yAxis.gridColor = Color.parseColor("#33888888")
        yAxis.textColor = ContextCompat.getColor(requireContext(), android.R.color.tab_indicator_text)

        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                return if (value >= 1000000) {
                    String.format(Locale.getDefault(), "%.1fM", value / 1000000)
                } else if (value >= 1000) {
                    String.format(Locale.getDefault(), "%.0fk", value / 1000)
                } else {
                    value.toInt().toString()
                }
            }
        }

        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val layoutDetalle = view.findViewById<LinearLayout>(R.id.layoutDetalleSliceIngresos)
                val txtFecha = view.findViewById<TextView>(R.id.txtFechaSeleccionadaIngresos)
                val txtMonto = view.findViewById<TextView>(R.id.txtMontoSeleccionadoIngresos)

                layoutDetalle.visibility = View.VISIBLE

                val index = e?.x?.toInt() ?: 0
                if (index >= 0 && index < fechasIngresosFormat.size) {
                    txtFecha.text = "Ingresos del ${fechasIngresosFormat[index]}"
                }

                val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                txtMonto.text = format.format(e?.y ?: 0f)
            }

            override fun onNothingSelected() {
                view.findViewById<LinearLayout>(R.id.layoutDetalleSliceIngresos).visibility = View.GONE
            }
        })
    }

    private fun obtenerCategoriaGastoInteligente(mov: Movimiento): Pair<String, Int> {
        val textoInfo = "${mov.categoria} ${mov.descripcion}".lowercase(Locale.getDefault())

        return when {
            textoInfo.matches(Regex(".*(comida|restaurante|almuerzo|cena|desayuno|pizza|hamburguesa|perro|empanada|helado|postre|snack|tinto).*")) ->
                Pair("🍔 Comida", Color.parseColor("#FF9800"))
            textoInfo.matches(Regex(".*(gasolina|moto|carro|repuestos|arreglo|mecanico|parqueadero|peaje|llanta|aceite|taller|vehiculo|soat|tecnomecanica).*")) ->
                Pair("🚗 Vehículo", Color.parseColor("#607D8B"))
            textoInfo.matches(Regex(".*(transporte|pasaje|bus|transmilenio|taxi|uber|didi|cabify|metro|picap).*")) ->
                Pair("🚌 Transporte", Color.parseColor("#03A9F4"))
            textoInfo.matches(Regex(".*(servicio|luz|agua|internet|recibo|gas|telefono|celular|plan|wifi|factura|arriendo|alquiler).*")) ->
                Pair("💡 Servicios y Recibos", Color.parseColor("#FFC107"))
            textoInfo.matches(Regex(".*(supermercado|mercado|despensa|viveres|tienda|d1|ara|exito|jumbo|olimpica|carulla|abastos).*")) ->
                Pair("🛒 Mercado", Color.parseColor("#4CAF50"))
            textoInfo.matches(Regex(".*(maquillaje|peluqueria|uñas|barbero|cuidado|crema|aseo|skincare|corte|perfume).*")) ->
                Pair("💅 Cuidado Personal", Color.parseColor("#E91E63"))
            textoInfo.matches(Regex(".*(salud|medicina|farmacia|medico|pastillas|hospital|eps|cita|droga|drogueria|examen).*")) ->
                Pair("💊 Salud", Color.parseColor("#F44336"))
            textoInfo.matches(Regex(".*(ropa|compras|zapatos|tenis|blusa|pantalon|chaqueta|centro comercial|mall|regalo|accesorio).*")) ->
                Pair("🛍️ Compras", Color.parseColor("#9C27B0"))
            textoInfo.matches(Regex(".*(educacion|estudio|universidad|colegio|cuaderno|libro|curso|matricula|pension|semestre|diplomado).*")) ->
                Pair("📚 Educación", Color.parseColor("#00BCD4"))
            textoInfo.matches(Regex(".*(viaje|hotel|vuelo|avion|vacaciones|turismo|paseo|hospedaje|airbnb|terminal).*")) ->
                Pair("✈️ Viajes", Color.parseColor("#3F51B5"))
            textoInfo.matches(Regex(".*(prostituta|puta|prepago|onlyfans|webcam|motel|cariñosa|chica|acompañante).*")) ->
                Pair("🔞 Ocio Nocturno", Color.parseColor("#B71C1C"))
            textoInfo.matches(Regex(".*(ocio|diversion|cine|rumba|fiesta|trago|cerveza|pola|licor|bar|netflix|spotify|suscripcion|videojuego|juego|xbox|play|suscripción).*")) ->
                Pair("🎉 Diversión", Color.parseColor("#CDDC39"))
            else ->
                Pair("🏷️ Otros Gastos", Color.parseColor("#795548"))
        }
    }

    private fun procesarGraficoGastos(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.pieChartResumen)
        val txtTotal = view.findViewById<TextView>(R.id.txtTotalGastos)
        val layoutDetalle = view.findViewById<LinearLayout>(R.id.layoutDetalleSlice)

        layoutDetalle.visibility = View.GONE
        val sdfParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val gastosFiltrados = listaMovimientosReal.filter { it.tipo == 0 }.filter { mov ->
            try {
                val time = sdfParser.parse(mov.fecha)?.time ?: 0L
                time in filtroInicio..filtroFin
            } catch (e: Exception) {
                true
            }
        }

        val agrupado = HashMap<String, Double>()
        val coloresAgrupados = HashMap<String, Int>()

        for (mov in gastosFiltrados) {
            val (nombreCategoria, colorAsignado) = obtenerCategoriaGastoInteligente(mov)
            agrupado[nombreCategoria] = (agrupado[nombreCategoria] ?: 0.0) + mov.cantidad
            coloresAgrupados[nombreCategoria] = colorAsignado
        }

        val totalGastos = agrupado.values.sum()
        val formatCurrency = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        txtTotal.text = formatCurrency.format(totalGastos)

        if (agrupado.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No hay gastos registrados en estas fechas.")
            pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        val coloresFinales = ArrayList<Int>()
        val listaOrdenada = agrupado.entries.sortedByDescending { it.value }

        for (entry in listaOrdenada) {
            if (entry.value > 0) {
                entries.add(PieEntry(entry.value.toFloat(), entry.key))
                coloresFinales.add(coloresAgrupados[entry.key] ?: Color.GRAY)
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = coloresFinales
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 6f

        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    private fun procesarGraficoIngresos(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChartIngresos)
        val txtTotal = view.findViewById<TextView>(R.id.txtTotalIngresos)
        val layoutDetalle = view.findViewById<LinearLayout>(R.id.layoutDetalleSliceIngresos)

        layoutDetalle.visibility = View.GONE
        fechasIngresosFormat.clear()

        val sdfParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val ingresosFiltrados = listaMovimientosReal.filter { it.tipo == 1 }.filter { mov ->
            try {
                val time = sdfParser.parse(mov.fecha)?.time ?: 0L
                time in filtroInicio..filtroFin
            } catch (e: Exception) {
                true
            }
        }

        val agrupadoPorFecha = HashMap<String, Double>()
        var totalIngresos = 0.0

        for (mov in ingresosFiltrados) {
            val fechaCorta = mov.fecha.take(5)
            agrupadoPorFecha[fechaCorta] = (agrupadoPorFecha[fechaCorta] ?: 0.0) + mov.cantidad
            totalIngresos += mov.cantidad
        }

        val formatCurrency = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        txtTotal.text = formatCurrency.format(totalIngresos)

        if (agrupadoPorFecha.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No hay ingresos registrados en estas fechas.")
            lineChart.invalidate()
            return
        }

        val sdfSort = SimpleDateFormat("dd/MM", Locale.getDefault())
        val listaOrdenada = agrupadoPorFecha.entries.sortedBy {
            try { sdfSort.parse(it.key)?.time ?: 0L } catch(e: Exception){ 0L }
        }

        val entries = ArrayList<Entry>()
        for ((index, entry) in listaOrdenada.withIndex()) {
            entries.add(Entry(index.toFloat(), entry.value.toFloat()))
            fechasIngresosFormat.add(entry.key)
        }

        val dataSet = LineDataSet(entries, "Ingresos")
        dataSet.color = Color.parseColor("#4CAF50")
        dataSet.setCircleColor(Color.parseColor("#4CAF50"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 0f
        dataSet.setDrawValues(false)

        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#A5D6A7")
        dataSet.fillAlpha = 150

        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.xAxis.labelCount = Math.min(fechasIngresosFormat.size, 5)
        lineChart.invalidate()
    }

    private fun mostrarDialogoFiltroMeses(view: View) {
        val opciones = arrayOf("Este Mes", "Mes Anterior", "Todo el Historial", "Rango Personalizado...")
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por Ventana de Tiempo")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> aplicarFiltroMes(view, 0)
                    1 -> aplicarFiltroMes(view, -1)
                    2 -> aplicarFiltroMes(view, null)
                    3 -> abrirSelectorFechas(view)
                }
            }
            .show()
    }

    private fun aplicarFiltroMes(view: View, offset: Int?) {
        if (offset == null) {
            filtroInicio = 0L
            filtroFin = Long.MAX_VALUE
            view.findViewById<TextView>(R.id.txtRangoFechas).text = "Todo el historial"
            view.findViewById<TextView>(R.id.txtRangoFechasIngresos).text = "Todo el historial"
        } else {
            val calInicio = Calendar.getInstance()
            calInicio.add(Calendar.MONTH, offset)
            calInicio.set(Calendar.DAY_OF_MONTH, 1)
            calInicio.set(Calendar.HOUR_OF_DAY, 0)
            calInicio.set(Calendar.MINUTE, 0)
            calInicio.set(Calendar.SECOND, 0)
            calInicio.set(Calendar.MILLISECOND, 0)
            filtroInicio = calInicio.timeInMillis

            val calFin = calInicio.clone() as Calendar
            calFin.set(Calendar.DAY_OF_MONTH, calFin.getActualMaximum(Calendar.DAY_OF_MONTH))
            calFin.set(Calendar.HOUR_OF_DAY, 23)
            calFin.set(Calendar.MINUTE, 59)
            calFin.set(Calendar.SECOND, 59)
            calFin.set(Calendar.MILLISECOND, 999)
            filtroFin = calFin.timeInMillis

            val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val textoRango = sdf.format(calInicio.time).replaceFirstChar { it.uppercase() }
            view.findViewById<TextView>(R.id.txtRangoFechas).text = textoRango
            view.findViewById<TextView>(R.id.txtRangoFechasIngresos).text = textoRango
        }

        procesarGraficoGastos(view)
        procesarGraficoIngresos(view)

        if (isChartExpandedGastos) view.findViewById<PieChart>(R.id.pieChartResumen).animateY(800)
        if (isChartExpandedIngresos) view.findViewById<LineChart>(R.id.lineChartIngresos).animateX(800)
    }

    private fun abrirSelectorFechas(view: View) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Seleccionar periodo")
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            filtroInicio = selection.first
            filtroFin = selection.second

            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            val fechaInicio = sdf.format(Date(filtroInicio))
            val fechaFin = sdf.format(Date(filtroFin))
            val textoRango = "$fechaInicio - $fechaFin"

            view.findViewById<TextView>(R.id.txtRangoFechas).text = textoRango
            view.findViewById<TextView>(R.id.txtRangoFechasIngresos).text = textoRango

            procesarGraficoGastos(view)
            procesarGraficoIngresos(view)

            if (isChartExpandedGastos) view.findViewById<PieChart>(R.id.pieChartResumen).animateY(800)
            if (isChartExpandedIngresos) view.findViewById<LineChart>(R.id.lineChartIngresos).animateX(800)
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
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
            Toast.makeText(requireContext(), "Este evento es del calendario del sistema, bórralo en su app.", Toast.LENGTH_LONG).show()
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