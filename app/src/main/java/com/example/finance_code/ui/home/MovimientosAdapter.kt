package com.example.finance_code.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
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
        val ivIcono: ImageView = itemView.findViewById(R.id.ivIcono)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val iconContainer: CardView = itemView.findViewById(R.id.iconContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return MovimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        val movimiento = movimientos[position]

        holder.tvNombre.text = movimiento.descripcion
        holder.tvFecha.text = movimiento.fecha

        val locale = Locale.Builder().setLanguage("es").setRegion("CO").build()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.maximumFractionDigits = 0
        val montoFormateado = formatter.format(movimiento.cantidad)

        if (movimiento.tipo == 1) {
            holder.tvMonto.text = "+ $montoFormateado"
            val verde = Color.parseColor("#4CAF50")
            holder.tvMonto.setTextColor(verde)

            holder.ivIcono.setImageResource(R.drawable.ic_arrow_up)
            holder.ivIcono.imageTintList = ColorStateList.valueOf(verde)
            holder.iconContainer.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        } else {
            holder.tvMonto.text = "- $montoFormateado"
            val rojo = Color.parseColor("#F44336")
            holder.tvMonto.setTextColor(rojo)

            holder.ivIcono.setImageResource(R.drawable.ic_arrow_down)
            holder.ivIcono.imageTintList = ColorStateList.valueOf(rojo)
            holder.iconContainer.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
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