package com.example.finance_code.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue

object ThemeUtils {
    private const val PREFS_NAME = "HelpCoinAuraPrefs"
    private const val AURA_COLOR_KEY = "aura_color_hex"
    private const val DEFAULT_AURA_COLOR = "#6200EE" // Morado HelpCoin principal

    fun saveAuraColor(context: Context, hexColor: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(AURA_COLOR_KEY, hexColor).apply()
    }

    fun getAuraColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hex = prefs.getString(AURA_COLOR_KEY, DEFAULT_AURA_COLOR) ?: DEFAULT_AURA_COLOR
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.parseColor(DEFAULT_AURA_COLOR)
        }
    }

    // Color neutral para botones tipo Switch (apagar/encender)
    fun getSwitchNeutralColor(): Int {
        return Color.parseColor("#B0BEC5")
    }

    // Mapeo perfecto para la barra de navegación (compatible modo oscuro y claro)
    fun getBottomNavColorStateList(auraColor: Int): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(
            auraColor,
            Color.parseColor("#9EA3B0") // Gris secundario armónico para íconos inactivos
        )
        return ColorStateList(states, colors)
    }

    // Heredado de Nexu: Resuelve el efecto dominó (ripple) nativo de MD3
    fun getSelectableItemBackground(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }
}