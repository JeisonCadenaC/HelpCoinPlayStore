package com.example.finance_code.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.cardview.widget.CardView
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

    private var userEmail: String = "default"

    // VARIABLES DE ESTADO Y PERSISTENCIA
    private var isChartExpandedGastos = false
    private var isChartExpandedIngresos = false

    private var pTipoGastos: Int = 5 // 5 = Historial
    private var pInicioGastos: Long = 0L
    private var pFinGastos: Long = Long.MAX_VALUE

    private var cTipoGastos: Int = -1 // -1 = Sin comparacion
    private var cInicioGastos: Long = 0L
    private var cFinGastos: Long = Long.MAX_VALUE

    private var pTipoIngresos: Int = 5
    private var pInicioIngresos: Long = 0L
    private var pFinIngresos: Long = Long.MAX_VALUE

    private var cTipoIngresos: Int = -1
    private var cInicioIngresos: Long = 0L
    private var cFinIngresos: Long = Long.MAX_VALUE

    private var fechasIngresosFormat = mutableListOf<String>()

    data class EventoCombinado(val id: String, val titulo: String, val horaMillis: Long, val fuente: String)

    private val requestCalendarPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { actualizarAgendaCombinada() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "default"
        if (userEmail != "default") {
            val database = AppDB.getDatabase(requireContext(), userEmail)
            val repository = MovimientoRepository(database.movimientoDao())
            val factory = MovimientoViewModelFactory(repository)
            movimientoViewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]
        }

        cargarPreferencias()

        setupRecyclerView()
        setupCalendar()

        configurarPieChart(view.findViewById(R.id.pieChartResumen), view)
        configurarPieChart(view.findViewById(R.id.pieChartComparacion), view)

        setupAnalisisGastosLayout(view)
        setupLineChartIngresos(view)
        setupAnalisisIngresosLayout(view)

        restaurarUIGastos(view)
        restaurarUIIngresos(view)

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

        binding.fabNuevoRecordatorio.setOnClickListener { findNavController().navigate(R.id.action_inicioFragment_to_calendarioFragment) }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            actualizarAgendaCombinada()
        } else {
            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE)
        pTipoGastos = prefs.getInt("pTipoGastos", 5)
        pInicioGastos = prefs.getLong("pInicioGastos", 0L)
        pFinGastos = prefs.getLong("pFinGastos", Long.MAX_VALUE)
        cTipoGastos = prefs.getInt("cTipoGastos", -1)
        cInicioGastos = prefs.getLong("cInicioGastos", 0L)
        cFinGastos = prefs.getLong("cFinGastos", Long.MAX_VALUE)

        pTipoIngresos = prefs.getInt("pTipoIngresos", 5)
        pInicioIngresos = prefs.getLong("pInicioIngresos", 0L)
        pFinIngresos = prefs.getLong("pFinIngresos", Long.MAX_VALUE)
        cTipoIngresos = prefs.getInt("cTipoIngresos", -1)
        cInicioIngresos = prefs.getLong("cInicioIngresos", 0L)
        cFinIngresos = prefs.getLong("cFinIngresos", Long.MAX_VALUE)
    }

    private fun guardarPreferencias(tipo: Int) {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE).edit()
        if (tipo == 0) {
            prefs.putInt("pTipoGastos", pTipoGastos)
            prefs.putLong("pInicioGastos", pInicioGastos)
            prefs.putLong("pFinGastos", pFinGastos)
            prefs.putInt("cTipoGastos", cTipoGastos)
            prefs.putLong("cInicioGastos", cInicioGastos)
            prefs.putLong("cFinGastos", cFinGastos)
        } else {
            prefs.putInt("pTipoIngresos", pTipoIngresos)
            prefs.putLong("pInicioIngresos", pInicioIngresos)
            prefs.putLong("pFinIngresos", pFinIngresos)
            prefs.putInt("cTipoIngresos", cTipoIngresos)
            prefs.putLong("cInicioIngresos", cInicioIngresos)
            prefs.putLong("cFinIngresos", cFinIngresos)
        }
        prefs.apply()
    }

    private fun restaurarUIGastos(view: View) {
        val txtRango = obtenerTextoRango(pTipoGastos, pInicioGastos, pFinGastos)
        view.findViewById<TextView>(R.id.txtRangoFechas).text = txtRango
        view.findViewById<MaterialButton>(R.id.btnConfigurarAnalisisGastos).text = "Periodo: $txtRango"
    }

    private fun restaurarUIIngresos(view: View) {
        val txtRango = obtenerTextoRango(pTipoIngresos, pInicioIngresos, pFinIngresos)
        view.findViewById<TextView>(R.id.txtRangoFechasIngresos).text = txtRango
        view.findViewById<MaterialButton>(R.id.btnConfigurarAnalisisIngresos).text = "Periodo: $txtRango"
    }

    private fun setupAnalisisGastosLayout(view: View) {
        val layoutCardHeader = view.findViewById<RelativeLayout>(R.id.layoutCardHeader)
        val layoutColapsable = view.findViewById<LinearLayout>(R.id.layoutColapsable)
        val imgExpandir = view.findViewById<ImageView>(R.id.imgExpandir)
        val btnConfigurar = view.findViewById<MaterialButton>(R.id.btnConfigurarAnalisisGastos)

        view.findViewById<CardView>(R.id.cardTendenciaGastos).visibility = View.GONE

        layoutCardHeader.setOnClickListener {
            isChartExpandedGastos = !isChartExpandedGastos
            if (isChartExpandedGastos) {
                layoutColapsable.visibility = View.VISIBLE
                imgExpandir.animate().rotation(180f).setDuration(250).start()
                view.findViewById<PieChart>(R.id.pieChartResumen).animateY(1000)
                if (cTipoGastos != -1) view.findViewById<PieChart>(R.id.pieChartComparacion).animateY(1000)
            } else {
                layoutColapsable.visibility = View.GONE
                imgExpandir.animate().rotation(0f).setDuration(250).start()
                view.findViewById<LinearLayout>(R.id.layoutDetalleSlice).visibility = View.GONE
            }
        }

        btnConfigurar.setOnClickListener { mostrarBottomSheetAnalisis(view, 0) }
    }

    private fun setupAnalisisIngresosLayout(view: View) {
        val layoutCardHeader = view.findViewById<RelativeLayout>(R.id.layoutCardHeaderIngresos)
        val layoutColapsable = view.findViewById<LinearLayout>(R.id.layoutColapsableIngresos)
        val imgExpandir = view.findViewById<ImageView>(R.id.imgExpandirIngresos)
        val btnConfigurar = view.findViewById<MaterialButton>(R.id.btnConfigurarAnalisisIngresos)

        view.findViewById<CardView>(R.id.cardTendenciaIngresos).visibility = View.GONE

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

        btnConfigurar.setOnClickListener { mostrarBottomSheetAnalisis(view, 1) }
    }

    private fun mapearPeriodoAId(tipo: Int): Int = when(tipo) {
        0 -> R.id.chipEsteMes
        1 -> R.id.chipMesAnterior
        2 -> R.id.chip7Dias
        3 -> R.id.chip30Dias
        4 -> R.id.chipEsteAno
        6 -> R.id.chipPersonalizado
        else -> R.id.chipHistorial
    }

    private fun mapearCompAId(tipo: Int): Int = when(tipo) {
        -2 -> R.id.chipCompAuto
        0 -> R.id.chipCompEsteMes
        1 -> R.id.chipCompMesAnterior
        2 -> R.id.chipComp7Dias
        3 -> R.id.chipComp30Dias
        4 -> R.id.chipCompEsteAno
        5 -> R.id.chipCompHistorial
        6 -> R.id.chipCompPersonalizado
        else -> R.id.chipCompNinguno
    }

    private fun mapearIdATipo(id: Int): Int = when(id) {
        R.id.chipEsteMes, R.id.chipCompEsteMes -> 0
        R.id.chipMesAnterior, R.id.chipCompMesAnterior -> 1
        R.id.chip7Dias, R.id.chipComp7Dias -> 2
        R.id.chip30Dias, R.id.chipComp30Dias -> 3
        R.id.chipEsteAno, R.id.chipCompEsteAno -> 4
        R.id.chipHistorial, R.id.chipCompHistorial -> 5
        R.id.chipPersonalizado, R.id.chipCompPersonalizado -> 6
        R.id.chipCompAuto -> -2
        else -> -1
    }

    private fun mostrarBottomSheetAnalisis(parentView: View, tipoAnalisis: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_filtros, null)
        bottomSheetDialog.setContentView(sheetView)

        val chipGroupPeriodo = sheetView.findViewById<ChipGroup>(R.id.chipGroupPeriodo)
        val chipGroupComparacion = sheetView.findViewById<ChipGroup>(R.id.chipGroupComparacion)
        val btnAplicar = sheetView.findViewById<MaterialButton>(R.id.btnAplicarAnalisis)

        val pTipoActual = if (tipoAnalisis == 0) pTipoGastos else pTipoIngresos
        val cTipoActual = if (tipoAnalisis == 0) cTipoGastos else cTipoIngresos

        sheetView.findViewById<Chip>(mapearPeriodoAId(pTipoActual))?.isChecked = true
        sheetView.findViewById<Chip>(mapearCompAId(cTipoActual))?.isChecked = true

        val actualizarEstiloChips = { group: ChipGroup ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip
                chip?.typeface = if (chip?.isChecked == true) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }

        chipGroupPeriodo.setOnCheckedChangeListener { group, _ -> actualizarEstiloChips(group) }
        chipGroupComparacion.setOnCheckedChangeListener { group, _ -> actualizarEstiloChips(group) }

        actualizarEstiloChips(chipGroupPeriodo)
        actualizarEstiloChips(chipGroupComparacion)

        btnAplicar.setOnClickListener {
            val nuevoPTipo = mapearIdATipo(chipGroupPeriodo.checkedChipId)
            val nuevoCTipo = mapearIdATipo(chipGroupComparacion.checkedChipId)

            if (nuevoPTipo == 6) {
                abrirSelectorFechas(parentView, tipoAnalisis, isComp = false) { iniP, finP ->
                    if (nuevoCTipo == 6) {
                        abrirSelectorFechas(parentView, tipoAnalisis, isComp = true) { iniC, finC ->
                            aplicarSeleccion(parentView, tipoAnalisis, nuevoPTipo, iniP, finP, nuevoCTipo, iniC, finC)
                        }
                    } else {
                        val (iniC, finC) = calcularFechasAbsolutas(nuevoCTipo)
                        aplicarSeleccion(parentView, tipoAnalisis, nuevoPTipo, iniP, finP, nuevoCTipo, iniC, finC)
                    }
                }
            } else if (nuevoCTipo == 6) {
                val (iniP, finP) = calcularFechasAbsolutas(nuevoPTipo)
                abrirSelectorFechas(parentView, tipoAnalisis, isComp = true) { iniC, finC ->
                    aplicarSeleccion(parentView, tipoAnalisis, nuevoPTipo, iniP, finP, nuevoCTipo, iniC, finC)
                }
            } else {
                val (iniP, finP) = calcularFechasAbsolutas(nuevoPTipo)
                val (iniC, finC) = calcularFechasAbsolutas(nuevoCTipo)
                aplicarSeleccion(parentView, tipoAnalisis, nuevoPTipo, iniP, finP, nuevoCTipo, iniC, finC)
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun aplicarSeleccion(view: View, tipoAnalisis: Int, pTipo: Int, pIni: Long, pFin: Long, cTipo: Int, cIni: Long, cFin: Long) {
        if (tipoAnalisis == 0) {
            pTipoGastos = pTipo; pInicioGastos = pIni; pFinGastos = pFin
            cTipoGastos = cTipo; cInicioGastos = cIni; cFinGastos = cFin
            guardarPreferencias(0)
            restaurarUIGastos(view)
            procesarGraficoGastos(view)
            if (isChartExpandedGastos) {
                view.findViewById<PieChart>(R.id.pieChartResumen).animateY(800)
                if (cTipoGastos != -1) view.findViewById<PieChart>(R.id.pieChartComparacion).animateY(800)
            }
        } else {
            pTipoIngresos = pTipo; pInicioIngresos = pIni; pFinIngresos = pFin
            cTipoIngresos = cTipo; cInicioIngresos = cIni; cFinIngresos = cFin
            guardarPreferencias(1)
            restaurarUIIngresos(view)
            procesarGraficoIngresos(view)
            if (isChartExpandedIngresos) view.findViewById<LineChart>(R.id.lineChartIngresos).animateX(800)
        }
    }

    private fun obtenerTextoRango(tipo: Int, ini: Long, fin: Long): String {
        return when (tipo) {
            0 -> "Este Mes"
            1 -> "Mes Anterior"
            2 -> "Últimos 7 Días"
            3 -> "Últimos 30 Días"
            4 -> "Este Año"
            5 -> "Todo el Historial"
            6 -> {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                "${sdf.format(Date(ini))} - ${sdf.format(Date(fin))}"
            }
            else -> "Periodo"
        }
    }

    private fun calcularFechasAbsolutas(rangoTipo: Int): Pair<Long, Long> {
        if (rangoTipo == 5 || rangoTipo < 0) return Pair(0L, Long.MAX_VALUE)
        var inicio = 0L
        var fin = Long.MAX_VALUE
        val cal = Calendar.getInstance()

        when (rangoTipo) {
            0 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fin = maximizeTime(cal).timeInMillis
            }
            1 -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fin = maximizeTime(cal).timeInMillis
            }
            2 -> {
                fin = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                inicio = resetTime(cal).timeInMillis
            }
            3 -> {
                fin = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -30)
                inicio = resetTime(cal).timeInMillis
            }
            4 -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal).timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                fin = maximizeTime(cal).timeInMillis
            }
        }
        return Pair(inicio, fin)
    }

    private fun abrirSelectorFechas(view: View, tipoAnalisis: Int, isComp: Boolean, onSeleccion: (Long, Long) -> Unit) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText(if (isComp) "Seleccionar Periodo de Comparación" else "Seleccionar Periodo de Análisis")
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            onSeleccion(selection.first, selection.second)
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun resetTime(cal: Calendar): Calendar {
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0); return cal
    }

    private fun maximizeTime(cal: Calendar): Calendar {
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999); return cal
    }

    private fun obtenerFechasComparacionAuto(pTipo: Int, pIni: Long, pFin: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (pTipo) {
            0 -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val i = resetTime(cal.clone() as Calendar).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                Pair(i, maximizeTime(cal).timeInMillis)
            }
            1 -> {
                cal.add(Calendar.MONTH, -2)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val i = resetTime(cal.clone() as Calendar).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                Pair(i, maximizeTime(cal).timeInMillis)
            }
            2 -> Pair(pIni - 7L * 24 * 60 * 60 * 1000, pIni - 1)
            3 -> Pair(pIni - 30L * 24 * 60 * 60 * 1000, pIni - 1)
            4 -> {
                cal.add(Calendar.YEAR, -1)
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val i = resetTime(cal.clone() as Calendar).timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                Pair(i, maximizeTime(cal).timeInMillis)
            }
            6 -> {
                val duracion = pFin - pIni
                Pair(pIni - duracion - 1, pIni - 1)
            }
            else -> Pair(0L, 0L)
        }
    }

    private fun obtenerNombreModo(cTipo: Int, pTipo: Int): String {
        return when (cTipo) {
            0 -> "vs Este Mes"
            1 -> "vs Mes Anterior"
            2 -> "vs 7 Días"
            3 -> "vs 30 Días"
            4 -> "vs Este Año"
            5 -> "vs Historial"
            6 -> "vs Rango Personalizado"
            -2 -> {
                when(pTipo) {
                    0 -> "vs Mes Anterior"
                    1 -> "vs Hace 2 Meses"
                    2 -> "vs 7 Días Previos"
                    3 -> "vs 30 Días Previos"
                    4 -> "vs Año Anterior"
                    6 -> "vs Periodo Previo"
                    else -> "vs Anterior"
                }
            }
            else -> ""
        }
    }

    private fun actualizarUITendencia(view: View, tipoAnalisis: Int, actual: Double, anterior: Double, textoModo: String) {
        val cardTendencia = if (tipoAnalisis == 0) view.findViewById<CardView>(R.id.cardTendenciaGastos) else view.findViewById<CardView>(R.id.cardTendenciaIngresos)
        val txtTendencia = if (tipoAnalisis == 0) view.findViewById<TextView>(R.id.txtTendenciaGastos) else view.findViewById<TextView>(R.id.txtTendenciaIngresos)
        val txtModo = if (tipoAnalisis == 0) view.findViewById<TextView>(R.id.txtTendenciaModoGastos) else view.findViewById<TextView>(R.id.txtTendenciaModoIngresos)
        val imgTendencia = if (tipoAnalisis == 0) view.findViewById<ImageView>(R.id.imgTendenciaGastos) else view.findViewById<ImageView>(R.id.imgTendenciaIngresos)

        cardTendencia.visibility = View.VISIBLE
        txtModo.text = " $textoModo"

        if (anterior > 0) {
            val porcentaje = ((actual - anterior) / anterior) * 100
            txtTendencia.text = String.format(Locale.getDefault(), "%.1f%%", Math.abs(porcentaje))
            imgTendencia.visibility = View.VISIBLE

            val aumento = actual >= anterior
            val colorPositivo = Color.parseColor(if (tipoAnalisis == 0) "#F44336" else "#4CAF50")
            val colorNegativo = Color.parseColor(if (tipoAnalisis == 0) "#4CAF50" else "#F44336")

            if (aumento) {
                txtTendencia.setTextColor(colorPositivo)
                imgTendencia.setColorFilter(colorPositivo)
                imgTendencia.setImageResource(R.drawable.ic_arrow_up)
            } else {
                txtTendencia.setTextColor(colorNegativo)
                imgTendencia.setColorFilter(colorNegativo)
                imgTendencia.setImageResource(R.drawable.ic_arrow_down)
            }
        } else {
            txtTendencia.text = "Sin datos previos"
            txtTendencia.setTextColor(Color.GRAY)
            imgTendencia.visibility = View.GONE
        }
    }

    private fun configurarPieChart(pieChart: PieChart, view: View) {
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
        legend.textSize = 10f
        legend.form = Legend.LegendForm.CIRCLE
        legend.formSize = 8f
        legend.xEntrySpace = 8f
        legend.yEntrySpace = 4f
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
        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)

        val legend = lineChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textColor = ContextCompat.getColor(requireContext(), android.R.color.tab_indicator_text)

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
                return if (value >= 1000000) String.format(Locale.getDefault(), "%.1fM", value / 1000000)
                else if (value >= 1000) String.format(Locale.getDefault(), "%.0fk", value / 1000)
                else value.toInt().toString()
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
                } else {
                    txtFecha.text = "Dato Anterior"
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
            textoInfo.matches(Regex(".*(comida|restaurante|almuerzo|cena|desayuno|pizza|hamburguesa|perro|empanada|helado|postre|snack|tinto).*")) -> Pair("🍔 Comida", Color.parseColor("#FF9800"))
            textoInfo.matches(Regex(".*(gasolina|moto|carro|repuestos|arreglo|mecanico|parqueadero|peaje|llanta|aceite|taller|vehiculo|soat|tecnomecanica).*")) -> Pair("🚗 Vehículo", Color.parseColor("#607D8B"))
            textoInfo.matches(Regex(".*(transporte|pasaje|bus|transmilenio|taxi|uber|didi|cabify|metro|picap).*")) -> Pair("🚌 Transporte", Color.parseColor("#03A9F4"))
            textoInfo.matches(Regex(".*(servicio|luz|agua|internet|recibo|gas|telefono|celular|plan|wifi|factura|arriendo|alquiler).*")) -> Pair("💡 Servicios y Recibos", Color.parseColor("#FFC107"))
            textoInfo.matches(Regex(".*(supermercado|mercado|despensa|viveres|tienda|d1|ara|exito|jumbo|olimpica|carulla|abastos).*")) -> Pair("🛒 Mercado", Color.parseColor("#4CAF50"))
            textoInfo.matches(Regex(".*(maquillaje|peluqueria|uñas|barbero|cuidado|crema|aseo|skincare|corte|perfume).*")) -> Pair("💅 Cuidado Personal", Color.parseColor("#E91E63"))
            textoInfo.matches(Regex(".*(salud|medicina|farmacia|medico|pastillas|hospital|eps|cita|droga|drogueria|examen).*")) -> Pair("💊 Salud", Color.parseColor("#F44336"))
            textoInfo.matches(Regex(".*(ropa|compras|zapatos|tenis|blusa|pantalon|chaqueta|centro comercial|mall|regalo|accesorio).*")) -> Pair("🛍️ Compras", Color.parseColor("#9C27B0"))
            textoInfo.matches(Regex(".*(educacion|estudio|universidad|colegio|cuaderno|libro|curso|matricula|pension|semestre|diplomado).*")) -> Pair("📚 Educación", Color.parseColor("#00BCD4"))
            textoInfo.matches(Regex(".*(viaje|hotel|vuelo|avion|vacaciones|turismo|paseo|hospedaje|airbnb|terminal).*")) -> Pair("✈️ Viajes", Color.parseColor("#3F51B5"))
            textoInfo.matches(Regex(".*(prostituta|puta|prepago|onlyfans|webcam|motel|cariñosa|chica|acompañante).*")) -> Pair("🔞 Ocio Nocturno", Color.parseColor("#B71C1C"))
            textoInfo.matches(Regex(".*(ocio|diversion|cine|rumba|fiesta|trago|cerveza|pola|licor|bar|netflix|spotify|suscripcion|videojuego|juego|xbox|play|suscripción).*")) -> Pair("🎉 Diversión", Color.parseColor("#CDDC39"))
            else -> Pair("🏷️ Otros Gastos", Color.parseColor("#795548"))
        }
    }

    private fun rellenarPieChart(pieChart: PieChart, movimientos: List<Movimiento>) {
        val agrupado = HashMap<String, Double>()
        val coloresAgrupados = HashMap<String, Int>()

        for (mov in movimientos) {
            val (nombreCategoria, colorAsignado) = obtenerCategoriaGastoInteligente(mov)
            agrupado[nombreCategoria] = (agrupado[nombreCategoria] ?: 0.0) + mov.cantidad
            coloresAgrupados[nombreCategoria] = colorAsignado
        }

        if (agrupado.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No hay datos en este periodo")
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

        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun procesarGraficoGastos(view: View) {
        val pieChartActual = view.findViewById<PieChart>(R.id.pieChartResumen)
        val pieChartComp = view.findViewById<PieChart>(R.id.pieChartComparacion)
        val layoutComp = view.findViewById<LinearLayout>(R.id.layoutPieComparacion)
        val txtActual = view.findViewById<TextView>(R.id.txtLabelActualGastos)
        val txtTotal = view.findViewById<TextView>(R.id.txtTotalGastos)
        val cardTendencia = view.findViewById<CardView>(R.id.cardTendenciaGastos)

        view.findViewById<LinearLayout>(R.id.layoutDetalleSlice).visibility = View.GONE
        val sdfParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val gastosActuales = listaMovimientosReal.filter { it.tipo == 0 }.filter { mov ->
            try { val time = sdfParser.parse(mov.fecha)?.time ?: 0L; time in pInicioGastos..pFinGastos } catch (e: Exception) { true }
        }

        val totalGastos = gastosActuales.sumOf { it.cantidad }
        val formatCurrency = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        txtTotal.text = formatCurrency.format(totalGastos)

        rellenarPieChart(pieChartActual, gastosActuales)

        if (cTipoGastos != -1) {
            layoutComp.visibility = View.VISIBLE
            txtActual.visibility = View.VISIBLE

            val (cIni, cFin) = if (cTipoGastos == -2) obtenerFechasComparacionAuto(pTipoGastos, pInicioGastos, pFinGastos) else Pair(cInicioGastos, cFinGastos)

            val gastosAnt = listaMovimientosReal.filter { it.tipo == 0 }.filter { mov ->
                try { val time = sdfParser.parse(mov.fecha)?.time ?: 0L; time in cIni..cFin } catch (e: Exception) { true }
            }

            rellenarPieChart(pieChartComp, gastosAnt)
            val totalAnt = gastosAnt.sumOf { it.cantidad }
            actualizarUITendencia(view, 0, totalGastos, totalAnt, obtenerNombreModo(cTipoGastos, pTipoGastos))
        } else {
            layoutComp.visibility = View.GONE
            txtActual.visibility = View.GONE
            cardTendencia.visibility = View.GONE
        }
    }

    private fun procesarGraficoIngresos(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChartIngresos)
        val txtTotal = view.findViewById<TextView>(R.id.txtTotalIngresos)
        val cardTendencia = view.findViewById<CardView>(R.id.cardTendenciaIngresos)

        view.findViewById<LinearLayout>(R.id.layoutDetalleSliceIngresos).visibility = View.GONE
        fechasIngresosFormat.clear()
        lineChart.clear()

        val sdfParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val ingresosBase = listaMovimientosReal.filter { it.tipo == 1 }

        val ingresosActuales = ingresosBase.filter { mov ->
            try { val time = sdfParser.parse(mov.fecha)?.time ?: 0L; time in pInicioIngresos..pFinIngresos } catch (e: Exception) { true }
        }

        val agrupadoActual = HashMap<String, Double>()
        for (mov in ingresosActuales) {
            val fechaCorta = mov.fecha.take(5)
            agrupadoActual[fechaCorta] = (agrupadoActual[fechaCorta] ?: 0.0) + mov.cantidad
        }

        val totalIngresos = ingresosActuales.sumOf { it.cantidad }
        val formatCurrency = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        txtTotal.text = formatCurrency.format(totalIngresos)

        if (agrupadoActual.isEmpty()) {
            lineChart.setNoDataText("No hay datos en este periodo")
            lineChart.invalidate()
        }

        val sdfSort = SimpleDateFormat("dd/MM", Locale.getDefault())
        val listaOrdenadaAct = agrupadoActual.entries.sortedBy { try { sdfSort.parse(it.key)?.time ?: 0L } catch(e: Exception){ 0L } }

        val entriesAct = ArrayList<Entry>()
        for ((index, entry) in listaOrdenadaAct.withIndex()) {
            entriesAct.add(Entry(index.toFloat(), entry.value.toFloat()))
            fechasIngresosFormat.add(entry.key)
        }

        val dataSetAct = LineDataSet(entriesAct, "Actual")
        dataSetAct.color = Color.parseColor("#4CAF50")
        dataSetAct.setCircleColor(Color.parseColor("#4CAF50"))
        dataSetAct.lineWidth = 3f
        dataSetAct.circleRadius = 5f
        dataSetAct.setDrawCircleHole(true)
        dataSetAct.valueTextSize = 0f
        dataSetAct.setDrawValues(false)
        dataSetAct.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSetAct.setDrawFilled(true)
        dataSetAct.fillColor = Color.parseColor("#A5D6A7")
        dataSetAct.fillAlpha = 150

        val lineData = LineData()
        if (entriesAct.isNotEmpty()) lineData.addDataSet(dataSetAct)

        if (cTipoIngresos != -1) {
            val (cIni, cFin) = if (cTipoIngresos == -2) obtenerFechasComparacionAuto(pTipoIngresos, pInicioIngresos, pFinIngresos) else Pair(cInicioIngresos, cFinIngresos)

            val ingresosAnt = ingresosBase.filter { mov ->
                try { val time = sdfParser.parse(mov.fecha)?.time ?: 0L; time in cIni..cFin } catch (e: Exception) { true }
            }

            val agrupadoAnt = HashMap<String, Double>()
            for (mov in ingresosAnt) {
                val fechaCorta = mov.fecha.take(5)
                agrupadoAnt[fechaCorta] = (agrupadoAnt[fechaCorta] ?: 0.0) + mov.cantidad
            }

            val listaOrdenadaAnt = agrupadoAnt.entries.sortedBy { try { sdfSort.parse(it.key)?.time ?: 0L } catch(e: Exception){ 0L } }
            val entriesAnt = ArrayList<Entry>()
            for ((index, entry) in listaOrdenadaAnt.withIndex()) {
                entriesAnt.add(Entry(index.toFloat(), entry.value.toFloat()))
            }

            if (entriesAnt.isNotEmpty()) {
                val dataSetAnt = LineDataSet(entriesAnt, obtenerNombreModo(cTipoIngresos, pTipoIngresos))
                dataSetAnt.color = Color.parseColor("#9E9E9E")
                dataSetAnt.setCircleColor(Color.parseColor("#9E9E9E"))
                dataSetAnt.lineWidth = 2f
                dataSetAnt.circleRadius = 4f
                dataSetAnt.enableDashedLine(10f, 10f, 0f)
                dataSetAnt.valueTextSize = 0f
                dataSetAnt.setDrawValues(false)
                dataSetAnt.mode = LineDataSet.Mode.CUBIC_BEZIER
                dataSetAnt.setDrawFilled(false)
                lineData.addDataSet(dataSetAnt)
            }

            val totalAnt = ingresosAnt.sumOf { it.cantidad }
            actualizarUITendencia(view, 1, totalIngresos, totalAnt, obtenerNombreModo(cTipoIngresos, pTipoIngresos))
        } else {
            cardTendencia.visibility = View.GONE
        }

        if (lineData.dataSetCount > 0) {
            lineChart.data = lineData
            lineChart.xAxis.labelCount = Math.min(fechasIngresosFormat.size, 5)
            lineChart.invalidate()
        }
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaAdapter(emptyList()) { evento -> confirmarEliminacion(evento) }
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
        val filtradosApp = listaRecordatoriosApp.filter { it.fechaMillis in startMillis..endMillis }.map {
            EventoCombinado(it.id.toString(), it.nombre, it.fechaMillis, "Finanzas App")
        }
        eventosDelDia.addAll(filtradosApp)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            eventosDelDia.addAll(obtenerEventosSistema(startMillis, endMillis))
        }

        val listaOrdenada = eventosDelDia.sortedBy { it.horaMillis }
        agendaAdapter.actualizarLista(listaOrdenada)

        if (listaOrdenada.isEmpty()) binding.txtTituloAgenda.text = "No hay eventos para este día"
        else binding.txtTituloAgenda.text = "Agenda del día"
    }

    private fun obtenerEventosSistema(startMillis: Long, endMillis: Long): List<EventoCombinado> {
        val lista = mutableListOf<EventoCombinado>()
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.CALENDAR_DISPLAY_NAME)
        val selection = "(( ${CalendarContract.Events.DTSTART} >= ?) AND ( ${CalendarContract.Events.DTSTART} <= ?))"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        try {
            val cursor: Cursor? = requireContext().contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")
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
        return resetTime(c).timeInMillis
    }

    private fun obtenerFinDia(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        return maximizeTime(c).timeInMillis
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

    class AgendaAdapter(private var eventos: List<EventoCombinado>, private val onDeleteClick: (EventoCombinado) -> Unit) : RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>() {

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
            holder.btnEliminar.visibility = if (esDeMiApp && evento.id == idItemSeleccionado) View.VISIBLE else View.GONE

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
        fun actualizarLista(nuevosEventos: List<EventoCombinado>) { eventos = nuevosEventos; notifyDataSetChanged() }
    }
}