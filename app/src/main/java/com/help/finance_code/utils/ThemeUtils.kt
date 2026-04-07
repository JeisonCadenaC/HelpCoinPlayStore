package com.help.finance_code.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import com.help.finance_code.R

object ThemeUtils {
    private const val PREFS_NAME = "HelpCoinAuraPrefs"
    private const val AURA_COLOR_KEY = "aura_color_hex"
    private const val DEFAULT_AURA_COLOR = "#8000FF"

    fun saveAuraColor(context: Context, hexColor: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(AURA_COLOR_KEY, hexColor).apply()
    }

    fun getAuraColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var hex = prefs.getString(AURA_COLOR_KEY, DEFAULT_AURA_COLOR) ?: DEFAULT_AURA_COLOR

        // Si el usuario tenía el amarillo brillante viejo guardado, lo pasamos al nuevo
        if (hex.uppercase() == "#FFFF00") {
            hex = "#FBC02D"
        }

        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.parseColor(DEFAULT_AURA_COLOR)
        }
    }

    fun getAuraTheme(context: Context): Int {
        val hex = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(AURA_COLOR_KEY, DEFAULT_AURA_COLOR)?.uppercase()
        return when (hex) {
            "#8000FF" -> R.style.Theme_FinanceCode_Purple
            "#4000FF" -> R.style.Theme_FinanceCode_Indigo
            "#0000FF" -> R.style.Theme_FinanceCode_Blue
            "#00FFFF" -> R.style.Theme_FinanceCode_Cyan
            "#00FF80" -> R.style.Theme_FinanceCode_Teal
            "#00FF00" -> R.style.Theme_FinanceCode_Green
            "#FBC02D", "#FFFF00" -> R.style.Theme_FinanceCode_Amber
            "#FF8000" -> R.style.Theme_FinanceCode_Orange
            "#FF0000" -> R.style.Theme_FinanceCode_Red
            "#FF00FF" -> R.style.Theme_FinanceCode_Pink
            else -> R.style.Theme_FinanceCode_Purple
        }
    }

    fun getSwitchNeutralColor(): Int {
        return Color.parseColor("#B0BEC5")
    }

    fun getBottomNavColorStateList(auraColor: Int): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(
            auraColor,
            Color.parseColor("#9EA3B0")
        )
        return ColorStateList(states, colors)
    }

    fun getSelectableItemBackground(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }
}