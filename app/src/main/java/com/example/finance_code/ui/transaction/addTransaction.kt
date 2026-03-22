package com.example.finance_code.ui.transaction

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.FinanceWidgetProvider
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Categoria
import com.example.finance_code.data.EXTRA_WIDGET_TRANSACTION_TYPE
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.utils.ThemeUtils // Importe crucial
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
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

class addTransaction : AppCompatActivity() {

    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etMonto: TextInputEditText
    private lateinit var cardIngreso: MaterialCardView
    private lateinit var cardEgreso: MaterialCardView

    private lateinit var cardSelectorCategoria: MaterialCardView
    private lateinit var cardEmojiFondo: MaterialCardView
    private lateinit var tvEmojiCategoriaActual: TextView
    private lateinit var tvNombreCategoriaActual: TextView

    private lateinit var database: AppDB

    private var tipoSeleccionado = -1
    private var isUpdating = false

    private var currentCategoriaId: Long = -1L
    private var currentCategoriaNombre: String = ""
    private var currentCategoriaEmoji: String = ""
    private var currentColorHex: String = "#E0E0E0"

    private var listaCategoriasEnDB: List<Categoria> = emptyList()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val speechResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = speechResult?.get(0) ?: ""
            procesarTextoVoz(spokenText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)

        val viewModel: MovimientoViewModel

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        etDescripcion = findViewById(R.id.etDescripcion)
        etMonto = findViewById(R.id.etMonto)
        cardIngreso = findViewById(R.id.cardIngreso)
        cardEgreso = findViewById(R.id.cardEgreso)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)
        val btnVoiceInput = findViewById<ImageButton>(R.id.btnVoiceInput)

        // 🛑 APLICAR AURA AL BOTÓN PRINCIPAL DE GUARDAR 🛑
        val auraColor = ThemeUtils.getAuraColor(this)
        btnGuardar.backgroundTintList = ColorStateList.valueOf(auraColor)

        cardSelectorCategoria = findViewById(R.id.cardSelectorCategoria)
        cardEmojiFondo = findViewById(R.id.cardEmojiFondo)
        tvEmojiCategoriaActual = findViewById(R.id.tvEmojiCategoriaActual)
        tvNombreCategoriaActual = findViewById(R.id.tvNombreCategoriaActual)

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = AppDB.getDatabase(this, userEmail)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

        cargarCategorias()

        cardIngreso.setOnClickListener { seleccionarTipo(1) }
        cardEgreso.setOnClickListener { seleccionarTipo(0) }

        cardSelectorCategoria.setOnClickListener { mostrarBottomSheetCategorias() }

        val transactionTypeFromWidget = intent.getIntExtra(EXTRA_WIDGET_TRANSACTION_TYPE, -1)
        if (transactionTypeFromWidget != -1) {
            seleccionarTipo(if (transactionTypeFromWidget == 1) 1 else 0)
        } else {
            seleccionarTipo(0)
        }

        toolbar.setNavigationOnClickListener { finish() }
        btnVoiceInput.setOnClickListener { startVoiceInput() }

        etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val desc = s.toString()
                if (desc.isNotEmpty()) {
                    val descLower = desc.lowercase()
                    var categoriaEncontrada: Categoria? = null

                    categoriaEncontrada = listaCategoriasEnDB.find { cat ->
                        cat.esPersonalizada && cat.palabrasClave.isNotBlank() && cat.palabrasClave.split(",").any { palabra ->
                            val p = palabra.trim().lowercase()
                            p.isNotEmpty() && Regex("\\b$p(s|es)?\\b").containsMatchIn(descLower)
                        }
                    }

                    if (categoriaEncontrada == null) {
                        val sugerenciaNombre = CategorySuggester.suggestCategory(desc)
                        if (sugerenciaNombre != null) {
                            categoriaEncontrada = listaCategoriasEnDB.find { it.nombre == sugerenciaNombre }
                        }
                    }

                    if (categoriaEncontrada != null) {
                        currentCategoriaId = categoriaEncontrada.id
                        currentCategoriaNombre = categoriaEncontrada.nombre
                        currentCategoriaEmoji = categoriaEncontrada.emoji
                        currentColorHex = categoriaEncontrada.colorHex
                        actualizarUISeleccionCategoria()
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
                        val localFormatter = DecimalFormat("#,###", symbols)
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
            val descripcion = etDescripcion.text.toString()
            val cantidadTexto = etMonto.text.toString()

            if (descripcion.isEmpty()) {
                Toast.makeText(this, "⚠️ Ingresa una descripción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cantidadTexto.isEmpty()) {
                Toast.makeText(this, "⚠️ Ingresa una cantidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (tipoSeleccionado == -1) {
                Toast.makeText(this, "⚠️ Selecciona Ingreso o Egreso", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentCategoriaId == -1L) {
                val catOtros = listaCategoriasEnDB.find { it.nombre == "Otros" }
                if (catOtros != null) {
                    currentCategoriaId = catOtros.id
                    currentCategoriaNombre = catOtros.nombre
                } else {
                    currentCategoriaId = 21L
                    currentCategoriaNombre = "Otros"
                }
            }

            val cleanCantidadTexto = cantidadTexto.replace(".", "").replace(",", ".")
            val cantidad = cleanCantidadTexto.toDoubleOrNull()

            if (cantidad == null || cantidad <= 0) {
                Toast.makeText(this, "⚠️ Cantidad inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val nuevoMovimiento = Movimiento(
                descripcion = descripcion,
                cantidad = cantidad,
                tipo = tipoSeleccionado,
                fecha = fechaActual,
                categoria = currentCategoriaNombre,
                categoriaId = currentCategoriaId,
                hora = horaActual
            )

            val job = viewModel.insertar(nuevoMovimiento)
            job.invokeOnCompletion {
                val intentWidget = Intent(applicationContext, FinanceWidgetProvider::class.java)
                intentWidget.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                    ComponentName(applicationContext, FinanceWidgetProvider::class.java)
                )
                intentWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                sendBroadcast(intentWidget)

                runOnUiThread {
                    Toast.makeText(this@addTransaction, "✅ Transacción guardada", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
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
                        actualizarUISeleccionCategoria()
                    }
                }
            }
        }
    }

    private fun actualizarUISeleccionCategoria() {
        if (currentCategoriaId == -1L) {
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
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_categories, null)
        dialog.setContentView(view)

        val rvCategorias = view.findViewById<RecyclerView>(R.id.rvCategorias)
        val btnCrear = view.findViewById<MaterialButton>(R.id.btnCrearCategoria)

        // 🛑 APLICAR AURA AL BOTÓN CREAR 🛑
        val auraColor = ThemeUtils.getAuraColor(this)
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
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Gestionar ${cat.nombre}")
                        .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                            if (which == 0) {
                                dialog.dismiss()
                                mostrarDialogoCrearCategoria(cat)
                            } else {
                                android.app.AlertDialog.Builder(this)
                                    .setTitle("¿Eliminar categoría?")
                                    .setMessage("Esta acción no se puede deshacer.")
                                    .setPositiveButton("Eliminar") { _, _ ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            database.categoriaDao().eliminar(cat)
                                            withContext(Dispatchers.Main) {
                                                dialog.dismiss()
                                                Toast.makeText(this@addTransaction, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                            }
                        }.show()
                } else {
                    Toast.makeText(this, "Las categorías del sistema no se pueden modificar", Toast.LENGTH_SHORT).show()
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
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_emoji_picker, null)
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
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_color_picker, null)
        dialog.setContentView(view)

        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayoutColors)
        val etHex = view.findViewById<TextInputEditText>(R.id.etHexColor)
        val btnAplicar = view.findViewById<MaterialButton>(R.id.btnAplicarColorHex)

        // 🛑 APLICAR AURA AL BOTÓN APLICAR 🛑
        val auraColor = ThemeUtils.getAuraColor(this)
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
            val card = MaterialCardView(this).apply {
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
                Toast.makeText(this, "HEX inválido. Usa formato #RRGGBB", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoCrearCategoria(categoriaAEditar: Categoria? = null) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_create_category, null)
        dialog.setContentView(view)

        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombreCategoria)
        val etPalabrasClave = view.findViewById<TextInputEditText>(R.id.etPalabrasClave)
        val llColorSelector = view.findViewById<LinearLayout>(R.id.llColorSelector)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardarCategoriaNueva)

        // 🛑 APLICAR AURA AL BOTÓN GUARDAR CATEGORÍA 🛑
        val auraColor = ThemeUtils.getAuraColor(this)
        btnGuardar.backgroundTintList = ColorStateList.valueOf(auraColor)

        val cardLivePreview = view.findViewById<MaterialCardView>(R.id.cardLivePreview)
        val tvLivePreviewEmoji = view.findViewById<TextView>(R.id.tvLivePreviewEmoji)

        var emojiSeleccionado = "📦"
        var colorSeleccionado = "#9E9E9E"
        var vistaSeleccionada: MaterialCardView? = null

        if (categoriaAEditar != null) {
            etNombre.setText(categoriaAEditar.nombre)
            etPalabrasClave.setText(categoriaAEditar.palabrasClave)
            emojiSeleccionado = categoriaAEditar.emoji
            colorSeleccionado = categoriaAEditar.colorHex
            btnGuardar.text = "Actualizar Categoría"
        }

        tvLivePreviewEmoji.text = emojiSeleccionado
        try {
            val original = Color.parseColor(colorSeleccionado)
            cardLivePreview.setCardBackgroundColor(Color.argb(40, Color.red(original), Color.green(original), Color.blue(original)))
        } catch (e: Exception) {
            cardLivePreview.setCardBackgroundColor(Color.LTGRAY)
        }

        cardLivePreview.setOnClickListener {
            mostrarDialogoSelectorEmoji { emoji ->
                emojiSeleccionado = emoji
                tvLivePreviewEmoji.text = emoji
            }
        }

        val paletaRapida = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#00BCD4", "#4CAF50", "#FFEB3B", "#FF9800", "#FF5722", "#795548", "#607D8B", "#9E9E9E")

        llColorSelector.removeAllViews()

        paletaRapida.forEach { colorHex ->
            val colorView = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(8, 8, 8, 8) }
                radius = 60f
                setCardBackgroundColor(Color.parseColor(colorHex))

                strokeWidth = if (categoriaAEditar != null && colorHex.equals(colorSeleccionado, ignoreCase = true)) {
                    vistaSeleccionada = this
                    10
                } else 0

                strokeColor = Color.BLACK
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

        val btnCustomColor = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(8, 8, 24, 8) }
            radius = 60f
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = 4
            strokeColor = Color.LTGRAY
            isClickable = true

            val icon = ImageView(this@addTransaction).apply {
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
            val palabras = etPalabrasClave.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Por favor escribe un nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                if (categoriaAEditar == null) {
                    val nuevaCat = Categoria(
                        nombre = nombre,
                        emoji = emojiSeleccionado,
                        colorHex = colorSeleccionado,
                        esPersonalizada = true,
                        palabrasClave = palabras
                    )
                    val newId = database.categoriaDao().insertar(nuevaCat)
                    withContext(Dispatchers.Main) {
                        currentCategoriaId = newId
                        currentCategoriaNombre = nombre
                        currentCategoriaEmoji = emojiSeleccionado
                        currentColorHex = colorSeleccionado
                        actualizarUISeleccionCategoria()

                        cargarCategorias()
                        dialog.dismiss()
                        Toast.makeText(this@addTransaction, "Categoría creada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val catActualizada = categoriaAEditar.copy(
                        nombre = nombre,
                        emoji = emojiSeleccionado,
                        colorHex = colorSeleccionado,
                        palabrasClave = palabras
                    )
                    database.categoriaDao().actualizar(catActualizada)
                    withContext(Dispatchers.Main) {
                        if (currentCategoriaId == catActualizada.id) {
                            currentCategoriaId = catActualizada.id
                            currentCategoriaNombre = catActualizada.nombre
                            currentCategoriaEmoji = catActualizada.emoji
                            currentColorHex = catActualizada.colorHex
                            actualizarUISeleccionCategoria()
                        }

                        cargarCategorias()
                        dialog.dismiss()
                        Toast.makeText(this@addTransaction, "Categoría actualizada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun seleccionarTipo(tipo: Int) {
        tipoSeleccionado = tipo

        val colorIngreso = Color.parseColor("#4CAF50") // Verde
        val colorEgreso = Color.parseColor("#F44336")  // Rojo

        if (tipo == 1) { // Ingreso Seleccionado
            cardIngreso.strokeColor = colorIngreso
            cardIngreso.strokeWidth = 6
            cardIngreso.setCardBackgroundColor(Color.argb(38, Color.red(colorIngreso), Color.green(colorIngreso), Color.blue(colorIngreso)))

            // Egreso Inactivo (Limpio)
            cardEgreso.strokeWidth = 0 // Sin borde
            cardEgreso.setCardBackgroundColor(Color.TRANSPARENT)
        } else { // Egreso Seleccionado
            cardEgreso.strokeColor = colorEgreso
            cardEgreso.strokeWidth = 6
            cardEgreso.setCardBackgroundColor(Color.argb(38, Color.red(colorEgreso), Color.green(colorEgreso), Color.blue(colorEgreso)))

            // Ingreso Inactivo (Limpio)
            cardIngreso.strokeWidth = 0 // Sin borde
            cardIngreso.setCardBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo como: 'Almuerzo 50.000'")

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tu dispositivo no soporta entrada de voz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarTextoVoz(textoOriginal: String) {
        var texto = textoOriginal.lowercase(Locale.getDefault()).trim()
        texto = texto.replace(" pesos", "").replace(" de pesos", "").trim()

        val numerosMap = mapOf(
            "cero" to "0", "un" to "1", "uno" to "1", "una" to "1",
            "dos" to "2", "tres" to "3", "cuatro" to "4", "cinco" to "5",
            "seis" to "6", "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10"
        )
        numerosMap.forEach { (palabra, digito) ->
            texto = texto.replace(Regex("\\b$palabra\\b"), digito)
        }

        val tokenRegex = Regex("""(\d+[.,]?\d*[.,]?\d*|\bmil\b|\bmillones\b|\bmillón\b|\bmillon\b)""")
        val matches = tokenRegex.findAll(texto).toList()

        if (matches.isNotEmpty()) {
            var startIndex = -1
            var priceFound = false

            for (i in matches.indices) {
                val token = matches[i].value
                if (token.matches(Regex("""\d+[.,]?\d*[.,]?\d*"""))) {
                    if (i + 1 < matches.size && matches[i+1].value.matches(Regex("""\bmil\b|\bmillones\b|\bmillón\b|\bmillon\b"""))) {
                        startIndex = matches[i].range.first
                        priceFound = true
                        break
                    }
                }
            }

            if (!priceFound) {
                val lastNumberMatch = matches.lastOrNull { it.value.matches(Regex("""\d+[.,]?\d*[.,]?\d*""")) }
                if (lastNumberMatch != null) {
                    startIndex = lastNumberMatch.range.first
                }
            }

            if (startIndex != -1) {
                var descripcionStr = texto.substring(0, startIndex).trim()
                if (descripcionStr.endsWith(" en")) descripcionStr = descripcionStr.dropLast(3).trim()
                if (descripcionStr.endsWith(" por")) descripcionStr = descripcionStr.dropLast(4).trim()
                if (descripcionStr.endsWith(" de")) descripcionStr = descripcionStr.dropLast(3).trim()
                if (descripcionStr.endsWith(" a")) descripcionStr = descripcionStr.dropLast(2).trim()

                if (descripcionStr.isEmpty()) descripcionStr = "Gasto sin descripción"

                val cantidadStr = texto.substring(startIndex).trim()

                var montoFinal = 0L
                var bloqueActual = 0L

                val tokens = cantidadStr.split(" ", " y ")
                for (token in tokens) {
                    val cleanToken = token.replace(".", "").replace(",", "").trim()

                    if (cleanToken == "millón" || cleanToken == "millones" || cleanToken == "millon") {
                        if (bloqueActual == 0L) bloqueActual = 1L
                        montoFinal += bloqueActual * 1000000L
                        bloqueActual = 0L
                    } else if (cleanToken == "mil") {
                        if (bloqueActual == 0L) bloqueActual = 1L
                        montoFinal += bloqueActual * 1000L
                        bloqueActual = 0L
                    } else {
                        val num = cleanToken.toLongOrNull()
                        if (num != null) {
                            if (montoFinal >= 1000000L && num in 100..999) {
                                bloqueActual += num * 1000L
                            } else {
                                bloqueActual += num
                            }
                        }
                    }
                }
                montoFinal += bloqueActual

                if (montoFinal > 0) {
                    etMonto.setText(montoFinal.toString())
                    etDescripcion.setText(descripcionStr.replaceFirstChar { it.uppercase() })
                } else {
                    etDescripcion.setText(textoOriginal.replaceFirstChar { it.uppercase() })
                    Toast.makeText(this, "No detecté un monto válido", Toast.LENGTH_SHORT).show()
                }
            } else {
                etDescripcion.setText(textoOriginal.replaceFirstChar { it.uppercase() })
                Toast.makeText(this, "No detecté ningún número", Toast.LENGTH_SHORT).show()
            }
        } else {
            etDescripcion.setText(textoOriginal.replaceFirstChar { it.uppercase() })
            Toast.makeText(this, "No detecté ningún número", Toast.LENGTH_SHORT).show()
        }
    }
}