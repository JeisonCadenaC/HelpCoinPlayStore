package com.example.finance_code.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insertar(movimiento: Movimiento)

    @Update
    suspend fun actualizar(movimiento: Movimiento)

    @Delete
    suspend fun eliminar(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<Movimiento>>


    @Query("SELECT SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END) FROM movimientos")
    fun obtenerSaldoActualSincrono(): Double?

    @Query("SELECT SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END) FROM movimientos")
    fun obtenerSaldoTotalFlow(): Flow<Double?>
}