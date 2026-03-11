package com.example.finance_code.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Categoria
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MovimientosFragment : Fragment() {

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var database: AppDB
    private lateinit var adapter: MovimientosAdapter

    private var listaMovimientosGlobal: List<Movimiento> = emptyList()
    private var listaCategoriasGlobal: List<Categoria> = emptyList()

    private var userEmail: String = "default"

    private var fTipo: Int = 5
    private var fInicio: Long = 0L
    private var fFin: Long = Long.MAX_VALUE

    private lateinit var btnFiltrar: Button
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_movimientos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "default"
        btnFiltrar = view.findViewById(R.id.btnFiltrarFechas)
        tvEmpty = view.findViewById(R.id.tvEmptyMessage)

        cargarPreferencias()
        actualizarBotonFiltro()

        adapter = MovimientosAdapter { mov ->
            val bundle = Bundle().apply { putParcelable("movimiento", mov) }
            findNavController().navigate(R.id.action_movimientosFragment_to_eTransactionFragment, bundle)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMovimientos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        if (userEmail != "default") {
            database = AppDB.getDatabase(requireContext(), userEmail)
            val repository = MovimientoRepository(database.movimientoDao())
            val factory = MovimientoViewModelFactory(repository)
            viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

            viewLifecycleOwner.lifecycleScope.launch {
                database.categoriaDao().obtenerTodas().collect { categorias ->
                    listaCategoriasGlobal = categorias
                    aplicarFiltrosActuales()
                }
            }

            viewModel.movimientos.observe(viewLifecycleOwner) { movimientos ->
                listaMovimientosGlobal = movimientos
                aplicarFiltrosActuales()
            }
        }

        btnFiltrar.setOnClickListener {
            mostrarBottomSheetFiltros(view)
        }
    }

    private fun getSafeLong(prefs: SharedPreferences, key: String, defaultVal: Long): Long {
        return try { prefs.getLong(key, defaultVal) } catch (e: Exception) { defaultVal }
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE)
        fTipo = prefs.getInt("fTipoMovs", 5)
        fInicio = getSafeLong(prefs, "fInicioMovs", 0L)
        fFin = getSafeLong(prefs, "fFinMovs", Long.MAX_VALUE)
    }

    private fun guardarPreferencias() {
        val prefs = requireContext().getSharedPreferences("analisis_prefs_$userEmail", Context.MODE_PRIVATE).edit()
        prefs.putInt("fTipoMovs", fTipo)
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

    private fun aplicarFiltrosActuales() {
        val filtrados = listaMovimientosGlobal.filter { mov ->
            if (fInicio == 0L && fFin == Long.MAX_VALUE) true
            else {
                val time = parseDateToMillis(mov.fecha)
                time in fInicio..fFin
            }
        }

        tvEmpty.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        adapter.actualizarListaYCategorias(filtrados, listaCategoriasGlobal)
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
            else -> "Filtrar por Fecha"
        }
        btnFiltrar.text = texto
    }

    private fun mostrarBottomSheetFiltros(parentView: View) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_filtros, null)
        bottomSheetDialog.setContentView(sheetView)

        val chipGroupPeriodo = sheetView.findViewById<ChipGroup>(R.id.chipGroupPeriodo)
        sheetView.findViewById<TextView>(R.id.tvTituloComparacion).visibility = View.GONE
        sheetView.findViewById<ChipGroup>(R.id.chipGroupComparacion).visibility = View.GONE

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

        val chipId = when (fTipo) {
            0 -> R.id.chipEsteMes
            1 -> R.id.chipMesAnterior
            2 -> R.id.chip7Dias
            3 -> R.id.chip30Dias
            4 -> R.id.chipEsteAno
            6 -> R.id.chipPersonalizado
            else -> R.id.chipHistorial
        }
        sheetView.findViewById<Chip>(chipId)?.isChecked = true

        btnAplicar.setOnClickListener {
            fTipo = when (chipGroupPeriodo.checkedChipId) {
                R.id.chipEsteMes -> 0
                R.id.chipMesAnterior -> 1
                R.id.chip7Dias -> 2
                R.id.chip30Dias -> 3
                R.id.chipEsteAno -> 4
                R.id.chipHistorial -> 5
                R.id.chipPersonalizado -> 6
                else -> 5
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
        }
        return Pair(inicio, fin)
    }
}