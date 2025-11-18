package com.example.finance_code.data

import kotlinx.coroutines.flow.Flow

class MovimientoRepository (private val movimientoDao: MovimientoDao){
    fun obtenerTodos(): Flow<List<Movimiento>> {
        return movimientoDao.obtenerTodos()
    }
    suspend fun insertar(movimiento: Movimiento) {
        movimientoDao.insertar(movimiento)
    }

    suspend fun actualizar(movimiento: Movimiento) {
        movimientoDao.actualizar(movimiento)
    }

    suspend fun eliminar(movimiento: Movimiento) {
        movimientoDao.eliminar(movimiento)
    }

    fun obtenerSaldoActualSincrono(): Double? {
        return movimientoDao.obtenerSaldoActualSincrono()
    }

    fun obtenerSaldoTotalFlow(): Flow<Double?> {
        return movimientoDao.obtenerSaldoTotalFlow()
    }
}