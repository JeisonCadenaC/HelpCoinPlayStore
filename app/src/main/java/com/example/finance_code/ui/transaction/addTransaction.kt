package com.example.finance_code.ui.transaction

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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

class addTransaction : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)
        val viewModel: MovimientoViewModel

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val btnregistrarOpcion = findViewById<MaterialButtonToggleGroup>(R.id.registrarOpcion)
        val cdodescripcionT = findViewById<TextInputEditText>(R.id.descripcionT)
        val cdoValorT = findViewById<TextInputEditText>(R.id.valorT)
        val btnGuardar = findViewById<MaterialButton>(R.id.guardarT)

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
            if (buttonId != -1) {
                btnregistrarOpcion.check(buttonId)
            }
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }

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

            val cantidad = cantidadTexto.replace(".", "").replace(",", ".").toDoubleOrNull()

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
}