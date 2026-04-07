package com.help.finance_code.ui.home

import android.content.Context
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.help.finance_code.R
import com.help.finance_code.data.MetaDB
import com.help.finance_code.DiscreetModeManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.NumberFormat
import java.util.Locale

class MetasAdapter(
    private val currentUserEmail: String,
    private val onMetaClick: (MetaDB) -> Unit,
    private val onMetaLongClick: (MetaDB) -> Unit,
    private val onDragStart: (RecyclerView.ViewHolder) -> Unit,
    private val onAceptarClick: (MetaDB) -> Unit,
    private val onRechazarClick: (MetaDB) -> Unit
) : RecyclerView.Adapter<MetasAdapter.MetaViewHolder>() {

    private var metas = mutableListOf<MetaDB>()

    fun setData(newMetas: List<MetaDB>) {
        metas.clear()
        metas.addAll(newMetas)
        notifyDataSetChanged()
    }

    fun getActualList(): List<MetaDB> = metas.toList()

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val meta = metas.removeAt(fromPosition)
        metas.add(toPosition, meta)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun updateDiscreetMode() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meta, parent, false)
        return MetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        val meta = metas[position]
        holder.bind(meta, currentUserEmail, onMetaClick, onMetaLongClick, onDragStart, onAceptarClick, onRechazarClick)
    }

    override fun getItemCount() = metas.size

    class MetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutNormal: View = itemView.findViewById(R.id.layoutNormal)
        private val layoutExpanded: LinearLayout = itemView.findViewById(R.id.layoutExpanded)
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
        private val ivImagenMeta: ImageView = itemView.findViewById(R.id.ivImagenMeta)
        private val cardImageMeta: View = itemView.findViewById(R.id.cardImageMeta)
        private val ivExpandIcon: ImageView = itemView.findViewById(R.id.ivExpandIcon)
        private val ivDragHandle: View = itemView.findViewById(R.id.ivDragHandle)
        private val hitboxDrag: View? = itemView.findViewById(R.id.hitboxDrag) // Capa opcional si existe en el XML

        private var isExpanded = false

        private fun resolveThemeColor(context: Context, attrId: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attrId, typedValue, true)
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(context, typedValue.resourceId)
            }
            return typedValue.data
        }

        fun showDragIndicator() {
            ivDragHandle.visibility = View.VISIBLE
        }

        fun hideDragIndicator() {
            ivDragHandle.visibility = View.GONE
        }

        fun bind(
            meta: MetaDB,
            myEmail: String,
            onClick: (MetaDB) -> Unit,
            onLongClick: (MetaDB) -> Unit,
            onDragStart: (RecyclerView.ViewHolder) -> Unit,
            onAceptar: (MetaDB) -> Unit,
            onRechazar: (MetaDB) -> Unit
        ) {
            ivDragHandle.visibility = View.GONE

            val esInvitacion = meta.invitaciones.contains(myEmail)
            val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            formatoMoneda.maximumFractionDigits = 0

            val colorSurfaceAttrId = com.google.android.material.R.attr.colorSurface
            val colorSurface = resolveThemeColor(itemView.context, colorSurfaceAttrId)

            val colorPrimary = resolveThemeColor(itemView.context, com.google.android.material.R.attr.colorPrimary)
            val colorGray = ContextCompat.getColor(itemView.context, com.help.finance_code.R.color.gray)

            if (esInvitacion) {
                layoutNormal.visibility = View.GONE
                layoutExpanded.visibility = View.GONE
                layoutInvitacion.visibility = View.VISIBLE

                cardView.strokeColor = colorPrimary
                cardView.strokeWidth = 4
                cardView.setCardBackgroundColor(colorSurface)

                tvInviteTitulo.text = meta.nombre
                val remitente = meta.usuarios.firstOrNull() ?: "Alguien"
                tvInviteRemitente.text = "Invitado por: $remitente"

                if (DiscreetModeManager.isDiscreetModeActive) {
                    tvInviteMonto.text = "Objetivo: •••••••••••"
                } else {
                    tvInviteMonto.text = "Objetivo: ${formatoMoneda.format(meta.montoObjetivo)}"
                }

                btnAceptar.setOnClickListener { onAceptar(meta) }
                btnRechazar.setOnClickListener { onRechazar(meta) }

                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener(null)
                hitboxDrag?.setOnClickListener(null)
                hitboxDrag?.setOnLongClickListener(null)
            } else {
                layoutInvitacion.visibility = View.GONE
                layoutNormal.visibility = View.VISIBLE

                if (DiscreetModeManager.isDiscreetModeActive) {
                    tvNombre.text = "•••••••••••"
                    val censuraMontos = "••••••••••• / •••••••••••"
                    tvMonto.text = censuraMontos
                    tvPorcentaje.text = "••%"

                    progressBar.setIndicatorColor(colorGray)
                    progressBar.progress = 0
                    layoutExpanded.visibility = View.GONE
                    isExpanded = false
                    ivExpandIcon.rotation = 0f
                } else {
                    tvNombre.text = meta.nombre
                    val actualStr = formatoMoneda.format(meta.montoActual)
                    val objetivoStr = formatoMoneda.format(meta.montoObjetivo)
                    tvMonto.text = "$actualStr / $objetivoStr"

                    val progreso = if (meta.montoObjetivo > 0) (meta.montoActual / meta.montoObjetivo * 100).toInt() else 0
                    tvPorcentaje.text = "$progreso%"

                    progressBar.setIndicatorColor(colorPrimary)
                    progressBar.progress = progreso

                    if (meta.imagenUrl != null) {
                        cardImageMeta.visibility = View.VISIBLE
                        Glide.with(itemView.context)
                            .load(meta.imagenUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(ivImagenMeta)
                    } else {
                        cardImageMeta.visibility = View.GONE
                    }

                    layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    ivExpandIcon.rotation = if (isExpanded) 180f else 0f
                }

                cardView.strokeWidth = 0
                if (meta.completada) {
                    cardView.setCardBackgroundColor(colorGray)
                } else {
                    cardView.setCardBackgroundColor(colorSurface)
                }

                // LOGICA DE EVENTOS (Expansión)
                val clickAction = View.OnClickListener {
                    if (!DiscreetModeManager.isDiscreetModeActive) {
                        isExpanded = !isExpanded
                        layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        ivExpandIcon.animate().rotation(if (isExpanded) 180f else 0f).setDuration(200).start()
                    }
                }

                itemView.setOnClickListener(clickAction)
                hitboxDrag?.setOnClickListener(clickAction) // Por si el hitbox atrapa el clic normal

                // LÓGICA DE ARRASTRE AL MANTENER PRESIONADO
                val longClickAction = View.OnLongClickListener {
                    if (!DiscreetModeManager.isDiscreetModeActive) {
                        itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onDragStart(this@MetaViewHolder)
                        // onLongClick(meta) <-- Lo comento para que arrastrar no active acciones secundarias a la vez
                    }
                    true
                }

                itemView.setOnLongClickListener(longClickAction)
                hitboxDrag?.setOnLongClickListener(longClickAction)

                btnActualizar.setOnClickListener { onClick(meta) }
            }
        }
    }
}