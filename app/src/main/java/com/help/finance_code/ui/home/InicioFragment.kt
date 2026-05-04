package com.help.finance_code.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.help.finance_code.R
import com.help.finance_code.data.AppDB
import com.help.finance_code.data.Categoria
import com.help.finance_code.data.Movimiento
import com.help.finance_code.data.MovimientoRepository
import com.help.finance_code.data.Recordatorio
import com.help.finance_code.databinding.FragmentInicioBinding
import com.help.finance_code.viewmodel.MovimientoViewModel
import com.help.finance_code.viewmodel.MovimientoViewModelFactory
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
import kotlinx.coroutines.launch
import java.io.File
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
    private lateinit var database: AppDB

    private lateinit var agendaAdapter: AgendaAdapter
    private var selectedDate: Calendar = Calendar.getInstance()

    private var listaRecordatoriosApp: List<Recordatorio> = emptyList()
    private var listaMovimientosReal: List<Movimiento> = emptyList()
    private var listaCategoriasGlobal: List<Categoria> = emptyList()

    private var userEmail: String = "default"

    private var isChartExpandedGastos = false
    private var isChartExpandedIngresos = false

    private var pTipoGastos: Int = 5
    private var pInicioGastos: Long = 0L
    private var pFinGastos: Long = Long.MAX_VALUE

    private var cTipoGastos: Int = -1
    private var cInicioGastos: Long = 0L
    private var cFinGastos: Long = Long.MAX_VALUE

    private var pTipoIngresos: Int = 5
    private var pInicioIngresos: Long = 0L
    private var pFinIngresos: Long = Long.MAX_VALUE

    private var cTipoIngresos: Int = -1
    private var cInicioIngresos: Long = 0L
    private var cFinIngresos: Long = Long.MAX_VALUE

    private var fechasIngresosFormat = mutableListOf<String>()


    data class EventoCombinado(
        val id: String,
        val titulo: String,
        val horaMillis: Long,
        val fuente: String
    )

    private val requestCalendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { actualizarAgendaCombinada() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        userEmail = currentUser?.email ?: "default"
        val uid = currentUser?.uid

        var primerNombre = ""

        if (uid != null) {
            val prefs = requireContext().getSharedPreferences("${uid}_UserProfilePrefs", Context.MODE_PRIVATE)
            val nombreGuardado = prefs.getString("user_name", "") ?: ""
            if (nombreGuardado.isNotBlank() && nombreGuardado.lowercase() != "usuario") {
                primerNombre = nombreGuardado.trim().split("\\s+".toRegex()).first()
            }
        }

        if (primerNombre.isBlank() && !currentUser?.displayName.isNullOrBlank()) {
            primerNombre = currentUser?.displayName!!.trim().split("\\s+".toRegex()).first()
        }

        if (primerNombre.isNotBlank()) {
            binding.txtHolaNombre.text = "Hola $primerNombre"
        } else {
            binding.txtHolaNombre.text = "¡Hola, Bienvenido!"
        }

        // 🛑 LÓGICA DE FOTO Y NAVEGACIÓN A PERFIL DISCRETO 🛑
        val btnIrPerfil = view.findViewById<ImageView>(R.id.btnIrPerfil)
        if (uid != null) {
            val prefs = requireContext().getSharedPreferences("${uid}_UserProfilePrefs", Context.MODE_PRIVATE)
            val rutaImagenGuardada = prefs.getString("profile_image_path", null)
            val googlePhotoUrl = prefs.getString("google_photo_url", null)

            if (rutaImagenGuardada != null && File(rutaImagenGuardada).exists()) {
                btnIrPerfil.setPadding(0, 0, 0, 0)
                Glide.with(this).load(File(rutaImagenGuardada)).centerCrop().into(btnIrPerfil)
            } else if (googlePhotoUrl != null) {
                btnIrPerfil.setPadding(0, 0, 0, 0)
                Glide.with(this).load(googlePhotoUrl).centerCrop().into(btnIrPerfil)
            } else {
                btnIrPerfil.setPadding(16, 16, 16, 16)
            }
        }
        btnIrPerfil.setOnClickListener {
            findNavController().navigate(R.id.perfilFragment)
        }
        // 🛑 FIN DE LA LÓGICA DEL BOTÓN 🛑

        if (userEmail != "default") {
            database = AppDB.getDatabase(requireContext(), userEmail)
            val repository = MovimientoRepository(database.movimientoDao())
            val factory = MovimientoViewModelFactory(repository)
            movimientoViewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

            viewLifecycleOwner.lifecycleScope.launch {
                database.categoriaDao().obtenerTodas().collect { categorias ->
                    listaCategoriasGlobal = categorias
                    if (listaMovimientosReal.isNotEmpty() && isAdded) {
                        procesarGraficoGastos(view)
                        procesarGraficoIngresos(view)
                    }
                }
            }
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

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            actualizarAgendaCombinada()
        } else {
            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        // Lanzar el tutorial interactivo con un ligero retraso
        view.postDelayed({
            mostrarTutorialDetallado(view)
        }, 500)
    }

    private fun mostrarTutorialDetallado(view: View) {
        val prefs = requireContext().getSharedPreferences("HelpCoinPrefs", Context.MODE_PRIVATE)
        // Cambia a 'false' si quieres probarlo cada vez que entres mientras desarrollas
        val tutorialCompletado = prefs.getBoolean("tutorial_inicio_completo", false)

        if (tutorialCompletado) return

        // Buscamos todas las vistas clave de tu fragment_inicio.xml y sus includes
        val btnPerfil = view.findViewById<View>(R.id.btnIrPerfil)
        val cardGastos = view.findViewById<View>(R.id.layoutCardHeader) // Del include de gastos
        val cardIngresos = view.findViewById<View>(R.id.layoutCardHeaderIngresos) // Del include de ingresos
        val calendario = view.findViewById<View>(R.id.calendarViewInicio)
        val fabRecordatorio = view.findViewById<View>(R.id.fabNuevoRecordatorio)

        // Evitamos crasheos si alguna vista no ha cargado
        if (btnPerfil == null || cardGastos == null || cardIngresos == null || calendario == null || fabRecordatorio == null) return

        // Color principal para los círculos (Aura de HelpCoin)
        val colorPrimario = R.color.colorPrimary
        val colorBlanco = android.R.color.white

        TapTargetSequence(requireActivity())
            .targets(
                // --- PASO 1: PERFIL ---
                TapTarget.forView(
                    btnPerfil,
                    "Tu Perfil y Ajustes",
                    "Aquí puedes personalizar tu cuenta, cambiar tu foto, activar el modo discreto y gestionar tus copias de seguridad.\n\n(Toca el círculo iluminado para continuar)"
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .titleTextSize(22)
                    .titleTextColor(colorBlanco)
                    .descriptionTextSize(16)
                    .descriptionTextColor(colorBlanco)
                    .cancelable(true) // Permite omitir el tutorial si tocan la zona oscura
                    .transparentTarget(true)
                    .targetRadius(40),

                // --- PASO 2: GASTOS ---
                TapTarget.forView(
                    cardGastos,
                    "Análisis de Gastos",
                    "Toca esta tarjeta para expandir un gráfico detallado de tus gastos. ¡Usa el botón de configuración para filtrar por fechas y comparar meses!"
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(60),

                // --- PASO 3: INGRESOS ---
                TapTarget.forView(
                    cardIngresos,
                    "Control de Ingresos",
                    "Igual que con los gastos, aquí puedes visualizar una gráfica de tendencia de tu dinero entrante. ¡Mantén tus finanzas en verde!"
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(60),

                // --- PASO 4: CALENDARIO ---
                TapTarget.forView(
                    calendario,
                    "Tu Agenda Financiera",
                    "Selecciona cualquier día en el calendario para ver los eventos, pagos o recordatorios programados para esa fecha específica."
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(80), // Radio más grande porque el calendario es ancho

                // --- PASO 5: FAB (RECORDATORIOS) ---
                TapTarget.forView(
                    fabRecordatorio,
                    "Añadir Recordatorios",
                    "¿Tienes un pago pendiente o una suscripción por vencer? Toca aquí para agregar un nuevo recordatorio a tu agenda.\n\n¡Eso es todo! Estás listo para usar HelpCoin."
                )
                    .outerCircleColor(colorPrimario)
                    .targetCircleColor(colorBlanco)
                    .cancelable(true)
                    .transparentTarget(true)
                    .targetRadius(40)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    // Se completó todo el tour
                    prefs.edit().putBoolean("tutorial_inicio_completo", true).apply()
                    Toast.makeText(requireContext(), "¡Tutorial completado! A dominar esas finanzas 🚀", Toast.LENGTH_LONG).show()
                }

                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                    // Aquí podrías hacer scroll automático si la pantalla es muy larga
                }

                override fun onSequenceCanceled(lastTarget: TapTarget?) {
                    // El usuario tocó la zona oscura para saltarse el tutorial
                    prefs.edit().putBoolean("tutorial_inicio_completo", true).apply()
                    Toast.makeText(requireContext(), "Tutorial omitido", Toast.LENGTH_SHORT).show()
                }
            })
            .start()
    }

    private fun getSafeLong(prefs: SharedPreferences, key: String, defaultVal: Long): Long {
        return try {
            prefs.getLong(key, defaultVal)
        } catch (e: ClassCastException) {
            try {
                prefs.getInt(key, defaultVal.toInt()).toLong()
            } catch (e2: Exception) {
                defaultVal
            }
        }
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE)

        pTipoGastos = prefs.getInt("pTipoGastos", 5)
        if (pTipoGastos != 6 && pTipoGastos != 5) {
            val pBounds = calcularFechasAbsolutas(pTipoGastos)
            pInicioGastos = pBounds.first
            pFinGastos = pBounds.second
        } else {
            pInicioGastos = getSafeLong(prefs, "pInicioGastos", 0L)
            pFinGastos = getSafeLong(prefs, "pFinGastos", Long.MAX_VALUE)
        }

        cTipoGastos = prefs.getInt("cTipoGastos", -1)
        if (cTipoGastos != 6 && cTipoGastos != 5 && cTipoGastos != -2 && cTipoGastos != -1) {
            val cBounds = calcularFechasAbsolutas(cTipoGastos)
            cInicioGastos = cBounds.first
            cFinGastos = cBounds.second
        } else {
            cInicioGastos = getSafeLong(prefs, "cInicioGastos", 0L)
            cFinGastos = getSafeLong(prefs, "cFinGastos", Long.MAX_VALUE)
        }

        pTipoIngresos = prefs.getInt("pTipoIngresos", 5)
        if (pTipoIngresos != 6 && pTipoIngresos != 5) {
            val pBoundsI = calcularFechasAbsolutas(pTipoIngresos)
            pInicioIngresos = pBoundsI.first
            pFinIngresos = pBoundsI.second
        } else {
            pInicioIngresos = getSafeLong(prefs, "pInicioIngresos", 0L)
            pFinIngresos = getSafeLong(prefs, "pFinIngresos", Long.MAX_VALUE)
        }

        cTipoIngresos = prefs.getInt("cTipoIngresos", -1)
        if (cTipoIngresos != 6 && cTipoIngresos != 5 && cTipoIngresos != -2 && cTipoIngresos != -1) {
            val cBoundsI = calcularFechasAbsolutas(cTipoIngresos)
            cInicioIngresos = cBoundsI.first
            cFinIngresos = cBoundsI.second
        } else {
            cInicioIngresos = getSafeLong(prefs, "cInicioIngresos", 0L)
            cFinIngresos = getSafeLong(prefs, "cFinIngresos", Long.MAX_VALUE)
        }
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

    private fun mapearPeriodoAId(tipo: Int): Int = when (tipo) {
        0 -> R.id.chipEsteMes
        1 -> R.id.chipMesAnterior
        2 -> R.id.chip7Dias
        3 -> R.id.chip30Dias
        4 -> R.id.chipEsteAno
        6 -> R.id.chipPersonalizado
        7 -> R.id.chipHoy
        else -> R.id.chipHistorial
    }

    private fun mapearCompAId(tipo: Int): Int = when (tipo) {
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

    private fun mapearIdATipo(id: Int): Int = when (id) {
        R.id.chipHoy -> 7
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

        try {
            sheetView.findViewById<View>(R.id.divisorAgrupacion)?.visibility = View.GONE
            sheetView.findViewById<TextView>(R.id.tvTituloAgrupar)?.visibility = View.GONE
            val chipGroupAgrupar = sheetView.findViewById<ChipGroup>(R.id.chipGroupAgrupacion)
            chipGroupAgrupar?.visibility = View.GONE
            (chipGroupAgrupar?.parent as? View)?.visibility = View.GONE
        } catch (e: Exception) {}

        val chipGroupPeriodo = sheetView.findViewById<ChipGroup>(R.id.chipGroupPeriodo)
        val chipGroupComparacion = sheetView.findViewById<ChipGroup>(R.id.chipGroupComparacion)
        val btnAplicar = sheetView.findViewById<MaterialButton>(R.id.btnAplicarAnalisis)

        val pTipoActual = if (tipoAnalisis == 0) pTipoGastos else pTipoIngresos
        val cTipoActual = if (tipoAnalisis == 0) cTipoGastos else cTipoIngresos

        var tempPIni = if (tipoAnalisis == 0) pInicioGastos else pInicioIngresos
        var tempPFin = if (tipoAnalisis == 0) pFinGastos else pFinIngresos
        var tempCIni = if (tipoAnalisis == 0) cInicioGastos else cInicioIngresos
        var tempCFin = if (tipoAnalisis == 0) cFinGastos else cFinIngresos

        val actualizarEstiloChips = { group: ChipGroup ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip
                chip?.typeface = if (chip?.isChecked == true) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }

        val chipPersonalizado = sheetView.findViewById<Chip>(R.id.chipPersonalizado)
        val chipCompPersonalizado = sheetView.findViewById<Chip>(R.id.chipCompPersonalizado)

        if (pTipoActual == 6) chipPersonalizado.text = obtenerTextoRango(6, tempPIni, tempPFin)
        if (cTipoActual == 6) chipCompPersonalizado.text = "vs " + obtenerTextoRango(6, tempCIni, tempCFin)

        chipPersonalizado.setOnClickListener {
            abrirSelectorFechas(parentView, tipoAnalisis, isComp = false) { ini, fin ->
                tempPIni = ini
                tempPFin = fin
                chipPersonalizado.text = obtenerTextoRango(6, ini, fin)
                chipPersonalizado.isChecked = true
                actualizarEstiloChips(chipGroupPeriodo)
            }
        }

        chipCompPersonalizado.setOnClickListener {
            abrirSelectorFechas(parentView, tipoAnalisis, isComp = true) { ini, fin ->
                tempCIni = ini
                tempCFin = fin
                chipCompPersonalizado.text = "vs " + obtenerTextoRango(6, ini, fin)
                chipCompPersonalizado.isChecked = true
                actualizarEstiloChips(chipGroupComparacion)
            }
        }

        sheetView.findViewById<Chip>(mapearPeriodoAId(pTipoActual))?.isChecked = true
        sheetView.findViewById<Chip>(mapearCompAId(cTipoActual))?.isChecked = true

        chipGroupPeriodo.setOnCheckedChangeListener { group, _ -> actualizarEstiloChips(group) }
        chipGroupComparacion.setOnCheckedChangeListener { group, _ -> actualizarEstiloChips(group) }

        actualizarEstiloChips(chipGroupPeriodo)
        actualizarEstiloChips(chipGroupComparacion)

        btnAplicar.setOnClickListener {
            val nuevoPTipo = mapearIdATipo(chipGroupPeriodo.checkedChipId)
            val nuevoCTipo = mapearIdATipo(chipGroupComparacion.checkedChipId)

            val (iniP, finP) = if (nuevoPTipo == 6) Pair(tempPIni, tempPFin) else calcularFechasAbsolutas(nuevoPTipo)
            val (iniC, finC) = if (nuevoCTipo == 6) Pair(tempCIni, tempCFin) else calcularFechasAbsolutas(nuevoCTipo)

            aplicarSeleccion(parentView, tipoAnalisis, nuevoPTipo, iniP, finP, nuevoCTipo, iniC, finC)
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
            7 -> "Hoy"
            else -> "Periodo"
        }
    }

    private fun calcularFechasAbsolutas(rangoTipo: Int): Pair<Long, Long> {
        val hoy = System.currentTimeMillis()
        if (rangoTipo == 5 || rangoTipo < 0) return Pair(0L, hoy)
        var inicio = 0L
        var fin = Long.MAX_VALUE
        val cal = Calendar.getInstance()

        when (rangoTipo) {
            0 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fin = maximizeTime(cal).timeInMillis
                fin = minOf(fin, hoy)
            }
            1 -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fin = maximizeTime(cal).timeInMillis
            }
            2 -> { fin = hoy; cal.add(Calendar.DAY_OF_YEAR, -7); inicio = resetTime(cal).timeInMillis }
            3 -> { fin = hoy; cal.add(Calendar.DAY_OF_YEAR, -30); inicio = resetTime(cal).timeInMillis }
            4 -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal).timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER); cal.set(Calendar.DAY_OF_MONTH, 31)
                fin = maximizeTime(cal).timeInMillis; fin = minOf(fin, hoy)
            }
            7 -> { inicio = resetTime(cal).timeInMillis; fin = maximizeTime(cal).timeInMillis }
        }
        return Pair(inicio, fin)
    }

    private fun abrirSelectorFechas(view: View, tipoAnalisis: Int, isComp: Boolean, onSeleccion: (Long, Long) -> Unit) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText(if (isComp) "Seleccionar Periodo de Comparación" else "Seleccionar Periodo de Análisis")
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection -> onSeleccion(selection.first, selection.second) }
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
                cal.add(Calendar.MONTH, -1); cal.set(Calendar.DAY_OF_MONTH, 1)
                val i = resetTime(cal.clone() as Calendar).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                Pair(i, maximizeTime(cal).timeInMillis)
            }
            1 -> {
                cal.add(Calendar.MONTH, -2); cal.set(Calendar.DAY_OF_MONTH, 1)
                val i = resetTime(cal.clone() as Calendar).timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                Pair(i, maximizeTime(cal).timeInMillis)
            }
            2 -> Pair(pIni - 7L * 24 * 60 * 60 * 1000, pIni - 1)
            3 -> Pair(pIni - 30L * 24 * 60 * 60 * 1000, pIni - 1)
            4 -> {
                cal.add(Calendar.YEAR, -1); cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1)
                val i = resetTime(cal.clone() as Calendar).timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER); cal.set(Calendar.DAY_OF_MONTH, 31)
                Pair(i, maximizeTime(cal).timeInMillis)
            }
            6 -> { val duracion = pFin - pIni; Pair(pIni - duracion - 1, pIni - 1) }
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
            -2 -> when (pTipo) {
                0 -> "vs Mes Anterior"
                1 -> "vs Hace 2 Meses"
                2 -> "vs 7 Días Previos"
                3 -> "vs 30 Días Previos"
                4 -> "vs Año Anterior"
                6 -> "vs Periodo Previo"
                else -> "vs Anterior"
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
        pieChart.isDrawHoleEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.setExtraOffsets(0f, 0f, 0f, 0f)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.isWordWrapEnabled = true
        legend.textSize = 11f
        legend.formSize = 11f
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
            override fun onNothingSelected() { view.findViewById<LinearLayout>(R.id.layoutDetalleSlice).visibility = View.GONE }
        })
    }

    private fun setupLineChartIngresos(view: View) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChartIngresos)
        lineChart.description.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        val legend = lineChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 14f
        legend.formSize = 14f
        legend.textColor = ContextCompat.getColor(requireContext(), android.R.color.tab_indicator_text)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textSize = 13f
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
        yAxis.textSize = 13f
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
                if (index >= 0 && index < fechasIngresosFormat.size) txtFecha.text = "Ingresos del ${fechasIngresosFormat[index]}"
                else txtFecha.text = "Dato Anterior"
                val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                txtMonto.text = format.format(e?.y ?: 0f)
            }
            override fun onNothingSelected() { view.findViewById<LinearLayout>(R.id.layoutDetalleSliceIngresos).visibility = View.GONE }
        })
    }

    private fun obtenerDatosCategoriaReal(mov: Movimiento): Pair<String, Int> {
        val catEncontrada = listaCategoriasGlobal.find { it.id == mov.categoriaId }
        return if (catEncontrada != null) {
            try { Pair("${catEncontrada.emoji} ${catEncontrada.nombre}", Color.parseColor(catEncontrada.colorHex)) }
            catch (e: Exception) { Pair("${catEncontrada.emoji} ${catEncontrada.nombre}", Color.GRAY) }
        } else {
            Pair("📦 ${mov.categoria}", Color.parseColor("#9E9E9E"))
        }
    }

    private fun rellenarPieChart(pieChart: PieChart, movimientos: List<Movimiento>) {
        val agrupado = HashMap<String, Double>()
        val coloresAgrupados = HashMap<String, Int>()

        for (mov in movimientos) {
            val (nombreCategoria, colorAsignado) = obtenerDatosCategoriaReal(mov)
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

    private fun parseDateToMillis(fecha: String?): Long {
        if (fecha.isNullOrEmpty()) return 0L
        val formats = arrayOf("dd/MM/yyyy", "yyyy-MM-dd", "dd/MM/yy", "dd-MM-yyyy")
        for (f in formats) {
            try {
                val parsed = SimpleDateFormat(f, Locale.getDefault()).parse(fecha)
                if (parsed != null) return parsed.time
            } catch (e: Exception) {}
        }
        return 0L
    }

    private fun procesarGraficoGastos(view: View) {
        val pieChartActual = view.findViewById<PieChart>(R.id.pieChartResumen)
        val pieChartComp = view.findViewById<PieChart>(R.id.pieChartComparacion)
        val layoutComp = view.findViewById<LinearLayout>(R.id.layoutPieComparacion)
        val txtActual = view.findViewById<TextView>(R.id.txtLabelActualGastos)
        val txtAnterior = view.findViewById<TextView>(R.id.txtLabelAnteriorGastos)
        val txtTotal = view.findViewById<TextView>(R.id.txtTotalGastos)
        val cardTendencia = view.findViewById<CardView>(R.id.cardTendenciaGastos)

        view.findViewById<LinearLayout>(R.id.layoutDetalleSlice).visibility = View.GONE
        val sdfRangoCenter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        val gastosActuales = listaMovimientosReal.filter { it.tipo == 0 }.filter { mov ->
            if (pInicioGastos == 0L && pFinGastos == Long.MAX_VALUE) true
            else { val time = parseDateToMillis(mov.fecha); time in pInicioGastos..pFinGastos }
        }

        val totalGastos = gastosActuales.sumOf { it.cantidad }
        val formatCurrency = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        txtTotal.text = formatCurrency.format(totalGastos)

        rellenarPieChart(pieChartActual, gastosActuales)

        val validDates = gastosActuales.map { parseDateToMillis(it.fecha) }.filter { it > 0L }
        val txtFechaActual = if (pTipoGastos == 5 && validDates.isNotEmpty()) {
            val minD = validDates.minOrNull() ?: 0L
            val maxD = validDates.maxOrNull() ?: 0L
            "${sdfRangoCenter.format(Date(minD))} - ${sdfRangoCenter.format(Date(maxD))}"
        } else if (pTipoGastos == 5) {
            "Historial Completo"
        } else {
            "${sdfRangoCenter.format(Date(if (pInicioGastos == 0L) System.currentTimeMillis() else pInicioGastos))} - ${sdfRangoCenter.format(Date(if (pFinGastos == Long.MAX_VALUE) System.currentTimeMillis() else pFinGastos))}"
        }

        val txtRangoHeader = view.findViewById<TextView>(R.id.txtRangoFechas)
        val btnConf = view.findViewById<MaterialButton>(R.id.btnConfigurarAnalisisGastos)
        if (pTipoGastos == 5) {
            txtRangoHeader.text = txtFechaActual
            btnConf.text = "Periodo: $txtFechaActual"
        } else {
            txtRangoHeader.text = obtenerTextoRango(pTipoGastos, pInicioGastos, pFinGastos)
            btnConf.text = "Periodo: ${obtenerTextoRango(pTipoGastos, pInicioGastos, pFinGastos)}"
        }

        txtActual.visibility = View.VISIBLE

        if (cTipoGastos != -1) {
            txtActual.text = "Actual\n($txtFechaActual)"
            layoutComp.visibility = View.VISIBLE

            val (cIni, cFin) = if (cTipoGastos == -2) obtenerFechasComparacionAuto(pTipoGastos, pInicioGastos, pFinGastos) else Pair(cInicioGastos, cFinGastos)

            val gastosAnt = listaMovimientosReal.filter { it.tipo == 0 }.filter { mov ->
                if (cIni == 0L && cFin == Long.MAX_VALUE) true
                else { val time = parseDateToMillis(mov.fecha); time in cIni..cFin }
            }

            rellenarPieChart(pieChartComp, gastosAnt)

            val validDatesAnt = gastosAnt.map { parseDateToMillis(it.fecha) }.filter { it > 0L }
            val txtFechaAnt = if (cIni == 0L && cFin == Long.MAX_VALUE && validDatesAnt.isNotEmpty()) {
                val minD = validDatesAnt.minOrNull() ?: 0L
                val maxD = validDatesAnt.maxOrNull() ?: 0L
                "${sdfRangoCenter.format(Date(minD))} - ${sdfRangoCenter.format(Date(maxD))}"
            } else if (cIni == 0L && cFin == Long.MAX_VALUE) {
                "Historial Completo"
            } else {
                "${sdfRangoCenter.format(Date(cIni))} - ${sdfRangoCenter.format(Date(if (cFin == Long.MAX_VALUE) System.currentTimeMillis() else cFin))}"
            }

            txtAnterior.text = "Anterior\n($txtFechaAnt)"
            val totalAnt = gastosAnt.sumOf { it.cantidad }
            actualizarUITendencia(view, 0, totalGastos, totalAnt, obtenerNombreModo(cTipoGastos, pTipoGastos))
        } else {
            txtActual.text = txtFechaActual
            layoutComp.visibility = View.GONE
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

        val ingresosBase = listaMovimientosReal.filter { it.tipo == 1 }

        val ingresosActuales = ingresosBase.filter { mov ->
            if (pInicioIngresos == 0L && pFinIngresos == Long.MAX_VALUE) true
            else { val time = parseDateToMillis(mov.fecha); time in pInicioIngresos..pFinIngresos }
        }

        val agrupadoActual = HashMap<String, Double>()
        for (mov in ingresosActuales) {
            val fechaCorta = mov.fecha.take(5)
            agrupadoActual[fechaCorta] = (agrupadoActual[fechaCorta] ?: 0.0) + mov.cantidad
        }

        val totalIngresos = ingresosActuales.sumOf { it.cantidad }
        val formatCurrency = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        txtTotal.text = formatCurrency.format(totalIngresos)

        val sdfRangoCenter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val validDatesIngresos = ingresosActuales.map { parseDateToMillis(it.fecha) }.filter { it > 0L }
        val txtFechaActualIngresos = if (pTipoIngresos == 5 && validDatesIngresos.isNotEmpty()) {
            val minD = validDatesIngresos.minOrNull() ?: 0L
            val maxD = validDatesIngresos.maxOrNull() ?: 0L
            "${sdfRangoCenter.format(Date(minD))} - ${sdfRangoCenter.format(Date(maxD))}"
        } else if (pTipoIngresos == 5) {
            "Historial Completo"
        } else {
            "${sdfRangoCenter.format(Date(if (pInicioIngresos == 0L) System.currentTimeMillis() else pInicioIngresos))} - ${sdfRangoCenter.format(Date(if (pFinIngresos == Long.MAX_VALUE) System.currentTimeMillis() else pFinIngresos))}"
        }

        val txtRangoIng = view.findViewById<TextView>(R.id.txtRangoFechasIngresos)
        val btnConfIng = view.findViewById<MaterialButton>(R.id.btnConfigurarAnalisisIngresos)
        if (pTipoIngresos == 5) {
            txtRangoIng.text = txtFechaActualIngresos
            btnConfIng.text = "Periodo: $txtFechaActualIngresos"
        } else {
            txtRangoIng.text = obtenerTextoRango(pTipoIngresos, pInicioIngresos, pFinIngresos)
            btnConfIng.text = "Periodo: ${obtenerTextoRango(pTipoIngresos, pInicioIngresos, pFinIngresos)}"
        }

        if (agrupadoActual.isEmpty()) {
            lineChart.setNoDataText("No hay datos en este periodo")
            lineChart.invalidate()
        }

        val sdfSort = SimpleDateFormat("dd/MM", Locale.getDefault())
        val listaOrdenadaAct = agrupadoActual.entries.sortedBy {
            try { sdfSort.parse(it.key)?.time ?: 0L } catch (e: Exception) { 0L }
        }

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
                if (cIni == 0L && cFin == Long.MAX_VALUE) true
                else { val time = parseDateToMillis(mov.fecha); time in cIni..cFin }
            }

            val agrupadoAnt = HashMap<String, Double>()
            for (mov in ingresosAnt) {
                val fechaCorta = mov.fecha.take(5)
                agrupadoAnt[fechaCorta] = (agrupadoAnt[fechaCorta] ?: 0.0) + mov.cantidad
            }

            val listaOrdenadaAnt = agrupadoAnt.entries.sortedBy {
                try { sdfSort.parse(it.key)?.time ?: 0L } catch (e: Exception) { 0L }
            }
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
        } catch (e: Exception) {}
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