package com.example.finance_code.ui.transaction

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.finance_code.R
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository
import com.example.finance_code.viewmodel.MovimientoViewModel
import com.example.finance_code.viewmodel.MovimientoViewModelFactory
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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

    private var isUpdating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movimiento = requireArguments().getParcelable("movimiento", Movimiento::class.java)!!

        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        etMonto = view.findViewById(R.id.etMonto)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardar)

        cardEgreso = view.findViewById(R.id.cardEgreso)
        cardIngreso = view.findViewById(R.id.cardIngreso)
        ivEgresoArrow = view.findViewById(R.id.ivEgresoArrow)
        ivIngresoArrow = view.findViewById(R.id.ivIngresoArrow)
        tvEgresoText = view.findViewById(R.id.tvEgresoText)
        tvIngresoText = view.findViewById(R.id.tvIngresoText)

        etDescripcion.setText(movimiento.descripcion)
        etMonto.setText(formatAmount(movimiento.cantidad))

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

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        val database = AppDB.getDatabase(requireContext(), userEmail)
        val repository = MovimientoRepository(database.movimientoDao())
        val factory = MovimientoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MovimientoViewModel::class.java]

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

            val actualizado = movimiento.copy(
                descripcion = descripcionTexto,
                cantidad = nuevaCantidad,
                tipo = tipoMovimientoSeleccionado
            )
            viewModel.actualizar(actualizado)
            Toast.makeText(requireContext(), "Movimiento actualizado", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
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

        val typedValue = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        @ColorInt val colorSuperficie = typedValue.data

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
            cardEgreso.setCardBackgroundColor(colorRojoPastel)
            cardEgreso.strokeColor = colorRojoPuro
            cardEgreso.strokeWidth = 2

        } else {
            cardIngreso.setCardBackgroundColor(colorVerdePastel)
            cardIngreso.strokeColor = colorVerdePuro
            cardIngreso.strokeWidth = 2
        }
    }
}