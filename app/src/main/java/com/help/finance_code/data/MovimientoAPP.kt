package com.help.finance_code.data

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MovimientoAPP : Application() {

    override fun onCreate() {
        super.onCreate()
        aplicarTemaGuardado()
    }

    private fun aplicarTemaGuardado() {
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("is_dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}