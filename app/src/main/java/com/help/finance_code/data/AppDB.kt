package com.help.finance_code.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Movimiento::class, Recordatorio::class, MetaDB::class, Categoria::class],
    version = 17,
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
                try { database.execSQL("ALTER TABLE `movimientos` ADD COLUMN `categoriaId` INTEGER NOT NULL DEFAULT 21") } catch (e: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `categorias` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `esPersonalizada` INTEGER NOT NULL DEFAULT 0)")
                try { database.execSQL("ALTER TABLE `movimientos` ADD COLUMN `categoriaId` INTEGER NOT NULL DEFAULT 21") } catch (e: Exception) {}
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try { database.execSQL("ALTER TABLE `movimientos` ADD COLUMN `hora` TEXT NOT NULL DEFAULT '00:00:00'") } catch (e: Exception) {}
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val mapIds = mapOf(
                    "Salario e Ingresos" to 1L, "Motos" to 2L, "Carros" to 3L,
                    "Mantenimiento y Reparación" to 4L, "Bancos y Finanzas" to 5L,
                    "Comida y Restaurantes" to 6L, "Supermercado" to 7L,
                    "Transporte Público" to 8L, "Vehículo y Gasolina" to 9L,
                    "Ocio Nocturno" to 10L, "Cine y Entretenimiento" to 11L,
                    "Salud y Farmacia" to 12L, "Hogar y Servicios" to 13L,
                    "Ropa y Cuidado" to 14L, "Educación" to 15L, "Mascotas" to 16L,
                    "Viajes" to 17L, "Gimnasio y Deporte" to 18L, "Regalos" to 19L,
                    "Tecnología" to 20L, "Otros" to 21L
                )

                val cursor = database.query("SELECT id, descripcion FROM movimientos")
                val updates = mutableListOf<Pair<Int, Long>>()

                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val descripcion = cursor.getString(1) ?: ""

                    val nombreCategoria = com.help.finance_code.ui.transaction.CategorySuggester.suggestCategory(descripcion) ?: "Otros"
                    val catId = mapIds[nombreCategoria] ?: 21L

                    updates.add(Pair(id, catId))
                }
                cursor.close()

                updates.forEach { (movId, catId) ->
                    database.execSQL("UPDATE movimientos SET categoriaId = $catId WHERE id = $movId")
                }
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE `categorias` ADD COLUMN `palabrasClave` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE `metas_table` ADD COLUMN `imagenUrl` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
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
                    .addMigrations(MIGRATION_8_10, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
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