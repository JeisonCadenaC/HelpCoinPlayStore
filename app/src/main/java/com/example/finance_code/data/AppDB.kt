package com.example.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Movimiento::class, Recordatorio::class, MetaDB::class, Categoria::class],
    version = 11, // Subimos a versión 11
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDB : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun recordatorioDao(): RecordatorioDao
    abstract fun metaDao(): MetaDao
    abstract fun categoriaDao(): CategoriaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null
        private var CURRENT_DB_NAME: String? = null

        val MIGRATION_8_10 = object : Migration(8, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `categorias` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `esPersonalizada` INTEGER NOT NULL DEFAULT 0)")
                try { database.execSQL("ALTER TABLE `movimientos` ADD COLUMN `categoriaId` INTEGER NOT NULL DEFAULT 16") } catch (e: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `categorias` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `esPersonalizada` INTEGER NOT NULL DEFAULT 0)")
                try { database.execSQL("ALTER TABLE `movimientos` ADD COLUMN `categoriaId` INTEGER NOT NULL DEFAULT 16") } catch (e: Exception) {}
            }
        }

        // NUEVA MIGRACIÓN: Añadir la columna HORA
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try { database.execSQL("ALTER TABLE `movimientos` ADD COLUMN `hora` TEXT NOT NULL DEFAULT '00:00:00'") } catch (e: Exception) {}
            }
        }

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
                    .addMigrations(MIGRATION_8_10, MIGRATION_9_10, MIGRATION_10_11)
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