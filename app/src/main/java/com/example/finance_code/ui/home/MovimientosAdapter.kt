package com.example.finance_code.ui.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.Movimiento
import java.text.NumberFormat
import java.util.Locale

class MovimientosAdapter(
    private var movimientos: List<Movimiento>
) : RecyclerView.Adapter<MovimientosAdapter.MovimientoViewHolder>() {

    private var onItemLongClickListener: ((Movimiento) -> Unit)? = null

    inner class MovimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvMonto: TextView = itemView.findViewById(R.id.tvCantidad)
        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return MovimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        val movimiento = movimientos[position]
        val context = holder.itemView.context

        holder.tvNombre.text = movimiento.descripcion
        holder.tvFecha.text = movimiento.fecha

        val locale = Locale.Builder().setLanguage("es").setRegion("CO").build()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.maximumFractionDigits = 0
        val montoFormateado = formatter.format(movimiento.cantidad)

        holder.tvIcono.text = ""

        if (movimiento.tipo == 1) {
            holder.tvMonto.text = "+${montoFormateado}"
            holder.tvIcono.setBackgroundResource(R.drawable.ic_savings)
            val colorVerde = ContextCompat.getColor(context, R.color.colorIngreso)
            holder.tvIcono.backgroundTintList = ColorStateList.valueOf(colorVerde)
            holder.tvMonto.setTextColor(colorVerde)
        } else {
            holder.tvMonto.text = "-${montoFormateado}"
            holder.tvIcono.setBackgroundResource(R.drawable.ic_credit_card)
            val colorRojo = ContextCompat.getColor(context, R.color.colorEgreso)
            holder.tvIcono.backgroundTintList = ColorStateList.valueOf(colorRojo)
            holder.tvMonto.setTextColor(colorRojo)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClickListener?.invoke(movimiento)
            true
        }
    }

    override fun getItemCount(): Int = movimientos.size

    fun setData(nuevosMovimientos: List<Movimiento>) {
        this.movimientos = nuevosMovimientos
        notifyDataSetChanged()
    }

    fun setOnItemLongClickListener(listener: (Movimiento) -> Unit) {
        onItemLongClickListener = listener
    }
}