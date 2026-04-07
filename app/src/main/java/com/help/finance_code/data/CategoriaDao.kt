package com.help.finance_code.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(categoria: Categoria): Long

    @Update
    suspend fun actualizar(categoria: Categoria)

    @Delete
    suspend fun eliminar(categoria: Categoria)

    @Query("SELECT * FROM categorias")
    fun obtenerTodas(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias")
    fun obtenerTodasSync(): List<Categoria>

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): Categoria?
}