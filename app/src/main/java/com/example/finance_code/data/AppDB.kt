package com.example.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Agregamos la entidad Meta además de Movimiento
@Database(
    entities = [Movimiento::class, MetaDB::class],
    version = 2, // ⚠️ Aumenta la versión al cambiar estructura
    exportSchema = false
)
abstract class AppDB : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun metaDao(): MetaDao   // ✅ Nuevo DAO para las metas

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        fun getDatabase(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDB::class.java,
                    "finance_db"   // Nombre de la BD
                )
                    // Borra la BD si hay cambios de versión (útil durante desarrollo)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
