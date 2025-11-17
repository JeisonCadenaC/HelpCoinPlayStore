package com.example.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.Objects

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

        private var CURRENT_DB_NAME: String? = null

        private fun getDbNameFromEmail(email: String): String {
            return "finance_db_" + email.replace(Regex("[^a-zA-Z0-9]"), "_")
        }

        fun getDatabase(context: Context, email: String): AppDB {
            val dbName = getDbNameFromEmail(email)

            return INSTANCE?.let {
                if (CURRENT_DB_NAME != dbName) {
                    it.close()
                    INSTANCE = null
                    CURRENT_DB_NAME = null
                    getDatabase(context, email)
                } else {
                    it
                }
            } ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDB::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                CURRENT_DB_NAME = dbName
                instance
            }
        }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
            CURRENT_DB_NAME = null
        }
    }
}