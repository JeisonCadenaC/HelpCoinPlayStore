package com.example.finance_code.ui.transaction

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finance_code.R
import com.example.finance_code.data.Categoria
import com.google.android.material.card.MaterialCardView

class CategoryAdapter(
    private var categorias: List<Categoria>,
    private val onCategoryClick: (Categoria) -> Unit,
    private val onCategoryLongClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvCategoryEmoji)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val cardBg: MaterialCardView = view.findViewById(R.id.cardEmoji)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val cat = categorias[position]
        holder.tvName.text = cat.nombre
        holder.tvEmoji.text = cat.emoji

        try {
            val colorOriginal = Color.parseColor(cat.colorHex)
            val colorOpaco = Color.argb(40, Color.red(colorOriginal), Color.green(colorOriginal), Color.blue(colorOriginal))
            holder.cardBg.setCardBackgroundColor(colorOpaco)
        } catch (e: Exception) {
            holder.cardBg.setCardBackgroundColor(Color.LTGRAY)
        }

        holder.itemView.setOnClickListener {
            onCategoryClick(cat)
        }

        holder.itemView.setOnLongClickListener {
            onCategoryLongClick(cat)
            true
        }
    }

    override fun getItemCount() = categorias.size

    fun actualizarLista(nuevaLista: List<Categoria>) {
        categorias = nuevaLista
        notifyDataSetChanged()
    }
}