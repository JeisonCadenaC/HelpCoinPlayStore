package com.example.finance_code.ui.home

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.MetaDB
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.NumberFormat
import java.util.Locale

class MetasAdapter(
    private val currentUserEmail: String,
    private val onMetaClick: (MetaDB) -> Unit,
    private val onAceptarClick: (MetaDB) -> Unit,
    private val onRechazarClick: (MetaDB) -> Unit
) : RecyclerView.Adapter<MetasAdapter.MetaViewHolder>() {

    private var metas = listOf<MetaDB>()

    fun setData(newMetas: List<MetaDB>) {
        metas = newMetas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meta, parent, false)
        return MetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        val meta = metas[position]
        holder.bind(meta, currentUserEmail, onMetaClick, onAceptarClick, onRechazarClick)
    }

    override fun getItemCount() = metas.size

    class MetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutNormal: View = itemView.findViewById(R.id.layoutNormal)
        private val layoutInvitacion: View = itemView.findViewById(R.id.layoutInvitacion)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardMeta)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMeta)
        private val tvMonto: TextView = itemView.findViewById(R.id.tvMontos)
        private val progressBar: CircularProgressIndicator = itemView.findViewById(R.id.progressMeta)
        private val tvPorcentaje: TextView = itemView.findViewById(R.id.tvPorcentaje)
        private val btnActualizar: View = itemView.findViewById(R.id.btnActualizar)
        private val tvInviteTitulo: TextView = itemView.findViewById(R.id.tvInviteTitulo)
        private val tvInviteRemitente: TextView = itemView.findViewById(R.id.tvInviteRemitente)
        private val tvInviteMonto: TextView = itemView.findViewById(R.id.tvInviteMonto)
        private val btnAceptar: MaterialButton = itemView.findViewById(R.id.btnAceptar)
        private val btnRechazar: MaterialButton = itemView.findViewById(R.id.btnRechazar)

        private fun resolveThemeColor(context: Context, attrId: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attrId, typedValue, true)
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(context, typedValue.resourceId)
            }
            return typedValue.data
        }

        fun bind(
            meta: MetaDB,
            myEmail: String,
            onClick: (MetaDB) -> Unit,
            onAceptar: (MetaDB) -> Unit,
            onRechazar: (MetaDB) -> Unit
        ) {
            val esInvitacion = meta.invitaciones.contains(myEmail)
            val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            formatoMoneda.maximumFractionDigits = 0

            val colorSurfaceAttrId = com.google.android.material.R.attr.colorSurface
            val colorSurface = resolveThemeColor(itemView.context, colorSurfaceAttrId)

            if (esInvitacion) {
                layoutNormal.visibility = View.GONE
                layoutInvitacion.visibility = View.VISIBLE

                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                cardView.strokeWidth = 4
                cardView.setCardBackgroundColor(colorSurface)

                tvInviteTitulo.text = meta.nombre
                val remitente = meta.usuarios.firstOrNull() ?: "Alguien"
                tvInviteRemitente.text = "Invitado por: $remitente"
                tvInviteMonto.text = "Objetivo: ${formatoMoneda.format(meta.montoObjetivo)}"

                btnAceptar.setOnClickListener { onAceptar(meta) }
                btnRechazar.setOnClickListener { onRechazar(meta) }
                itemView.setOnClickListener(null)
            } else {
                layoutInvitacion.visibility = View.GONE
                layoutNormal.visibility = View.VISIBLE

                tvNombre.text = meta.nombre

                val actualStr = formatoMoneda.format(meta.montoActual)
                val objetivoStr = formatoMoneda.format(meta.montoObjetivo)
                tvMonto.text = "$actualStr / $objetivoStr"

                val progreso = if (meta.montoObjetivo > 0) (meta.montoActual / meta.montoObjetivo * 100).toInt() else 0
                progressBar.progress = progreso
                tvPorcentaje.text = "$progreso%"

                cardView.strokeWidth = 0

                if (meta.completada) {
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.gray))
                } else {
                    cardView.setCardBackgroundColor(colorSurface)
                }

                itemView.setOnClickListener { onClick(meta) }
                btnActualizar.setOnClickListener { onClick(meta) }
            }
        }
    }
}