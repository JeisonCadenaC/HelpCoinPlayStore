package com.example.finance_code.ui.home

import android.content.Context
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
import com.example.finance_code.DiscreetModeManager
import com.example.finance_code.utils.ThemeUtils
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

sealed class MovimientoListItem {
    data class Header(val title: String) : MovimientoListItem()
    data class Item(val movimiento: Movimiento) : MovimientoListItem()
}

class MovimientosAdapter(
    private var items: List<MovimientoListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemLongClickListener: ((Movimiento) -> Unit)? = null

    // Variable para rastrear la posición seleccionada actualmente
    private var selectedPosition = RecyclerView.NO_POSITION

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MovimientoListItem.Header -> TYPE_HEADER
            is MovimientoListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento, parent, false)
            MovimientoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        if (holder is HeaderViewHolder && item is MovimientoListItem.Header) {
            holder.tvHeaderTitle.text = item.title
        } else if (holder is MovimientoViewHolder && item is MovimientoListItem.Item) {
            val movimiento = item.movimiento
            val context = holder.itemView.context

            if (DiscreetModeManager.isDiscreetModeActive) {
                holder.tvNombre.text = "***********"
                holder.tvFecha.text = "--/--/----"
                holder.tvMonto.text = "•••••"
                holder.tvMonto.setTextColor(Color.parseColor("#9E9E9E"))
                holder.tvBanco.visibility = View.VISIBLE
                holder.tvBanco.text = "****"

                val grisClaro = Color.parseColor("#9E9E9E")
                val grisFondo = Color.parseColor("#EEEEEE")
                holder.ivIcono.setImageResource(R.drawable.ic_tag)
                holder.ivIcono.imageTintList = ColorStateList.valueOf(grisClaro)
                holder.iconContainer.setCardBackgroundColor(grisFondo)

            } else {
                holder.tvNombre.text = movimiento.descripcion

                if (movimiento.banco == "General") {
                    holder.tvBanco.visibility = View.GONE
                } else {
                    holder.tvBanco.visibility = View.VISIBLE
                    holder.tvBanco.text = movimiento.banco
                }

                val horaCorta = if (movimiento.hora.length >= 5) movimiento.hora.substring(0, 5) else ""
                if (horaCorta.isNotEmpty() && horaCorta != "00:00") {
                    holder.tvFecha.text = "${movimiento.fecha} • $horaCorta"
                } else {
                    holder.tvFecha.text = movimiento.fecha
                }

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
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                } else {
                    holder.tvMonto.text = "- $montoFormateado"
                    val rojo = Color.parseColor("#F44336")
                    holder.tvMonto.setTextColor(rojo)

                    holder.ivIcono.setImageResource(R.drawable.ic_arrow_down)
                    holder.ivIcono.imageTintList = ColorStateList.valueOf(rojo)
                    holder.iconContainer.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            }

            // --- LÓGICA DE SELECCIÓN Y BORDES DINÁMICOS ---
            val isSelected = selectedPosition == holder.adapterPosition
            val materialCardView = holder.itemView as MaterialCardView

            if (isSelected) {
                val strokeWidthPx = (2 * context.resources.displayMetrics.density).toInt()
                materialCardView.strokeWidth = strokeWidthPx

                if (movimiento.tipo == 1) {
                    materialCardView.strokeColor = Color.parseColor("#4CAF50")
                } else {
                    materialCardView.strokeColor = Color.parseColor("#F44336")
                }
            } else {
                materialCardView.strokeWidth = 0
                materialCardView.strokeColor = Color.TRANSPARENT
            }

            holder.itemView.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val previousPosition = selectedPosition

                if (previousPosition == currentPosition) {
                    selectedPosition = RecyclerView.NO_POSITION
                    notifyItemChanged(previousPosition)
                } else {
                    selectedPosition = currentPosition
                    if (previousPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previousPosition)
                    }
                    notifyItemChanged(selectedPosition)
                }
            }

            holder.itemView.setOnLongClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedPosition

                    if (previousPosition != currentPosition) {
                        // Selecciona el nuevo y deselecciona el anterior al mantener presionado
                        selectedPosition = currentPosition
                        if (previousPosition != RecyclerView.NO_POSITION) {
                            notifyItemChanged(previousPosition)
                        }
                        notifyItemChanged(selectedPosition)
                    }
                }
                onItemLongClickListener?.invoke(movimiento)
                true
            }
            // ----------------------------------------------
        }
    }

    override fun getItemCount(): Int = items.size

    fun setData(nuevosItems: List<MovimientoListItem>) {
        this.items = nuevosItems
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun setOnItemLongClickListener(listener: (Movimiento) -> Unit) {
        onItemLongClickListener = listener
    }

    fun updateDiscreetMode() {
        notifyDataSetChanged()
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeaderTitle: TextView = itemView.findViewById(R.id.tvHeaderTitle)
    }

    inner class MovimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvMonto: TextView = itemView.findViewById(R.id.tvCantidad)
        val ivIcono: ImageView = itemView.findViewById(R.id.ivIcono)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val iconContainer: CardView = itemView.findViewById(R.id.iconContainer)
        val tvBanco: TextView = itemView.findViewById(R.id.tvBancoMovimiento)
    }
}