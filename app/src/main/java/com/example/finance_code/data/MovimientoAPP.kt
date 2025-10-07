package com.example.finance_code.data

import android.app.Application
import androidx.room.Room
//Inicializar tu base de datos Room al arrancar la app.
//Guardar la instancia en un lugar global (Application) para que toda tu app la comparta.
class MovimientoAPP : Application() {
    val room= Room.databaseBuilder(applicationContext,AppDB::class.java,"Movimiento").build()

}
