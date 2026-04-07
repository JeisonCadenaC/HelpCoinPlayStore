package com.help.finance_code.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.help.finance_code.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class CalculadoraImpuestosFragment : Fragment() {

    private lateinit var etMontoBase: TextInputEditText
    private lateinit var etTasaImpuesto: TextInputEditText
    private lateinit var btnCalcular: MaterialButton
    private lateinit var btnDesglosar: MaterialButton
    private lateinit var btnVolver: ImageView
    private lateinit var cardResultado: MaterialCardView
    private lateinit var tvTituloResultado: TextView
    private lateinit var tvResultadoNeto: TextView
    private lateinit var tvResultadoImpuesto: TextView
    private lateinit var tvResultadoTotal: TextView

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calculadora_impuestos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnVolver = view.findViewById(R.id.btn_volver_impuestos)
        etMontoBase = view.findViewById(R.id.etMontoBase)
        etTasaImpuesto = view.findViewById(R.id.etTasaImpuesto)
        btnCalcular = view.findViewById(R.id.btnCalcular)
        btnDesglosar = view.findViewById(R.id.btnDesglosar)

        cardResultado = view.findViewById(R.id.cardResultado)
        tvTituloResultado = view.findViewById(R.id.tvTituloResultado)
        tvResultadoNeto = view.findViewById(R.id.tvResultadoNeto)
        tvResultadoImpuesto = view.findViewById(R.id.tvResultadoImpuesto)
        tvResultadoTotal = view.findViewById(R.id.tvResultadoTotal)

        cardResultado.visibility = View.INVISIBLE

        btnVolver.setOnClickListener {
            findNavController().popBackStack()
        }

        btnCalcular.setOnClickListener {
            calcularTotal()
        }

        btnDesglosar.setOnClickListener {
            desglosarImpuesto()
        }
    }

    private fun validarCampos(): Pair<Double, Double>? {
        val montoTexto = etMontoBase.text.toString().trim().replace(",", ".").toDoubleOrNull()
        val tasaTexto = etTasaImpuesto.text.toString().trim().replace(",", ".").toDoubleOrNull()

        if (montoTexto == null || tasaTexto == null) {
            Toast.makeText(context, "⚠️ Ingresa el monto y la tasa de impuesto", Toast.LENGTH_SHORT).show()
            return null
        }

        if (montoTexto <= 0) {
            Toast.makeText(context, "⚠️ El monto debe ser positivo", Toast.LENGTH_SHORT).show()
            return null
        }

        if (tasaTexto < 0) {
            Toast.makeText(context, "⚠️ La tasa de impuesto no puede ser negativa", Toast.LENGTH_SHORT).show()
            return null
        }

        return Pair(montoTexto, tasaTexto)
    }

    private fun calcularTotal() {
        val (montoNeto, tasa) = validarCampos() ?: return

        val factor = 1 + (tasa / 100.0)
        val total = montoNeto * factor
        val impuesto = total - montoNeto

        mostrarResultado(montoNeto, impuesto, total, tasa, esDesglose = false)
    }

    private fun desglosarImpuesto() {
        val (montoTotal, tasa) = validarCampos() ?: return

        val divisor = 1 + (tasa / 100.0)
        val montoNeto = montoTotal / divisor
        val impuesto = montoTotal - montoNeto

        mostrarResultado(montoNeto, impuesto, montoTotal, tasa, esDesglose = true)
    }

    private fun mostrarResultado(neto: Double, impuesto: Double, total: Double, tasa: Double, esDesglose: Boolean) {
        val operacion = if (esDesglose) "Desglose" else "Cálculo"
        tvTituloResultado.text = "$operacion aplicado del ${tasa}%"

        tvResultadoNeto.text = currencyFormat.format(neto)
        tvResultadoImpuesto.text = currencyFormat.format(impuesto)
        tvResultadoTotal.text = currencyFormat.format(total)

        cardResultado.visibility = View.VISIBLE
    }
}