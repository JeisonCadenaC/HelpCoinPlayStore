package com.example.finance_code.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MetaDao {

    // Obtener todas las metas (ordenadas: primero las no completadas)
    @Query("SELECT * FROM metas_table ORDER BY completada ASC, fechaLimite IS NULL, fechaLimite")
    fun getAllMetas(): LiveData<List<MetaDB>>

    // Insertar una nueva meta
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(metaDB: MetaDB)

    // Actualizar una meta existente
    @Update
    suspend fun update(metaDB: MetaDB)

    // Eliminar una meta
    @Delete
    suspend fun delete(metaDB: MetaDB)
}