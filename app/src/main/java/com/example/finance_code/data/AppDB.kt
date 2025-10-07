package com.example.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Aquí va la base de datos
@Database(entities = [Movimiento::class], version = 1, exportSchema = false)
abstract class AppDB : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        fun getDatabase(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDB::class.java,
                    "finance_db"   // Nombre de la BD
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}