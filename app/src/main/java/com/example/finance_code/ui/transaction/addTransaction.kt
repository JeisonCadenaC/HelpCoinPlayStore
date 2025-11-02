package com.example.finance_code.ui.transaction
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class addTransaction : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)
        val viewModel: MovimientoViewModel

        val btnregistrarOpcion = findViewById<RadioGroup>(R.id.registrarOpcion)
        val cdodescripcionT = findViewById<EditText>(R.id.descripcionT)
        val cdoValorT = findViewById<AppCompatEditText>(R.id.valorT)

       // Inicializar ViewModel
        val btnGuardar = findViewById<AppCompatButton>(R.id.guardarT)
        val database = AppDB.getDatabase(this)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]


        // Acción al presionar el botón
        btnGuardar.setOnClickListener {
            val descripcion = cdodescripcionT.text.toString()
            val cantidadTexto = cdoValorT.text.toString()
            val tipoSeleccionado = btnregistrarOpcion.checkedRadioButtonId

            // Validaciones
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

            // Convertir cantidad (eliminar puntos de miles si existen)
            val cantidad = cantidadTexto.replace(".", "").replace(",", ".").toDoubleOrNull()

            if (cantidad == null || cantidad <= 0) {
                Toast.makeText(this, "⚠️ Cantidad inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Determinar tipo (0 = Gasto, 1 = Ingreso)
            val tipo = if (tipoSeleccionado == R.id.ingreso) 1 else 0

            // Obtener fecha actual
            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Crear el movimiento
            val nuevoMovimiento = Movimiento(
                descripcion = descripcion,
                cantidad = cantidad,
                tipo = tipo,
                fecha = fechaActual,
                categoria = "" // Puedes agregar categoría después
            )

            // Guardar en la BD
            viewModel.insertar(nuevoMovimiento)

            // Mensaje de confirmación
            Toast.makeText(
                this,
                "✅ Transacción guardada",
                Toast.LENGTH_SHORT
            ).show()

            // Volver atrás (a MovimientosFragment)
            finish()
        }
    }
}


