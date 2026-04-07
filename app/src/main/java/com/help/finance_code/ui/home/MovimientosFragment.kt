package com.help.finance_code.ui.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.help.finance_code.DiscreetModeManager
import com.help.finance_code.PDF.ExtractoBancarioHelper
import com.help.finance_code.PDF.MovimientoExtraido
import com.help.finance_code.R
import com.help.finance_code.ShakeDetector
import com.help.finance_code.data.AppDB
import com.help.finance_code.data.Movimiento
import com.help.finance_code.data.MovimientoRepository
import com.help.finance_code.ui.transaction.addTransaction
import com.help.finance_code.utils.ThemeUtils
import com.help.finance_code.viewmodel.MovimientoViewModel
import com.help.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MovimientosFragment : Fragment() {

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var database: AppDB
    private lateinit var adapter: MovimientosAdapter

    private var listaMovimientosGlobal: List<Movimiento> = emptyList()

    private var userEmail: String = "default"
    private var userNameDisplay: String = "USUARIO"

    private var fTipo: Int = 5
    private var fInicio: Long = 0L
    private var fFin: Long = Long.MAX_VALUE
    private var fAgrupacion: Int = 1
    private var isBalanceHidden = false

    // Vistas
    private lateinit var btnFiltrar: Button
    private lateinit var tvEmpty: TextView
    private lateinit var tvSaldoTotal: TextView
    private lateinit var btnHideBalance: ImageButton
    private lateinit var cardSaldoContainer: View
    private var tvUserName: TextView? = null
    private var btnDiscreetModeManual: ImageButton? = null

    // Multi-selección Vistas
    private lateinit var cardSelectionMode: View
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnSelectAll: ImageButton
    private lateinit var btnDeleteSelected: ImageButton
    private lateinit var btnCancelSelection: ImageButton

    // FAB Expandible (Corregido el nombre fabAddTransaction)
    private var isFabOpen = false
    private lateinit var fabAddTransaction: FloatingActionButton
    private lateinit var fabManual: ExtendedFloatingActionButton
    private lateinit var fabImportPDF: ExtendedFloatingActionButton
    private lateinit var bgFabDim: View

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            procesarPDF(uri, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_movimientos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        userEmail = user?.email ?: "default"
        val uid = user?.uid ?: "default"

        val prefsName = "${uid}_UserProfilePrefs"
        val profilePrefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val customName = profilePrefs.getString("user_name", null)

        userNameDisplay = if (!customName.isNullOrEmpty()) {
            customName.uppercase()
        } else if (!user?.displayName.isNullOrEmpty()) {
            user.displayName!!.uppercase()
        } else {
            userEmail.substringBefore("@").uppercase()
        }

        // Enlace de vistas básicas
        btnFiltrar = view.findViewById(R.id.btnFiltrarFechas)
        tvEmpty = view.findViewById(R.id.tvEmptyMessage)
        tvSaldoTotal = view.findViewById(R.id.tvSaldoTotal)
        btnHideBalance = view.findViewById(R.id.btnHideBalance)
        cardSaldoContainer = view.findViewById(R.id.cardSaldoContainer)
        tvUserName = view.findViewById(R.id.tvUserName)
        btnDiscreetModeManual = view.findViewById(R.id.btnDiscreetModeManual)

        // Enlace Multi-selección
        cardSelectionMode = view.findViewById(R.id.cardSelectionMode)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        btnSelectAll = view.findViewById(R.id.btnSelectAll)
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected)
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection)

        // Enlace FABs (Nombres corregidos)
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction)
        fabManual = view.findViewById(R.id.fabAddManual)
        fabImportPDF = view.findViewById(R.id.fabImportPDF)
        bgFabDim = view.findViewById(R.id.bgFabDim)

        tvUserName?.text = userNameDisplay

        val auraColor = ThemeUtils.getAuraColor(requireContext())
        aplicarBordeTarjetaCredito(cardSaldoContainer, auraColor)
        fabAddTransaction.backgroundTintList = ColorStateList.valueOf(auraColor)

        cargarPreferencias()
        actualizarBotonFiltro()

        // Lógica del Adapter con Modo Selección
        adapter = MovimientosAdapter(emptyList())
        adapter.onItemClickListener = { mov ->
            val bundle = Bundle().apply { putParcelable("movimiento", mov) }
            findNavController().navigate(R.id.action_movimientosFragment_to_eTransactionFragment, bundle)
        }

        adapter.onSelectionModeChangeListener = { isSelectionMode, count ->
            if (isSelectionMode) {
                btnFiltrar.visibility = View.GONE
                cardSelectionMode.visibility = View.VISIBLE
                tvSelectedCount.text = "$count seleccionados"

                // Ocultar FABs en modo selección para no molestar
                if (isFabOpen) toggleFabMenu()
                fabAddTransaction.hide()
            } else {
                btnFiltrar.visibility = View.VISIBLE
                cardSelectionMode.visibility = View.GONE
                fabAddTransaction.show()
            }
        }

        // Lógica Botones de Selección
        btnSelectAll.setOnClickListener { adapter.selectAll() }
        btnCancelSelection.setOnClickListener { adapter.setSelectionModeActive(false) }
        btnDeleteSelected.setOnClickListener {
            val eliminados = adapter.selectedItems.toList()
            if (eliminados.isEmpty()) return@setOnClickListener

            val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirm, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensajeDelete)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelDelete)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmDelete)

            tvMensaje.text = "¿Estás seguro de eliminar permanentemente ${eliminados.size} movimientos?"

            btnCancel.setOnClickListener { dialog.dismiss() }

            btnConfirm.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    eliminados.forEach { database.movimientoDao().eliminar(it) }
                    activity?.runOnUiThread {
                        adapter.setSelectionModeActive(false)
                        Toast.makeText(requireContext(), "Movimientos eliminados", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
            dialog.show()
        }

        // Configuración RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.listMovies)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Lógica Menú FAB Expandible
        bgFabDim.setOnClickListener { if (isFabOpen) toggleFabMenu() }
        fabAddTransaction.setOnClickListener { toggleFabMenu() }

        fabManual.setOnClickListener {
            toggleFabMenu()
            startActivity(Intent(requireContext(), addTransaction::class.java))
        }

        fabImportPDF.setOnClickListener {
            toggleFabMenu()
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }

        // Lógica Modo Discreto y Ocultar Saldo
        btnDiscreetModeManual?.setOnClickListener {
            DiscreetModeManager.toggleMode()
            actualizarUIModoDiscreto()
            updateDiscreetModeButtonIcon()
            val message = if (DiscreetModeManager.isDiscreetModeActive) "Modo Discreto Activado" else "Modo Visible Activado"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        btnHideBalance.setOnClickListener {
            isBalanceHidden = !isBalanceHidden
            btnHideBalance.setImageResource(if (isBalanceHidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
            actualizarSaldoTotal()
        }

        // Database y ViewModel
        if (userEmail != "default") {
            database = AppDB.getDatabase(requireContext(), userEmail)
            val repository = MovimientoRepository(database.movimientoDao())
            val factory = MovimientoViewModelFactory(repository)
            viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

            viewModel.movimientos.observe(viewLifecycleOwner) { movimientos ->
                listaMovimientosGlobal = movimientos
                actualizarSaldoTotal()
                aplicarFiltrosActuales()
            }
        }

        btnFiltrar.setOnClickListener {
            mostrarBottomSheetFiltros()
        }

        // CANDADO DEL SENSOR
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)

        shakeDetector = ShakeDetector {
            val sharedPrefs = requireContext().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
            val isShakeDisabled = sharedPrefs.getBoolean("disable_shake_gesture", false)

            if (!isShakeDisabled) {
                activity?.runOnUiThread {
                    DiscreetModeManager.toggleMode()
                    actualizarUIModoDiscreto()
                    updateDiscreetModeButtonIcon()

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
        }

        actualizarUIModoDiscreto()
        updateDiscreetModeButtonIcon()
        checkAndShowShakeAnimation()
    }

    private fun toggleFabMenu() {
        isFabOpen = !isFabOpen
        if (isFabOpen) {
            bgFabDim.visibility = View.VISIBLE
            fabManual.show()
            fabImportPDF.show()
            fabAddTransaction.animate().rotation(45f).setDuration(200).start()

            fabManual.translationY = 50f
            fabManual.alpha = 0f
            fabManual.animate().translationY(0f).alpha(1f).setDuration(200).start()

            fabImportPDF.translationY = 50f
            fabImportPDF.alpha = 0f
            fabImportPDF.animate().translationY(0f).alpha(1f).setDuration(200).start()
        } else {
            bgFabDim.visibility = View.GONE
            fabAddTransaction.animate().rotation(0f).setDuration(200).start()

            fabManual.animate().translationY(50f).alpha(0f).setDuration(200).withEndAction {
                fabManual.hide()
            }.start()

            fabImportPDF.animate().translationY(50f).alpha(0f).setDuration(200).withEndAction {
                fabImportPDF.hide()
            }.start()
        }
    }

    private fun aplicarBordeTarjetaCredito(view: View, color: Int) {
        val density = resources.displayMetrics.density
        val strokeWidth = (2 * density).toInt()

        var bg = view.background?.mutate()

        if (bg is RippleDrawable) {
            bg = bg.getDrawable(0)?.mutate()
        }

        if (bg is LayerDrawable) {
            val lastLayerIndex = bg.numberOfLayers - 1
            if (lastLayerIndex >= 0) {
                val strokeItem = bg.getDrawable(lastLayerIndex) as? GradientDrawable
                strokeItem?.setStroke(strokeWidth, color)
            }
        } else if (bg is GradientDrawable) {
            bg.setStroke(strokeWidth, color)
        }
    }

    private fun formatCop(monto: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0
        return formatter.format(monto)
    }

    override fun onResume() {
        super.onResume()

        if (::cardSaldoContainer.isInitialized) {
            val auraColor = ThemeUtils.getAuraColor(requireContext())
            aplicarBordeTarjetaCredito(cardSaldoContainer, auraColor)
        }

        val sharedPrefs = requireContext().getSharedPreferences("AppPrefe", Context.MODE_PRIVATE)
        val isShakeDisabled = sharedPrefs.getBoolean("disable_shake_gesture", false)

        if (!isShakeDisabled) {
            accelerometer?.let {
                sensorManager.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else {
            sensorManager.unregisterListener(shakeDetector)
        }

        updateDiscreetModeButtonIcon()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }

    private fun actualizarSaldoTotal() {
        val total = listaMovimientosGlobal.sumOf { if (it.tipo == 1) it.cantidad else -it.cantidad }
        if (DiscreetModeManager.isDiscreetModeActive || isBalanceHidden) {
            tvSaldoTotal.text = "$ •••••••"
        } else {
            tvSaldoTotal.text = formatCop(total)
        }
    }

    private fun actualizarUIModoDiscreto() {
        actualizarSaldoTotal()
        adapter.updateDiscreetMode()
    }

    private fun getSafeLong(prefs: SharedPreferences, key: String, defaultVal: Long): Long {
        return try { prefs.getLong(key, defaultVal) } catch (e: Exception) { defaultVal }
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE)
        fTipo = prefs.getInt("fTipoMovs", 5)
        fAgrupacion = prefs.getInt("fAgrupacion", 1)

        if (fTipo != 6 && fTipo != 5) {
            val bounds = calcularFechasAbsolutas(fTipo)
            fInicio = bounds.first
            fFin = bounds.second
        } else {
            fInicio = getSafeLong(prefs, "fInicioMovs", 0L)
            fFin = getSafeLong(prefs, "fFinMovs", Long.MAX_VALUE)
        }
    }

    private fun guardarPreferencias() {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE).edit()
        prefs.putInt("fTipoMovs", fTipo)
        prefs.putInt("fAgrupacion", fAgrupacion)
        prefs.putLong("fInicioMovs", fInicio)
        prefs.putLong("fFinMovs", fFin)
        prefs.apply()
    }

    private fun parseDateToMillis(fecha: String?): Long {
        if (fecha.isNullOrEmpty()) return 0L
        val formats = arrayOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        for (f in formats) {
            try { return SimpleDateFormat(f, Locale.getDefault()).parse(fecha)?.time ?: 0L } catch (e: Exception) {}
        }
        return 0L
    }

    private fun parseDateTimeToMillis(fecha: String?, hora: String?): Long {
        if (fecha.isNullOrEmpty()) return 0L
        val h = if (hora.isNullOrEmpty()) "00:00:00" else hora
        val dateTimeStr = "$fecha $h"
        val formats = arrayOf("yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        for (f in formats) {
            try { return SimpleDateFormat(f, Locale.getDefault()).parse(dateTimeStr)?.time ?: 0L } catch (e: Exception) {}
        }
        return 0L
    }

    private fun aplicarFiltrosActuales() {
        val filtrados = listaMovimientosGlobal.filter { mov ->
            if (fInicio == 0L && fFin == Long.MAX_VALUE) true
            else {
                val time = parseDateToMillis(mov.fecha)
                time in fInicio..fFin
            }
        }

        tvEmpty.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE

        val ordenados = filtrados.sortedByDescending { parseDateTimeToMillis(it.fecha, it.hora) }

        val itemsFinales = mutableListOf<MovimientoListItem>()

        if (fAgrupacion == 0 || ordenados.isEmpty()) {
            itemsFinales.addAll(ordenados.map { MovimientoListItem.Item(it) })
        } else {
            val formatDia = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CO"))
            val formatMes = SimpleDateFormat("MMMM yyyy", Locale("es", "CO"))
            val formatAno = SimpleDateFormat("yyyy", Locale("es", "CO"))

            var currentHeaderTitle = ""

            for (mov in ordenados) {
                val date = Date(parseDateToMillis(mov.fecha))
                val title = when (fAgrupacion) {
                    1 -> formatDia.format(date)
                    2 -> formatMes.format(date)
                    3 -> formatAno.format(date)
                    else -> ""
                }

                if (title != currentHeaderTitle) {
                    itemsFinales.add(MovimientoListItem.Header(title.replaceFirstChar { it.uppercase() }))
                    currentHeaderTitle = title
                }
                itemsFinales.add(MovimientoListItem.Item(mov))
            }
        }

        if (adapter.isSelectionMode) {
            adapter.setSelectionModeActive(false)
        }

        adapter.setData(itemsFinales)
    }

    private fun actualizarBotonFiltro() {
        val texto = when (fTipo) {
            0 -> "Este Mes"
            1 -> "Mes Anterior"
            2 -> "Últimos 7 Días"
            3 -> "Últimos 30 Días"
            4 -> "Este Año"
            5 -> "Todo el Historial"
            6 -> {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                "${sdf.format(Date(fInicio))} - ${sdf.format(Date(fFin))}"
            }
            7 -> "Hoy"
            else -> "Filtrar por Fecha"
        }
        btnFiltrar.text = texto
    }

    private fun mostrarBottomSheetFiltros() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_filtros, null)
        bottomSheetDialog.setContentView(sheetView)

        try {
            sheetView.findViewById<TextView>(R.id.tvTituloComparacion)?.visibility = View.GONE
            sheetView.findViewById<View>(R.id.divisorComparacion)?.visibility = View.GONE
            sheetView.findViewById<View>(R.id.scrollComparacion)?.visibility = View.GONE
            sheetView.findViewById<ChipGroup>(R.id.chipGroupComparacion)?.visibility = View.GONE
        } catch (e: Exception) {}

        val chipGroupPeriodo = sheetView.findViewById<ChipGroup>(R.id.chipGroupPeriodo)
        val chipGroupAgrupacion = sheetView.findViewById<ChipGroup>(R.id.chipGroupAgrupacion)
        val btnAplicar = sheetView.findViewById<MaterialButton>(R.id.btnAplicarAnalisis)
        val chipPersonalizado = sheetView.findViewById<Chip>(R.id.chipPersonalizado)

        var tempIni = fInicio
        var tempFin = fFin

        if (fTipo == 6) {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            chipPersonalizado.text = "${sdf.format(Date(tempIni))} - ${sdf.format(Date(tempFin))}"
        }

        chipPersonalizado.setOnClickListener {
            val builder = MaterialDatePicker.Builder.dateRangePicker()
            builder.setTitleText("Seleccionar Periodo")
            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                tempIni = selection.first
                tempFin = selection.second
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                chipPersonalizado.text = "${sdf.format(Date(tempIni))} - ${sdf.format(Date(tempFin))}"
                chipPersonalizado.isChecked = true
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }

        val chipIdPeriodo = when (fTipo) {
            0 -> R.id.chipEsteMes
            1 -> R.id.chipMesAnterior
            2 -> R.id.chip7Dias
            3 -> R.id.chip30Dias
            4 -> R.id.chipEsteAno
            6 -> R.id.chipPersonalizado
            7 -> R.id.chipHoy
            else -> R.id.chipHistorial
        }
        sheetView.findViewById<Chip>(chipIdPeriodo)?.isChecked = true

        val chipIdAgrupar = when (fAgrupacion) {
            0 -> R.id.chipAgruparNinguno
            1 -> R.id.chipAgruparDia
            2 -> R.id.chipAgruparMes
            3 -> R.id.chipAgruparAno
            else -> R.id.chipAgruparDia
        }
        sheetView.findViewById<Chip>(chipIdAgrupar)?.isChecked = true

        btnAplicar.setOnClickListener {
            fTipo = when (chipGroupPeriodo.checkedChipId) {
                R.id.chipHoy -> 7
                R.id.chipEsteMes -> 0
                R.id.chipMesAnterior -> 1
                R.id.chip7Dias -> 2
                R.id.chip30Dias -> 3
                R.id.chipEsteAno -> 4
                R.id.chipHistorial -> 5
                R.id.chipPersonalizado -> 6
                else -> 5
            }

            fAgrupacion = when (chipGroupAgrupacion.checkedChipId) {
                R.id.chipAgruparNinguno -> 0
                R.id.chipAgruparDia -> 1
                R.id.chipAgruparMes -> 2
                R.id.chipAgruparAno -> 3
                else -> 1
            }

            if (fTipo == 6) {
                fInicio = tempIni
                fFin = tempFin
            } else {
                val bounds = calcularFechasAbsolutas(fTipo)
                fInicio = bounds.first
                fFin = bounds.second
            }

            guardarPreferencias()
            actualizarBotonFiltro()
            aplicarFiltrosActuales()
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun calcularFechasAbsolutas(rangoTipo: Int): Pair<Long, Long> {
        val hoy = System.currentTimeMillis()
        if (rangoTipo == 5 || rangoTipo < 0) return Pair(0L, Long.MAX_VALUE)

        var inicio = 0L
        var fin = Long.MAX_VALUE
        val cal = Calendar.getInstance()

        fun resetTime(c: Calendar): Long {
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
            return c.timeInMillis
        }

        fun maximizeTime(c: Calendar): Long {
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59)
            return c.timeInMillis
        }

        when (rangoTipo) {
            0 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fin = maximizeTime(cal)
            }
            1 -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fin = maximizeTime(cal)
            }
            2 -> { fin = hoy; cal.add(Calendar.DAY_OF_YEAR, -7); inicio = resetTime(cal) }
            3 -> { fin = hoy; cal.add(Calendar.DAY_OF_YEAR, -30); inicio = resetTime(cal) }
            4 -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1)
                inicio = resetTime(cal)
                cal.set(Calendar.MONTH, Calendar.DECEMBER); cal.set(Calendar.DAY_OF_MONTH, 31)
                fin = maximizeTime(cal)
            }
            7 -> {
                inicio = resetTime(cal)
                fin = maximizeTime(cal)
            }
        }
        return Pair(inicio, fin)
    }

    private fun updateDiscreetModeButtonIcon() {
        val drawableRes = if (DiscreetModeManager.isDiscreetModeActive)
            R.drawable.ic_visibility_off
        else
            R.drawable.ic_visibility
        btnDiscreetModeManual?.setImageResource(drawableRes)
    }

    private fun checkAndShowShakeAnimation() {
        val sharedPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenAnimation = sharedPrefs.getBoolean("has_seen_shake_animation", false)

        if (!hasSeenAnimation) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_modo_discreto, null)

            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(dialogView)
            dialog.setCancelable(false)

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val btnEntendido = dialogView.findViewById<View>(R.id.btnEntendido)
            btnEntendido.setOnClickListener {
                sharedPrefs.edit().putBoolean("has_seen_shake_animation", true).apply()
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    private fun procesarPDF(uri: Uri, passwordIntento: String) {
        val helper = ExtractoBancarioHelper(requireContext())
        val (necesitaPassword, textoExtraido) = helper.extraerTextoDePDF(uri, passwordIntento)

        if (necesitaPassword) {
            if (passwordIntento.isNotEmpty()) {
                Toast.makeText(requireContext(), "Contraseña incorrecta. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
            }
            mostrarDialogoPasswordPDF(uri)
        } else if (textoExtraido != null) {
            val movimientos = helper.analizarExtracto(textoExtraido)
            if (movimientos != null && movimientos.isNotEmpty()) {
                mostrarResumenEImportar(movimientos)
            } else if (movimientos != null && movimientos.isEmpty()) {
                Toast.makeText(requireContext(), "No se encontraron movimientos extraíbles.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Formato de banco no soportado", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Error al leer el archivo o archivo corrupto.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoPasswordPDF(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pdf_password, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPdfPassword)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarPassword)
        val btnDesbloquear = dialogView.findViewById<Button>(R.id.btnDesbloquearPdf)

        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnDesbloquear.setOnClickListener {
            val password = etPassword.text.toString()
            if (password.isNotEmpty()) {
                procesarPDF(uri, password)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Ingresa una contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun mostrarResumenEImportar(movimientosExtraidos: List<MovimientoExtraido>) {
        if (movimientosExtraidos.isEmpty()) {
            Toast.makeText(requireContext(), "No se encontraron movimientos", Toast.LENGTH_SHORT).show()
            return
        }

        val ingresosList = movimientosExtraidos.filter { it.esIngreso }
        val gastosList = movimientosExtraidos.filter { !it.esIngreso }

        val sumaIngresos = ingresosList.sumOf { it.monto }
        val sumaGastos = gastosList.sumOf { it.monto }

        val dialogView = layoutInflater.inflate(R.layout.dialog_import_summary, null)

        val tvTotal = dialogView.findViewById<TextView>(R.id.tvTotalMovimientos)
        val cbIngresos = dialogView.findViewById<CheckBox>(R.id.cbIngresos)
        val cbGastos = dialogView.findViewById<CheckBox>(R.id.cbGastos)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarImportacion)
        val btnConfirmar = dialogView.findViewById<Button>(R.id.btnConfirmarImportacion)

        tvTotal.text = "Se encontraron ${movimientosExtraidos.size} movimientos en total."
        cbIngresos.text = "Ingresos detectados: ${ingresosList.size} (${formatCop(sumaIngresos)})"
        cbGastos.text = "Gastos detectados: ${gastosList.size} (${formatCop(sumaGastos)})"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnConfirmar.setOnClickListener {
            val importarIngresos = cbIngresos.isChecked
            val importarGastos = cbGastos.isChecked

            val movimientosAImportar = movimientosExtraidos.filter {
                (it.esIngreso && importarIngresos) || (!it.esIngreso && importarGastos)
            }

            if (movimientosAImportar.isNotEmpty()) {
                guardarMovimientosEnBD(movimientosAImportar)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Debes seleccionar al menos una opción", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun guardarMovimientosEnBD(movimientosAImportar: List<MovimientoExtraido>) {
        for (extraido in movimientosAImportar) {
            val nuevoMovimiento = Movimiento(
                cantidad = extraido.monto,
                tipo = if (extraido.esIngreso) 1 else 0,
                fecha = extraido.fecha,
                hora = "00:00:00",
                descripcion = extraido.descripcion,
                categoria = extraido.categoria,
                categoriaId = extraido.categoriaId,
                banco = extraido.banco
            )
            viewModel.insertar(nuevoMovimiento)
        }
        Toast.makeText(requireContext(), "${movimientosAImportar.size} movimientos importados exitosamente", Toast.LENGTH_LONG).show()
    }
}