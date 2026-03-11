package com.example.finance_code.ui.transaction

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiAdapter(
    private val emojis: List<String>,
    private val onEmojiSelected: (String) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val tv = TextView(parent.context).apply {
            textSize = 32f
            gravity = android.view.Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                140 // Altura de cada celda de emoji
            )
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            parent.context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }
        return EmojiViewHolder(tv)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.tvEmoji.text = emoji

        holder.itemView.setOnClickListener {
            onEmojiSelected(emoji)
        }
    }

    override fun getItemCount() = emojis.size
}