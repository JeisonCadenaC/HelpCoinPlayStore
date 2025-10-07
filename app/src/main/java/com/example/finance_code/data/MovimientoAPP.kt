package com.example.finance_code.data

import android.app.Application

class MovimientoAPP : Application() {
    lateinit var room: AppDB

    override fun onCreate() {
        super.onCreate()
        // Inicializamos la base de datos con la función centralizada
        room = AppDB.getDatabase(applicationContext)
    }
}
