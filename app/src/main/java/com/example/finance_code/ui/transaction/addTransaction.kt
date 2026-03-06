package com.example.finance_code.ui.transaction

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.finance_code.FinanceWidgetProvider
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.EXTRA_WIDGET_TRANSACTION_TYPE
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.data.TYPE_EGRESO
import com.example.finance_code.data.TYPE_INGRESO
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
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

    private var tipoSeleccionado = -1
    private var isUpdating = false

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

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val database = AppDB.getDatabase(this, userEmail)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

        cardIngreso.setOnClickListener { seleccionarTipo(1) }
        cardEgreso.setOnClickListener { seleccionarTipo(0) }

        val transactionTypeFromWidget = intent.getIntExtra(EXTRA_WIDGET_TRANSACTION_TYPE, -1)
        if (transactionTypeFromWidget != -1) {
            seleccionarTipo(if (transactionTypeFromWidget == TYPE_INGRESO) 1 else 0)
        } else {
            seleccionarTipo(0)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnVoiceInput.setOnClickListener {
            startVoiceInput()
        }

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

            val cleanCantidadTexto = cantidadTexto.replace(".", "").replace(",", ".")
            val cantidad = cleanCantidadTexto.toDoubleOrNull()

            if (cantidad == null || cantidad <= 0) {
                Toast.makeText(this, "⚠️ Cantidad inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val nuevoMovimiento = Movimiento(
                descripcion = descripcion,
                cantidad = cantidad,
                tipo = tipoSeleccionado,
                fecha = fechaActual,
                categoria = ""
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

    private fun seleccionarTipo(tipo: Int) {
        tipoSeleccionado = tipo

        val colorIngreso = Color.parseColor("#4CAF50")
        val colorEgreso = Color.parseColor("#F44336")
        val colorInactive = Color.parseColor("#808080")

        if (tipo == 1) {
            cardIngreso.strokeColor = colorIngreso
            cardIngreso.strokeWidth = 6
            cardIngreso.setCardBackgroundColor(Color.argb(38, Color.red(colorIngreso), Color.green(colorIngreso), Color.blue(colorIngreso)))

            cardEgreso.strokeColor = colorInactive
            cardEgreso.strokeWidth = 2
            cardEgreso.setCardBackgroundColor(Color.TRANSPARENT)
        } else {
            cardEgreso.strokeColor = colorEgreso
            cardEgreso.strokeWidth = 6
            cardEgreso.setCardBackgroundColor(Color.argb(38, Color.red(colorEgreso), Color.green(colorEgreso), Color.blue(colorEgreso)))

            cardIngreso.strokeColor = colorInactive
            cardIngreso.strokeWidth = 2
            cardIngreso.setCardBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo como: 'Almuerzo 50000'")

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tu dispositivo no soporta entrada de voz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarTextoVoz(texto: String) {
        val regex = Regex("([0-9][0-9.,]*)$")
        val matchResult = regex.find(texto)

        if (matchResult != null) {
            val rawNumber = matchResult.value
            val cleanNumber = rawNumber.replace(".", "").replace(",", "")

            val descripcionStr = texto.substring(0, matchResult.range.first).trim()

            etMonto.setText(cleanNumber)

            if (descripcionStr.isNotEmpty()) {
                etDescripcion.setText(descripcionStr.replaceFirstChar { it.uppercase() })
            } else {
                etDescripcion.setText("Gasto sin descripción")
            }
        } else {
            etDescripcion.setText(texto.replaceFirstChar { it.uppercase() })
            Toast.makeText(this, "No detecté un monto al final", Toast.LENGTH_SHORT).show()
        }
    }
}