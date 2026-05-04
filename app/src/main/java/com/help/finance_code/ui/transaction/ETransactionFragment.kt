package com.help.finance_code.ui.transaction

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.core.os.BundleCompat
import com.help.finance_code.R
import com.help.finance_code.data.AppDB
import com.help.finance_code.data.Categoria
import com.help.finance_code.data.Movimiento
import com.help.finance_code.data.MovimientoRepository
import com.help.finance_code.utils.ThemeUtils
import com.help.finance_code.viewmodel.MovimientoViewModel
import com.help.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Nota: La clase RamaActiva ya está declarada en el paquete por addTransaction.kt
// por lo que podemos usarla directamente sin necesidad de redeclararla aquí.

class ETransactionFragment : Fragment(R.layout.fragment_e_transaction) {

    private lateinit var viewModel: MovimientoViewModel
    private lateinit var movimiento: Movimiento
    private var tipoMovimientoSeleccionado: Int = 0

    private lateinit var cardEgreso: MaterialCardView
    private lateinit var cardIngreso: MaterialCardView
    private lateinit var ivEgresoArrow: ImageView
    private lateinit var ivIngresoArrow: ImageView
    private lateinit var tvEgresoText: TextView
    private lateinit var tvIngresoText: TextView
    private lateinit var etMonto: EditText

    private lateinit var cardSelectorCategoria: MaterialCardView
    private lateinit var cardEmojiFondo: MaterialCardView
    private lateinit var tvEmojiCategoriaActual: TextView
    private lateinit var tvNombreCategoriaActual: TextView

    // Variables UI para el Bolsillo (Rama) - con ? para no crashear si falta en el XML
    private var tvRamaLabel: TextView? = null
    private var cardSelectorRama: MaterialCardView? = null
    private var tvNombreRamaActual: TextView? = null

    private lateinit var database: AppDB
    private var isUpdating = false

    private var currentCategoriaId: Long = -1L
    private var currentCategoriaNombre: String = ""
    private var currentCategoriaEmoji: String = ""
    private var currentColorHex: String = "#E0E0E0"

