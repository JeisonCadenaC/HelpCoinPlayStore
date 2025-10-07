package com.example.finance_code.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Aquí van las funciones (insertar, leer, etc.)
@Dao
interface MovimientoDao {

    @Insert
    suspend fun insertar(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos")
    fun obtenerTodos(): Flow<List<Movimiento>>
}