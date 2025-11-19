package com.example.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Movimiento::class, Recordatorio::class, MetaDB::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDB : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun recordatorioDao(): RecordatorioDao
    abstract fun metaDao(): MetaDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                CURRENT_DB_NAME = dbName
                instance
            }
        }

        fun checkpointAndClose(context: Context, email: String) {
            val db = getDatabase(context, email)
            if (db.isOpen) {
                try {
                    val checkpointQuery = "PRAGMA wal_checkpoint(FULL)"
                    db.openHelper.writableDatabase.query(checkpointQuery).close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            closeInstance()
        }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
            CURRENT_DB_NAME = null
        }
    }
}