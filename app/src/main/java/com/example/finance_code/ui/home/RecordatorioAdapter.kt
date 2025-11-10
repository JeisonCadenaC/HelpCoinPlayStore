package com.example.finance_code.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.data.Recordatorio
import com.example.finance_code.databinding.ItemRecordatorioBinding
import java.text.SimpleDateFormat
import java.util.Locale


class RecordatorioAdapter(
    private val onEliminarClicked: (Recordatorio) -> Unit
) : ListAdapter<Recordatorio, RecordatorioAdapter.RecordatorioViewHolder>(RecordatorioDiffCallback()) {

    inner class RecordatorioViewHolder(private val binding: ItemRecordatorioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recordatorio: Recordatorio) {
            binding.txtNombreRecordatorio.text = recordatorio.nombre


            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.txtFechaRecordatorio.text = sdf.format(recordatorio.fechaMillis)


            binding.btnEliminar.setOnClickListener {
                onEliminarClicked(recordatorio)
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