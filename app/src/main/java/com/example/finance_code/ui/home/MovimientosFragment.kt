package com.example.finance_code.ui.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.DiscreetModeManager
import com.example.finance_code.R
import com.example.finance_code.ShakeDetector
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.ui.transaction.addTransaction
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
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

    private lateinit var btnFiltrar: Button
    private lateinit var tvEmpty: TextView
    private lateinit var tvSaldoTotal: TextView
    private lateinit var tvUserName: TextView
    private lateinit var btnHideBalance: ImageButton
    private lateinit var btnDiscreetModeManual: ImageButton

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

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
            user!!.displayName!!.uppercase()
        } else {
            userEmail.substringBefore("@").uppercase()
        }

        btnFiltrar = view.findViewById(R.id.btnFiltrarFechas)
        tvEmpty = view.findViewById(R.id.tvEmptyMessage)
        tvSaldoTotal = view.findViewById(R.id.tvSaldoTotal)
        tvUserName = view.findViewById(R.id.tvUserName)
        btnHideBalance = view.findViewById(R.id.btnHideBalance)
        btnDiscreetModeManual = view.findViewById(R.id.btnDiscreetModeManual)
        val fabAddTransaction = view.findViewById<FloatingActionButton>(R.id.fabAddTransaction)

        tvUserName.text = userNameDisplay

        cargarPreferencias()
        actualizarBotonFiltro()

        adapter = MovimientosAdapter(emptyList())
        adapter.setOnItemLongClickListener { mov ->
            val bundle = Bundle().apply { putParcelable("movimiento", mov) }
            findNavController().navigate(R.id.action_movimientosFragment_to_eTransactionFragment, bundle)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.listMovies)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fabAddTransaction.setOnClickListener {
            val intent = Intent(requireContext(), addTransaction::class.java)
            startActivity(intent)
        }

        btnDiscreetModeManual.setOnClickListener {
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

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)

        shakeDetector = ShakeDetector {
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

        actualizarUIModoDiscreto()
        updateDiscreetModeButtonIcon()
        checkAndShowShakeAnimation()
    }

    override fun onResume() {
        super.onResume()

        accelerometer?.let {
            sensorManager.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: "default"
        val prefsName = "${uid}_UserProfilePrefs"
        val profilePrefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val customName = profilePrefs.getString("user_name", null)

        if (!customName.isNullOrEmpty() && ::tvUserName.isInitialized) {
            tvUserName.text = customName.uppercase()
        }

        updateDiscreetModeButtonIcon()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }

    private fun actualizarSaldoTotal() {
        val total = listaMovimientosGlobal.sumOf { if (it.tipo == 1) it.cantidad else -it.cantidad }
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        formatter.maximumFractionDigits = 0

        if (DiscreetModeManager.isDiscreetModeActive || isBalanceHidden) {
            tvSaldoTotal.text = "$ •••••••"
        } else {
            tvSaldoTotal.text = formatter.format(total)
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
        val formats = arrayOf(
            "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss",
            "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy"
        )
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
        if (::btnDiscreetModeManual.isInitialized) {
            val drawableRes = if (DiscreetModeManager.isDiscreetModeActive)
                R.drawable.ic_visibility_off
            else
                R.drawable.ic_visibility
            btnDiscreetModeManual.setImageResource(drawableRes)
        }
    }

    private fun checkAndShowShakeAnimation() {
        val sharedPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenAnimation = sharedPrefs.getBoolean("has_seen_shake_animation", false)

        if (!hasSeenAnimation) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_modo_discreto, null)

            // LA MAGIA: Usamos Dialog directamente en vez de AlertDialog.Builder
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(dialogView)
            dialog.setCancelable(false)

            // Hacemos transparente el fondo base del diálogo para que se vea el borde curvo del XML
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Ajustamos el ancho para que respete los márgenes
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val btnEntendido = dialogView.findViewById<View>(R.id.btnEntendido)
            btnEntendido.setOnClickListener {
                sharedPrefs.edit().putBoolean("has_seen_shake_animation", true).apply()
                dialog.dismiss()
            }
            dialog.show()
        }
    }
}