package com.example.finance_code.data

import android.app.Application

//Inicializar tu base de datos Room al arrancar la app.
//Guardar la instancia en un lugar global (Application) para que toda tu app la comparta.
class MovimientoAPP : Application() {
    lateinit var room: AppDB

    override fun onCreate() {
        super.onCreate()
        // Inicializamos la base de datos con la función centralizada
        room = AppDB.getDatabase(applicationContext)
    }
}
