package com.example.finance_code.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.Movimiento
import java.text.NumberFormat
import java.util.Locale

class MovimientosAdapter : RecyclerView.Adapter<MovimientosAdapter.MovimientoViewHolder>() {

    private var movimientos = listOf<Movimiento>()

    // ViewHolder: representa cada item de la lista
    class MovimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoria)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
    }

    // Inflar el layout del item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return MovimientoViewHolder(view)
    }

    // Llenar los datos de cada item
    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        val movimiento = movimientos[position]

        // Configurar icono y color según el tipo
        if (movimiento.tipo == 1) {
            // Ingreso
            holder.tvIcono.text = "⬆️"
            holder.tvCantidad.setTextColor(0xFF4CAF50.toInt()) // Verde
        } else {
            // Gasto
            holder.tvIcono.text = "⬇️"
            holder.tvCantidad.setTextColor(0xFFF44336.toInt()) // Rojo
        }

        // Formatear cantidad con separador de miles
        val formato = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        holder.tvCantidad.text = formato.format(movimiento.cantidad)

        // Datos básicos
        holder.tvDescripcion.text = movimiento.descripcion
        holder.tvFecha.text = movimiento.fecha

        // Categoría (ocultar si está vacía)
        if (movimiento.categoria.isEmpty()) {
            holder.tvCategoria.visibility = View.GONE
        } else {
            holder.tvCategoria.visibility = View.VISIBLE
            holder.tvCategoria.text = movimiento.categoria
        }
    }

    // Cantidad de items
    override fun getItemCount(): Int = movimientos.size

    // Actualizar la lista cuando cambien los datos
    fun actualizarMovimientos(nuevosMovimientos: List<Movimiento>) {
        movimientos = nuevosMovimientos
        notifyDataSetChanged()
    }
}