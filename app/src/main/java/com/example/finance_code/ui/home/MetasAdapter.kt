package com.example.finance_code.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.MetaDB

class MetasAdapter(
    private var metaDBS: List<MetaDB> = emptyList(),
    private val onActualizarClick: (MetaDB) -> Unit
) : RecyclerView.Adapter<MetasAdapter.MetaViewHolder>() {

    inner class MetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMeta)
        val tvMontos: TextView = itemView.findViewById(R.id.tvMontos)
        val tvPorcentaje: TextView = itemView.findViewById(R.id.tvPorcentaje)
        val progress: ProgressBar = itemView.findViewById(R.id.progressMeta)
        val btnActualizar: Button = itemView.findViewById(R.id.btnActualizar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meta, parent, false)
        return MetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        val meta = metaDBS[position]

        holder.tvNombre.text = meta.nombre
        holder.tvMontos.text = "$${meta.montoActual} / $${meta.montoObjetivo}"

        val porcentaje =
            if (meta.montoObjetivo > 0) ((meta.montoActual / meta.montoObjetivo) * 100).coerceIn(0.0, 100.0)
            else 0.0

        holder.tvPorcentaje.text = "${porcentaje.toInt()}%"
        holder.progress.progress = porcentaje.toInt()

        holder.btnActualizar.setOnClickListener {
            onActualizarClick(meta)
        }
    }

    override fun getItemCount(): Int = metaDBS.size

    fun setData(newMetaDBS: List<MetaDB>) {
        metaDBS = newMetaDBS
        notifyDataSetChanged()
    }
}