package com.example.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [Movimiento::class, MetaDB::class, Recordatorio::class],
    version = 3,
    exportSchema = false
)
abstract class AppDB : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun metaDao(): MetaDao
    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        fun getDatabase(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDB::class.java,
                    "finance_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}