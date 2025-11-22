package com.example.finance_code.ui.transaction

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import android.text.Editable
import android.text.TextWatcher
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class addTransaction : AppCompatActivity() {

    private lateinit var cdodescripcionT: TextInputEditText
    private lateinit var cdoValorT: TextInputEditText
    private lateinit var btnregistrarOpcion: MaterialButtonToggleGroup

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
        btnregistrarOpcion = findViewById(R.id.registrarOpcion)
        cdodescripcionT = findViewById(R.id.descripcionT)
        cdoValorT = findViewById(R.id.valorT)
        val btnGuardar = findViewById<MaterialButton>(R.id.guardarT)

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

        val transactionTypeFromWidget = intent.getIntExtra(EXTRA_WIDGET_TRANSACTION_TYPE, -1)
        if (transactionTypeFromWidget != -1) {
            val buttonId = if (transactionTypeFromWidget == TYPE_INGRESO) {
                R.id.ingreso
            } else {
                R.id.egreso
            }
            btnregistrarOpcion.check(buttonId)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnVoiceInput.setOnClickListener {
            startVoiceInput()
        }

        cdoValorT.addTextChangedListener(object : TextWatcher {
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

                        cdoValorT.setText(formatted)
                        cdoValorT.setSelection(formatted.length)

                    } catch (e: NumberFormatException) {
                    }
                }

                isUpdating = false
            }
        })

        btnGuardar.setOnClickListener {
            val descripcion = cdodescripcionT.text.toString()
            val cantidadTexto = cdoValorT.text.toString()
            val tipoSeleccionado = btnregistrarOpcion.checkedButtonId

            if (descripcion.isEmpty()) {
                Toast.makeText(this , "⚠️ Ingresa una descripción", Toast.LENGTH_SHORT).show()
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

            val cleanCantidadTexto = cantidadTexto
                .replace(".", "")
                .replace(",", ".")

            val cantidad = cleanCantidadTexto.toDoubleOrNull()

            if (cantidad == null || cantidad <= 0) {
                Toast.makeText(this, "⚠️ Cantidad inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipo = if (tipoSeleccionado == R.id.ingreso) 1 else 0

            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val nuevoMovimiento = Movimiento(
                descripcion = descripcion,
                cantidad = cantidad,
                tipo = tipo,
                fecha = fechaActual,
                categoria = ""
            )

            val job = viewModel.insertar(nuevoMovimiento)

            job.invokeOnCompletion {
                val intent = Intent(applicationContext, FinanceWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                    ComponentName(applicationContext, FinanceWidgetProvider::class.java)
                )
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                sendBroadcast(intent)

                runOnUiThread {
                    Toast.makeText(
                        this@addTransaction,
                        "✅ Transacción guardada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
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

            cdoValorT.setText(cleanNumber)

            if (descripcionStr.isNotEmpty()) {
                cdodescripcionT.setText(descripcionStr.replaceFirstChar { it.uppercase() })
            } else {
                cdodescripcionT.setText("Gasto sin descripción")
            }
        } else {
            cdodescripcionT.setText(texto.replaceFirstChar { it.uppercase() })
            Toast.makeText(this, "No detecté un monto al final", Toast.LENGTH_SHORT).show()
        }
    }
}