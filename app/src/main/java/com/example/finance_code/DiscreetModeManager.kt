package com.example.finance_code

import android.content.Context

object DiscreetModeManager {

    private const val PREFS_NAME = "AppPrefs"
    private const val MODE_KEY = "IsDiscreetModeActive"

    private lateinit var appContext: Context

    var isDiscreetModeActive: Boolean = false
        private set

    var modeChangeListener: (() -> Unit)? = null

    fun initialize(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            loadState()
        }
    }

    private fun loadState() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDiscreetModeActive = prefs.getBoolean(MODE_KEY, false)
    }

    private fun saveState() {
        if (::appContext.isInitialized) {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(MODE_KEY, isDiscreetModeActive)
                .apply()
        }
    }

    fun toggleMode() {
        isDiscreetModeActive = !isDiscreetModeActive
        saveState()
        modeChangeListener?.invoke()
    }
}