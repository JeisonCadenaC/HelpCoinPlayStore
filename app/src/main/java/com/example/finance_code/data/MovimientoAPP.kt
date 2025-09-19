package com.example.finance_code.data

import android.app.Application
import androidx.room.Room

class MovimientoAPP : Application() {
    val room= Room.databaseBuilder(applicationContext,AppDB::class.java,"Movimiento").build()

}
