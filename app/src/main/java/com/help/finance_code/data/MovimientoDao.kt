package com.help.finance_code.data

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

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos")
    suspend fun obtenerTodosSync(): List<Movimiento>

    @Query("SELECT SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END) FROM movimientos")
    fun obtenerSaldoTotalFlow(): Flow<Double?>

    @Query("SELECT SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END) FROM movimientos")
    fun obtenerSaldoActualSincrono(): Double?

    @Query("SELECT * FROM movimientos WHERE parentId IS NULL ORDER BY fecha DESC")
    fun obtenerRamas(): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos WHERE parentId = :padreId ORDER BY fecha DESC")
    fun obtenerHijosDeRama(padreId: Int): Flow<List<Movimiento>>

    @Query("SELECT SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END) FROM movimientos WHERE id = :padreId OR parentId = :padreId")
    fun obtenerSaldoDeRamaFlow(padreId: Int): Flow<Double?>

    @Query("SELECT SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END) FROM movimientos WHERE fecha BETWEEN :fechaInicio AND :fechaFin")
    fun obtenerFlujoDelPeriodoFlow(fechaInicio: String, fechaFin: String): Flow<Double?>

    @Query("""
        SELECT SUM(saldo_rama) FROM (
            SELECT (SUM(CASE WHEN tipo = 1 THEN cantidad ELSE -cantidad END)) AS saldo_rama 
            FROM movimientos 
            GROUP BY COALESCE(parentId, id)
        ) WHERE saldo_rama > 0
    """)
    fun obtenerDisponibleRealPositivoFlow(): Flow<Double?>
}