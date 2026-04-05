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
import com.example.finance_code.DiscreetModeManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import java.text.NumberFormat
import java.util.Locale

sealed class MovimientoListItem {
    data class Header(val title: String) : MovimientoListItem()
    data class Item(val movimiento: Movimiento) : MovimientoListItem()
}

class MovimientosAdapter(
    private var items: List<MovimientoListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // --- NUEVAS VARIABLES DE SELECCIÓN MÚLTIPLE ---
    var isSelectionMode = false
    val selectedItems = mutableSetOf<Movimiento>()

    var onItemClickListener: ((Movimiento) -> Unit)? = null
    var onSelectionModeChangeListener: ((Boolean, Int) -> Unit)? = null

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

            // --- TU LÓGICA VISUAL ORIGINAL (INTACTA) ---
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

            // --- NUEVA LÓGICA DE SELECCIÓN MÚLTIPLE (Adaptada a tus bordes dinámicos) ---
            val isSelected = selectedItems.contains(movimiento)
            val materialCardView = holder.itemView as MaterialCardView

            // Mostramos u ocultamos el checkbox dependiendo si estamos en modo selección masiva
            holder.cbSeleccion.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            holder.cbSeleccion.isChecked = isSelected

            // Tu efecto de borde original, pero ahora se activa con la selección múltiple
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

            // --- CONTROL DE CLICS (Integrando lo tuyo con la nueva función) ---
            holder.itemView.setOnClickListener {
                if (isSelectionMode) {
                    if (selectedItems.contains(movimiento)) {
                        selectedItems.remove(movimiento)
                    } else {
                        selectedItems.add(movimiento)
                    }
                    notifyItemChanged(position)
                    onSelectionModeChangeListener?.invoke(isSelectionMode, selectedItems.size)
                } else {
                    // Si no estamos seleccionando, abre la pantalla de edición como querías
                    onItemClickListener?.invoke(movimiento)
                }
            }

            // Reemplazo del Long Click original para activar el modo selección múltiple
            holder.itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    setSelectionModeActive(true)
                    selectedItems.add(movimiento)
                    notifyDataSetChanged() // Refresca todo para que aparezcan los checkboxes
                    onSelectionModeChangeListener?.invoke(isSelectionMode, selectedItems.size)
                }
                true
            }

            holder.cbSeleccion.setOnClickListener {
                if (holder.cbSeleccion.isChecked) {
                    selectedItems.add(movimiento)
                } else {
                    selectedItems.remove(movimiento)
                }
                notifyItemChanged(position)
                onSelectionModeChangeListener?.invoke(isSelectionMode, selectedItems.size)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setData(nuevosItems: List<MovimientoListItem>) {
        this.items = nuevosItems
        // Solo limpiamos la selección si no estamos en modo selección activa
        if (!isSelectionMode) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    fun updateDiscreetMode() {
        notifyDataSetChanged()
    }

    // Funciones para manejar el modo selección múltiple
    fun setSelectionModeActive(active: Boolean) {
        isSelectionMode = active
        if (!active) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
        onSelectionModeChangeListener?.invoke(isSelectionMode, selectedItems.size)
    }

    fun selectAll() {
        selectedItems.clear()
        items.forEach {
            if (it is MovimientoListItem.Item) selectedItems.add(it.movimiento)
        }
        notifyDataSetChanged()
        onSelectionModeChangeListener?.invoke(isSelectionMode, selectedItems.size)
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

        // El nuevo checkbox
        val cbSeleccion: MaterialCheckBox = itemView.findViewById(R.id.cbSeleccion)
    }
}