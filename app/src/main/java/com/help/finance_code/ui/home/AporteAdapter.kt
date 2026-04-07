package com.help.finance_code.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.help.finance_code.R
import com.help.finance_code.data.AporteDB
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AporteAdapter : RecyclerView.Adapter<AporteAdapter.AporteViewHolder>() {

    private var aportes = listOf<AporteDB>()
    private val formatoFecha = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    fun setData(newAportes: List<AporteDB>) {
        aportes = newAportes.sortedByDescending { it.timestamp }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AporteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aporte, parent, false)
        return AporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: AporteViewHolder, position: Int) {
        holder.bind(aportes[position], formatoFecha, formatoMoneda)
    }

    override fun getItemCount() = aportes.size

    class AporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha: TextView = itemView.findViewById(R.id.tvAporteFecha)
        private val tvUsuario: TextView = itemView.findViewById(R.id.tvAporteUsuario)
        private val tvMonto: TextView = itemView.findViewById(R.id.tvAporteMonto)

        fun bind(aporte: AporteDB, formatoFecha: SimpleDateFormat, formatoMoneda: NumberFormat) {
            tvFecha.text = formatoFecha.format(Date(aporte.timestamp))
            tvUsuario.text = "Usuario: ${aporte.userId.substringBefore('@')}"

            val montoStr = formatoMoneda.format(Math.abs(aporte.monto))
            tvMonto.text = if (aporte.tipo == "APORTE") "+ $montoStr" else "- $montoStr"

            val color = if (aporte.tipo == "APORTE") R.color.income_green else R.color.expense_red
            tvMonto.setTextColor(ContextCompat.getColor(itemView.context, color))
        }
    }
}