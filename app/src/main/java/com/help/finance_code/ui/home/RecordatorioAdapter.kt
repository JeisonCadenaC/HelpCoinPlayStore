package com.help.finance_code.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.help.finance_code.data.Recordatorio
import com.help.finance_code.databinding.ItemRecordatorioBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordatorioAdapter(
    private val onEliminarClicked: (Recordatorio) -> Unit
) : ListAdapter<Recordatorio, RecordatorioAdapter.RecordatorioViewHolder>(RecordatorioDiffCallback()) {

    private var idItemSeleccionado: Long? = null

    inner class RecordatorioViewHolder(private val binding: ItemRecordatorioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recordatorio: Recordatorio) {
            binding.txtNombreRecordatorio.text = recordatorio.nombre

            val sdf = SimpleDateFormat("EEEE d, MMM yyyy", Locale("es", "ES"))
            binding.txtFechaRecordatorio.text = sdf.format(Date(recordatorio.fechaMillis)).replaceFirstChar { it.uppercase() }

            if (recordatorio.id.toLong() == idItemSeleccionado) {
                binding.btnEliminar.visibility = View.VISIBLE
            } else {
                binding.btnEliminar.visibility = View.GONE
            }

            binding.root.setOnLongClickListener {
                val recId = recordatorio.id.toLong()
                if (idItemSeleccionado == recId) {
                    idItemSeleccionado = null
                } else {
                    idItemSeleccionado = recId
                }
                notifyDataSetChanged()
                true
            }

            binding.root.setOnClickListener {
                if (idItemSeleccionado != null) {
                    idItemSeleccionado = null
                    notifyDataSetChanged()
                }
            }

            binding.btnEliminar.setOnClickListener {
                onEliminarClicked(recordatorio)
                idItemSeleccionado = null
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordatorioViewHolder {
        val binding = ItemRecordatorioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordatorioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordatorioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class RecordatorioDiffCallback : DiffUtil.ItemCallback<Recordatorio>() {
    override fun areItemsTheSame(oldItem: Recordatorio, newItem: Recordatorio): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Recordatorio, newItem: Recordatorio): Boolean {
        return oldItem == newItem
    }
}