    // Variables para la lógica de Ramas (Bolsillos)
    private var currentRamaId: Int? = null
    private var listaRamasActivas: List<RamaActiva> = emptyList()
    private var listaCategoriasEnDB: List<Categoria> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón de Volver
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        movimiento = BundleCompat.getParcelable(requireArguments(), "movimiento", Movimiento::class.java)!!

        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardar)
        val btnEliminar = view.findViewById<MaterialButton>(R.id.btnEliminar)
        etMonto = view.findViewById(R.id.etMonto)

        // UI Ramas
        tvRamaLabel = view.findViewById(R.id.tvRamaLabel)
        cardSelectorRama = view.findViewById(R.id.cardSelectorRama)
        tvNombreRamaActual = view.findViewById(R.id.tvNombreRamaActual)

        // 🛑 APLICAR AURA AL BOTÓN GUARDAR 🛑
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        btnGuardar.backgroundTintList = ColorStateList.valueOf(auraColor)

        cardSelectorCategoria = view.findViewById(R.id.cardSelectorCategoria)
        cardEmojiFondo = view.findViewById(R.id.cardEmojiFondo)
        tvEmojiCategoriaActual = view.findViewById(R.id.tvEmojiCategoriaActual)
        tvNombreCategoriaActual = view.findViewById(R.id.tvNombreCategoriaActual)

        cardEgreso = view.findViewById(R.id.cardEgreso)
        cardIngreso = view.findViewById(R.id.cardIngreso)
        ivEgresoArrow = view.findViewById(R.id.ivEgresoArrow)
        ivIngresoArrow = view.findViewById(R.id.ivIngresoArrow)
        tvEgresoText = view.findViewById(R.id.tvEgresoText)
        tvIngresoText = view.findViewById(R.id.tvIngresoText)

        etDescripcion.setText(movimiento.descripcion)
        etMonto.setText(formatAmount(movimiento.cantidad))

        currentCategoriaId = movimiento.categoriaId
        currentCategoriaNombre = movimiento.categoria
        currentRamaId = movimiento.parentId // Recuperamos el bolsillo actual asignado al movimiento

        tipoMovimientoSeleccionado = movimiento.tipo
        actualizarEstiloBotones()

        cardEgreso.setOnClickListener {
            tipoMovimientoSeleccionado = 0
            actualizarEstiloBotones()
        }

        cardIngreso.setOnClickListener {
            tipoMovimientoSeleccionado = 1
            actualizarEstiloBotones()
        }

        cardSelectorCategoria.setOnClickListener {
            mostrarBottomSheetCategorias()
        }

        cardSelectorRama?.setOnClickListener {
            mostrarSelectorRamas()
        }

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        database = AppDB.getDatabase(requireContext(), userEmail)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

        cargarCategorias()
        cargarRamasActivas()

        etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val desc = s.toString()
                if (desc.isNotEmpty()) {
                    val sugerenciaNombre = CategorySuggester.suggestCategory(desc)
                    if (sugerenciaNombre != null) {
                        val categoriaEncontrada = listaCategoriasEnDB.find { it.nombre == sugerenciaNombre }
                        if (categoriaEncontrada != null) {
                            currentCategoriaId = categoriaEncontrada.id
                            currentCategoriaNombre = categoriaEncontrada.nombre
                            currentCategoriaEmoji = categoriaEncontrada.emoji
                            currentColorHex = categoriaEncontrada.colorHex
                            actualizarUISeleccionCategoria()
                        }
                    }
                }
            }
        })

        etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (isUpdating) return
                isUpdating = true
                val text = editable.toString()
                val cleanString = text.replace(".", "").replace(",", "")

                if (cleanString.isNotEmpty()) {
                    try {
                        val parsed = cleanString.toLong()
                        val symbols = DecimalFormatSymbols(Locale("es", "CO"))
                        symbols.groupingSeparator = '.'
                        symbols.decimalSeparator = ','
                        val localFormatter = DecimalFormat("#,##0", symbols)
                        val formatted = localFormatter.format(parsed)
                        etMonto.setText(formatted)
                        etMonto.setSelection(formatted.length)
                    } catch (e: NumberFormatException) {
                    }
                }
                isUpdating = false
            }
        })

        btnGuardar.setOnClickListener {
            val descripcionTexto = etDescripcion.text.toString()
            val montoTextoLimpio = etMonto.text.toString().replace(".", "").replace(",", ".")
            val nuevaCantidad: Double

            if (descripcionTexto.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, introduce una descripción.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                nuevaCantidad = montoTextoLimpio.toDouble()
                if (nuevaCantidad <= 0) {
                    Toast.makeText(requireContext(), "El monto debe ser un valor positivo.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Por favor, introduce un monto válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validaciones de Bolsillos (Ramas)
            if (tipoMovimientoSeleccionado == 0 && currentRamaId == null) {
                Toast.makeText(requireContext(), "⚠️ Por favor selecciona de dónde saldrá el dinero", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (tipoMovimientoSeleccionado == 0 && currentRamaId != null && currentRamaId != -1) {
                val ramaSeleccionada = listaRamasActivas.find { it.id == currentRamaId }
                if (ramaSeleccionada != null && nuevaCantidad > ramaSeleccionada.saldo) {
                    Toast.makeText(requireContext(), "⚠️ Saldo insuficiente en el bolsillo seleccionado", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            val finalParentId = if (tipoMovimientoSeleccionado == 0 && currentRamaId != -1) currentRamaId else null

            val actualizado = movimiento.copy(
                descripcion = descripcionTexto,
                cantidad = nuevaCantidad,
                tipo = tipoMovimientoSeleccionado,
                categoria = currentCategoriaNombre,
                categoriaId = currentCategoriaId,
                parentId = finalParentId
            )
            viewModel.actualizar(actualizado)
            Toast.makeText(requireContext(), "Movimiento actualizado", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        btnEliminar.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null)
            val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnCancelarEliminar = dialogView.findViewById<Button>(R.id.btnCancelarEliminar)
            val btnConfirmarEliminar = dialogView.findViewById<Button>(R.id.btnConfirmarEliminar)

            btnCancelarEliminar.setOnClickListener { dialog.dismiss() }

            btnConfirmarEliminar.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    database.movimientoDao().eliminar(movimiento)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        findNavController().popBackStack()
                    }
                }
            }
            dialog.show()
        }
    }

    // Helper matemático para ordenar fechas de manera robusta
    private fun parsearFecha(fechaStr: String?): Date {
        if (fechaStr.isNullOrBlank()) return Date(0)

        val fechaLimpia = fechaStr.trim()
        val formatos = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd"
        )
        for (formato in formatos) {
            try {
                val sdf = SimpleDateFormat(formato, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(fechaLimpia)
                if (date != null) return date
            } catch (e: Exception) { }
        }
        return Date(0)
    }

    private fun cargarRamasActivas() {
        lifecycleScope.launch(Dispatchers.IO) {
            val todosLosMovimientos = database.movimientoDao().obtenerTodosSync()

            val balancePorRama = todosLosMovimientos.groupBy { it.parentId ?: it.id }
                .mapValues { entry ->
                    entry.value.sumOf { if (it.tipo == 1) it.cantidad else -it.cantidad }
                }

            val ramasActivasTemp = mutableListOf<RamaActiva>()
            balancePorRama.forEach { (ramaId, saldo) ->
                // ¡CIRUGÍA DE PRECISIÓN!
                // Si estamos editando un gasto que ya descontó dinero de esta rama,
                // debemos "devolverle" temporalmente el dinero al saldo disponible para la validación visual y transaccional.
                var saldoAjustado = saldo
                if (ramaId == movimiento.parentId && movimiento.tipo == 0) {
                    saldoAjustado += movimiento.cantidad
                }

                if (saldoAjustado > 0 || ramaId == movimiento.parentId) {
                    val ramaPadre = todosLosMovimientos.find { it.id == ramaId }
                    if (ramaPadre != null) {
                        val tituloConFecha = "${ramaPadre.descripcion} (${ramaPadre.fecha})"
                        val fechaMillis = parsearFecha(ramaPadre.fecha).time
                        ramasActivasTemp.add(RamaActiva(ramaId, tituloConFecha, saldoAjustado, fechaMillis))
                    }
                }
            }

            withContext(Dispatchers.Main) {
                listaRamasActivas = ramasActivasTemp.sortedByDescending { it.fechaMillis }

                // Setear el nombre inicial en la UI
                if (currentRamaId != null && currentRamaId != -1) {
                    val ramaActual = listaRamasActivas.find { it.id == currentRamaId }
                    if (ramaActual != null) {
                        val format = DecimalFormat("$#,###", DecimalFormatSymbols(Locale("es", "CO")))
                        tvNombreRamaActual?.text = "${ramaActual.nombre} (${format.format(ramaActual.saldo)})"
                    } else {
                        tvNombreRamaActual?.text = "Gasto Libre"
                        currentRamaId = -1
                    }
                } else if (currentRamaId == -1) {
                    tvNombreRamaActual?.text = "Gasto Libre"
                } else {
                    tvNombreRamaActual?.text = "Selecciona el origen"
                }
            }
        }
    }

    private fun mostrarSelectorRamas() {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_ramas, null)
        dialog.setContentView(view)

        val llListaRamas = view.findViewById<LinearLayout>(R.id.llListaRamas)
        val format = DecimalFormat("$#,###", DecimalFormatSymbols(Locale("es", "CO")))

        val cantidadTexto = etMonto.text.toString().replace(".", "").replace(",", ".")
        val montoIngresado = cantidadTexto.toDoubleOrNull() ?: 0.0

        val viewLibre = LayoutInflater.from(requireContext()).inflate(R.layout.item_rama_selector, llListaRamas, false)
        val tvNombreLibre = viewLibre.findViewById<TextView>(R.id.tvRamaNombre)
        val tvSaldoLibre = viewLibre.findViewById<TextView>(R.id.tvRamaSaldo)
        val ivIconLibre = viewLibre.findViewById<ImageView>(R.id.ivRamaIcon)

        tvNombreLibre.text = "Gasto Libre / Dinero Extra"
        tvSaldoLibre.text = "No se descontará de ningún bolsillo"
        ivIconLibre.setImageResource(R.drawable.ic_info)
        ivIconLibre.setColorFilter(Color.GRAY)

        viewLibre.setOnClickListener {
            currentRamaId = -1
            tvNombreRamaActual?.text = "Gasto Libre"
            dialog.dismiss()
        }
        llListaRamas.addView(viewLibre)

        if (listaRamasActivas.isNotEmpty()) {
            val tvTituloDisponibles = TextView(requireContext()).apply {
                text = "Tus Bolsillos Disponibles"
                setPadding(16, 24, 16, 8)
                textSize = 14f
                setTextColor(Color.GRAY)
            }
            llListaRamas.addView(tvTituloDisponibles)

            listaRamasActivas.forEach { rama ->
                val viewRama = LayoutInflater.from(requireContext()).inflate(R.layout.item_rama_selector, llListaRamas, false)
                val tvNombre = viewRama.findViewById<TextView>(R.id.tvRamaNombre)
                val tvSaldo = viewRama.findViewById<TextView>(R.id.tvRamaSaldo)
                val ivIcon = viewRama.findViewById<ImageView>(R.id.ivRamaIcon)

                tvNombre.text = rama.nombre

                if (montoIngresado > rama.saldo) {
                    tvSaldo.text = "Disponible: ${format.format(rama.saldo)} (Insuficiente)"
                    val colorError = Color.parseColor("#D32F2F")
                    tvNombre.setTextColor(colorError)
                    tvSaldo.setTextColor(colorError)
                    ivIcon?.setColorFilter(colorError)

                    viewRama.setOnClickListener {
                        Toast.makeText(requireContext(), "Saldo insuficiente en este bolsillo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    tvSaldo.text = "Disponible: ${format.format(rama.saldo)}"
                    viewRama.setOnClickListener {
                        currentRamaId = rama.id
                        tvNombreRamaActual?.text = "${rama.nombre} (${format.format(rama.saldo)})"
                        dialog.dismiss()
                    }
                }

                llListaRamas.addView(viewRama)
            }
        }
        dialog.show()
    }

    private fun cargarCategorias() {
        lifecycleScope.launch(Dispatchers.IO) {
            database.categoriaDao().obtenerTodas().collect { lista ->
                val nombresEnDB = lista.map { it.nombre }
                val faltantes = CategorySuggester.getDefaultCategories().filter { it.nombre !in nombresEnDB }
                faltantes.forEach { database.categoriaDao().insertar(it) }

                withContext(Dispatchers.Main) {
                    if (faltantes.isEmpty()) {
                        listaCategoriasEnDB = lista
                        val categoriaActual = lista.find { it.id == currentCategoriaId }
                        if (categoriaActual != null) {
                            currentCategoriaEmoji = categoriaActual.emoji
                            currentCategoriaNombre = categoriaActual.nombre
                            currentColorHex = categoriaActual.colorHex
                        }
                        actualizarUISeleccionCategoria()
                    }
                }
            }
        }
    }

    private fun actualizarUISeleccionCategoria() {
        if (currentCategoriaId == -1L || currentCategoriaNombre.isEmpty()) {
            tvEmojiCategoriaActual.text = "❓"
            tvNombreCategoriaActual.text = "Selecciona una categoría"
            cardEmojiFondo.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
        } else {
            tvEmojiCategoriaActual.text = currentCategoriaEmoji
            tvNombreCategoriaActual.text = currentCategoriaNombre
            try {
                val colorOriginal = Color.parseColor(currentColorHex)
                val colorOpaco = Color.argb(40, Color.red(colorOriginal), Color.green(colorOriginal), Color.blue(colorOriginal))
                cardEmojiFondo.setCardBackgroundColor(colorOpaco)
            } catch (e: Exception) {
                cardEmojiFondo.setCardBackgroundColor(Color.LTGRAY)
            }
        }
    }

    private fun mostrarBottomSheetCategorias() {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_categories, null)
        dialog.setContentView(view)

        val rvCategorias = view.findViewById<RecyclerView>(R.id.rvCategorias)
        val btnCrear = view.findViewById<MaterialButton>(R.id.btnCrearCategoria)

        // 🛑 APLICAR AURA AL BOTÓN DE CREAR CATEGORÍA 🛑
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        btnCrear.backgroundTintList = ColorStateList.valueOf(auraColor)

        val adapter = CategoryAdapter(
            categorias = listaCategoriasEnDB,
            onCategoryClick = { categoriaSeleccionada ->
                currentCategoriaId = categoriaSeleccionada.id
                currentCategoriaNombre = categoriaSeleccionada.nombre
                currentCategoriaEmoji = categoriaSeleccionada.emoji
                currentColorHex = categoriaSeleccionada.colorHex
                actualizarUISeleccionCategoria()
                dialog.dismiss()
            },
            onCategoryLongClick = { cat ->
                if (cat.esPersonalizada) {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Gestionar ${cat.nombre}")
                        .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                            if (which == 0) {
                                dialog.dismiss()
                                mostrarDialogoCrearCategoria(cat)
                            } else {
                                android.app.AlertDialog.Builder(requireContext())
                                    .setTitle("¿Eliminar categoría?")
                                    .setMessage("Esta acción no se puede deshacer.")
                                    .setPositiveButton("Eliminar") { _, _ ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            database.categoriaDao().eliminar(cat)
                                            withContext(Dispatchers.Main) {
                                                dialog.dismiss()
                                                Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            }
                        }.show()
                } else {
                    Toast.makeText(requireContext(), "Las categorías del sistema no se pueden modificar", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvCategorias.adapter = adapter
        btnCrear.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoCrearCategoria()
        }
        dialog.show()
    }

    private fun mostrarDialogoSelectorEmoji(onEmojiSelected: (String) -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_emoji_picker, null)
        dialog.setContentView(view)

        val rvEmojis = view.findViewById<RecyclerView>(R.id.rvEmojis)
        val emojisStr = "😀 😃 😄 😁 😆 😅 😂 🤣 🥲 ☺️ 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🫣 🤭 🤫 🤥 😶 😶‍🌫️ 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 😵‍💫 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 👹 👺 🤡 💩 👻 💀 ☠️ 👽 👾 🤖 🎃 😺 😸 😹 😻 😼 😽 🙀 😿 😾 👋 🤚 🖐 ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🫰 🤟 🤘 🤙 👈 👉 👆 🖕 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✍️ 💅 🤳 💪 🦾 🦿 🦵 🦶 👂 🦻 👃 🫀 🫁 🧠 🦷 🦴 👀 👁 👅 👄 💋 🩸 🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐻‍❄️ 🐨 🐯 🦁 🐮 🐷 🐽 🐸 🐵 🙈 🙉 🙊 🐒 🐔 🐧 🐦 🐤 🐣 🐥 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🪱 🐛 🦋 🐌 🐞 🐜 🪰 🪲 🪳 🦟 🦗 🕷 🦂 🐢 🐍 🦎 🦖 🦕 🐙 🦑 🦐 🦞 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🦭 🐊 🐅 🐆 🦓 🦍 🦧 🦣 🐘 🦛 🦏 🐪 🐫 🦒 🦘 🐃 🐂 🐄 🐎 🐖 🐏 🐑 🦙 🐐 🦌 🐕 🐩 🦮 🐕‍🦺 🐈 🐈‍⬛ 🪶 🐓 🦃 🦤 🦚 🦜 🦢 🦩 🕊 🐇 🦝 🦨 🦡 🦫 🦦 🦥 🐁 🐀 🐿 🦔 🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶 🫑 🌽 🥕 🫒 🧄 🧅 🥔 🍠 🥐 🥯 🍞 🥖 🥨 🧀 🥚 🍳 🧈 🥞 🧇 🥓 🥩 🍗 🍖 🦴 🌭 🍔 🍟 🍕 🫓 🥪 🥙 🧆 🌮 🌯 🫔 🥗 🥘 🫕 🥫 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🦪 🍤 🍙 🍚 🍘 🍥 🥠 🥮 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🌰 🥜 🍯 🥛 🍼 🫖 ☕️ 🍵 🧃 🥤 🧋 🚰 🍺 🍻 🥂 🍷 🥃 🍸 🍹 🧉 🍾 🧊 🥄 🍴 🍽 🥣 🥡 🥢 🧂 🚗 🚕 🚙 🚌 🚎 🏎 🚓 🚑 🚒 🚐 🛻 🚚 🚛 🚜 🦯 🦽 🦼 🛴 🚲 🛵 🏍 🛺 🚨 🚔 🚍 🚘 🚖 🚡 🚠 🚟 🚃 🚋 🚞 🚝 🚄 🚅 🚈 🚂 🚆 🚇 🚊 🚉 ✈️ 🛫 🛬 🛩 💺 🛰 🚀 🛸 🚁 🛶 ⛵️ 🚤 🛥 🛳 ⛴ 🚢 ⚓️ 🪝 ⛽️ 🚧 🚦 🚥 🚏 🗺 🗿 🗽 🗼 🏰 🏯 🏟 🎡 🎢 🎠 ⛲️ ⛱ 🏖 🏝 🏜 🌋 ⛰ 🏔 🗻 🏕 ⛺️ 🛖 🏠 🏡 🏘 🏚 🏗 🏭 🏢 🏬 🏣 🏤 🏥 🏦 🏨 🏪 🏫 🏩 💒 🏛 ⛪️ 🕌 🕍 🛕 🕋 ⛩ 🛤 🛣 🗾 🎑 🏞 🌅 🌄 🌠 🎇 🎆 🌇 🌆 🏙 🌃 🌌 🌉 🌁 ⌚️ 📱 📲 💻 ⌨️ 🖥 🖨 🖱 🖲 🕹 🗜 💽 💾 💿 📀 📼 📷 📸 📹 🎥 📽 🎞 📞 ☎️ 📟 📠 📺 📻 🎙 🎚 🎛 🧭 ⏱ ⏲ ⏰ 🕰 ⌛️ ⏳ 📡 🔋 🔌 💡 🔦 🕯 🪔 🧯 🛢 💸 💵 💴 💶 💷 🪙 💰 💳 💎 ⚖️ 🪜 🧰 🪛 🔧 🔨 ⚒ 🛠 ⛏ 🪚 🔩 ⚙️ 🪤 🧱 ⛓ 🧲 🔫 💣 🧨 🪓 🔪 🗡 ⚔️ 🛡 🚬 ⚰️ 🪦 ⚱️ 🏺 🔮 📿 🧿 💈 ⚗️ 🔭 🔬 🕳 🩹 🩺 💊 💉 🩸 🧬 🦠 🧫 🧪 🌡 🧹 🪠 🧺 🧻 🚽 🚰 🚿 🛁 🛀 🧼 🪥 🧽 🧴 🛎 🔑 🗝 🚪 🪑 🛋 🛏 🛌 🧸 🪆 🖼 🪞 🪟 🛍 🛒 🎁 🎈 🎏 🎀 🪄 🪅 🎊 🎉 🎎 🏮 🎐 🧧 ✉️ 📩 📨 📧 💌 📥 📤 📦 🏷 🪧 📪 📫 📬 📭 📮 📯 📜 📃 📄 📑 🧾 📊 📈 📉 🗒 🗓 📆 📅 🗑 📇 🗃 🗳 🗄 📋 📁 📂 🗂 🗞 📰 📓 📔 📒 📕 📗 📘 📙 📚 📖 🔖 🧷 🔗 📎 🖇 📐 📏 🧮 📌 📍 ✂️ 🖊 🖋 ✒️ 🖌 🖍 📝 ✏️ 🔍 🔎 🔏 🔐 🔒 🔓 ❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ✝️ ☪️ 🕉 ☸️ ✡️ 🔯 🕎 ☯️ ☦️ 🛐 ⛎ ♈️ ♉️ ♊️ ♋️ ♌️ ♍️ ♎️ ♏️ ♐️ ♑️ ♒️ ♓️ 🆔 ⚛️ 🉑 ☢️ ☣️ 📴 📳 🈶 🈚️ 🈸 🈺 🈷️ ✴️ 🆚 💮 🉐 ㊙️ ㊗️ 🈴 🈵 🈹 🈲 🅰️ 🅱️ 🆎 🆑 🅾️ 🆘 ❌ ⭕️ 🛑 ⛔️ 📛 🚫 💯 💢 ♨️ 🚷 🚯 🚳 🚱 🔞 📵 🚭 ❗️ ❕ ❓ ❔ ‼️ ⁉️ 🔅 🔆 〽️ ⚠️ 🚸 🔱 ⚜️ 🔰 ♻️ ✅ 🈯️ 💹 ❇️ ✳️ ❎ 🌐 💠 Ⓜ️ 🌀 💤 🏧 🚾 ♿️ 🅿️ 🛗 🈳 🈂️ 🛂 🛃 🛄 🛅 🚹 🚺 🚼 ⚧ 🚻 🚮 🎦 🚰 ℹ️ 🔤 🔡 🔠 🔣 🎵 🎶 〰️ ➰ ✔️ 🔃 ➕ ➖ ➗ ✖️ ♾ ©️ ®️ ™️"
        val emojisList = emojisStr.split(" ").filter { it.isNotBlank() }

        val adapter = EmojiAdapter(emojisList) { emoji ->
            onEmojiSelected(emoji)
            dialog.dismiss()
        }
        rvEmojis.adapter = adapter
        dialog.show()
    }

    private fun mostrarDialogoPaletaColores(onColorSelected: (String) -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_color_picker, null)
        dialog.setContentView(view)

        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayoutColors)
        val etHex = view.findViewById<TextInputEditText>(R.id.etHexColor)
        val btnAplicar = view.findViewById<MaterialButton>(R.id.btnAplicarColorHex)

        // 🛑 APLICAR AURA AL BOTÓN APLICAR COLOR 🛑
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        btnAplicar.backgroundTintList = ColorStateList.valueOf(auraColor)

        val extendedPalette = listOf(
            "#F44336", "#E57373", "#D32F2F", "#B71C1C", "#E91E63", "#F06292", "#C2185B", "#880E4F",
            "#9C27B0", "#BA68C8", "#7B1FA2", "#4A148C", "#673AB7", "#9575CD", "#512DA8", "#311B92",
            "#3F51B5", "#7986CB", "#303F9F", "#1A237E", "#2196F3", "#64B5F6", "#1976D2", "#0D47A1",
            "#03A9F4", "#4FC3F7", "#0288D1", "#01579B", "#00BCD4", "#4DD0E1", "#0097A7", "#006064",
            "#009688", "#4DB6AC", "#00796B", "#004D40", "#4CAF50", "#81C784", "#388E3C", "#1B5E20",
            "#8BC34A", "#AED581", "#689F38", "#33691E", "#CDDC39", "#DCE775", "#AFB42B", "#827717",
            "#FFEB3B", "#FFF176", "#FBC02D", "#F57F17", "#FFC107", "#FFD54F", "#FFA000", "#FF6F00",
            "#FF9800", "#FFB74D", "#F57C00", "#E65100", "#FF5722", "#FF8A65", "#E64A19", "#BF360C",
            "#795548", "#A1887F", "#5D4037", "#3E2723", "#9E9E9E", "#E0E0E0", "#616161", "#212121",
            "#607D8B", "#90A4AE", "#455A64", "#263238", "#000000", "#FFFFFF"
        )

        extendedPalette.forEach { colorHex ->
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 110
                    height = 110
                    setMargins(12, 12, 12, 12)
                }
                radius = 55f
                setCardBackgroundColor(Color.parseColor(colorHex))
                strokeWidth = 2
                strokeColor = Color.parseColor("#40000000")
                isClickable = true
                setOnClickListener {
                    onColorSelected(colorHex)
                    dialog.dismiss()
                }
            }
            gridLayout.addView(card)
        }

        btnAplicar.setOnClickListener {
            val input = etHex.text.toString().trim()
            try {
                Color.parseColor(input)
                onColorSelected(input.uppercase())
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "HEX inválido. Usa formato #RRGGBB", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoCrearCategoria(categoriaAEditar: Categoria? = null) {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_create_category, null)
        dialog.setContentView(view)

        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombreCategoria)
        val llColorSelector = view.findViewById<LinearLayout>(R.id.llColorSelector)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardarCategoriaNueva)

        // 🛑 APLICAR AURA AL BOTÓN GUARDAR CATEGORÍA 🛑
        val auraColor = ThemeUtils.getAuraColor(requireContext())
        btnGuardar.backgroundTintList = ColorStateList.valueOf(auraColor)

        val cardLivePreview = view.findViewById<MaterialCardView>(R.id.cardLivePreview)
        val tvLivePreviewEmoji = view.findViewById<TextView>(R.id.tvLivePreviewEmoji)

        var emojiSeleccionado = "📦"
        var colorSeleccionado = "#9E9E9E"
        var vistaSeleccionada: MaterialCardView? = null

        cardLivePreview.setCardBackgroundColor(Color.argb(40, Color.red(Color.GRAY), Color.green(Color.GRAY), Color.blue(Color.GRAY)))

        cardLivePreview.setOnClickListener {
            mostrarDialogoSelectorEmoji { emoji ->
                emojiSeleccionado = emoji
                tvLivePreviewEmoji.text = emoji
            }
        }

        val paletaRapida = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#00BCD4", "#4CAF50", "#FFEB3B", "#FF9800", "#FF5722", "#795548", "#607D8B", "#9E9E9E")

        llColorSelector.removeAllViews()

        paletaRapida.forEach { colorHex ->
            val colorView = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(8, 8, 8, 8) }
                radius = 60f
                setCardBackgroundColor(Color.parseColor(colorHex))
                strokeWidth = 0
                isClickable = true
                setOnClickListener {
                    vistaSeleccionada?.strokeWidth = 0
                    strokeWidth = 10
                    strokeColor = Color.BLACK
                    vistaSeleccionada = this
                    colorSeleccionado = colorHex

                    val original = Color.parseColor(colorHex)
                    cardLivePreview.setCardBackgroundColor(Color.argb(40, Color.red(original), Color.green(original), Color.blue(original)))
                }
            }
            llColorSelector.addView(colorView)
        }

        val btnCustomColor = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(8, 8, 24, 8) }
            radius = 60f
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = 4
            strokeColor = Color.LTGRAY
            isClickable = true

            val icon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_add)
                setColorFilter(Color.GRAY)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }
            addView(icon)

            setOnClickListener {
                mostrarDialogoPaletaColores { nuevoHex ->
                    colorSeleccionado = nuevoHex
                    vistaSeleccionada?.strokeWidth = 0

                    val original = Color.parseColor(nuevoHex)
                    cardLivePreview.setCardBackgroundColor(Color.argb(40, Color.red(original), Color.green(original), Color.blue(original)))
                }
            }
        }
        llColorSelector.addView(btnCustomColor)

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor escribe un nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevaCat = Categoria(
                nombre = nombre,
                emoji = emojiSeleccionado,
                colorHex = colorSeleccionado,
                esPersonalizada = true
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val newId = database.categoriaDao().insertar(nuevaCat)
                withContext(Dispatchers.Main) {
                    currentCategoriaId = newId
                    currentCategoriaNombre = nombre
                    currentCategoriaEmoji = emojiSeleccionado
                    currentColorHex = colorSeleccionado
                    actualizarUISeleccionCategoria()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Categoría creada", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun formatAmount(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("es", "CO"))
        symbols.groupingSeparator = '.'
        symbols.decimalSeparator = ','

        if (amount == amount.toLong().toDouble()) {
            val integerFormatter = DecimalFormat("#,##0", symbols)
            return integerFormatter.format(amount)
        }

        val formatter = DecimalFormat("#,##0.##", symbols)
        return formatter.format(amount)
    }

    private fun actualizarEstiloBotones() {
        val context = requireContext()

        val colorRojoPuro = ContextCompat.getColor(context, R.color.transaction_expense)
        val colorVerdePuro = ContextCompat.getColor(context, R.color.transaction_income)

        val colorRojoPastel = ContextCompat.getColor(context, R.color.transaction_expense_pastel)
        val colorVerdePastel = ContextCompat.getColor(context, R.color.transaction_income_pastel)

        val colorBordeInactivo = ContextCompat.getColor(context, R.color.stroke_inactive)

        @ColorInt val colorSuperficie = ContextCompat.getColor(context, R.color.surface_card)

        cardEgreso.setCardBackgroundColor(colorSuperficie)
        cardEgreso.strokeColor = colorBordeInactivo
        cardEgreso.strokeWidth = 1
        ivEgresoArrow.setColorFilter(colorRojoPuro)
        tvEgresoText.setTextColor(colorRojoPuro)

        cardIngreso.setCardBackgroundColor(colorSuperficie)
        cardIngreso.strokeColor = colorBordeInactivo
        cardIngreso.strokeWidth = 1
        ivIngresoArrow.setColorFilter(colorVerdePuro)
        tvIngresoText.setTextColor(colorVerdePuro)

        if (tipoMovimientoSeleccionado == 0) {
            // Estilo Egreso
            cardEgreso.setCardBackgroundColor(colorRojoPastel)
            cardEgreso.strokeColor = colorRojoPuro
            cardEgreso.strokeWidth = 2

            // Mostrar Ramas
            tvRamaLabel?.visibility = View.VISIBLE
            cardSelectorRama?.visibility = View.VISIBLE
        } else {
            // Estilo Ingreso
            cardIngreso.setCardBackgroundColor(colorVerdePastel)
            cardIngreso.strokeColor = colorVerdePuro
            cardIngreso.strokeWidth = 2

            // Ocultar Ramas
            tvRamaLabel?.visibility = View.GONE
            cardSelectorRama?.visibility = View.GONE
            currentRamaId = null
            tvNombreRamaActual?.text = "Selecciona el origen"
        }
    }
}