package com.example.finance_code.data

class MovimientoRepository (private val movimientoDao: MovimientoDao){
    fun obtenerTodos():kotlinx.coroutines.flow.Flow<List<Movimiento>> {
        return movimientoDao.obtenerTodos()
    }
    suspend fun insertar(movimiento: Movimiento) {
        movimientoDao.insertar(movimiento)
    }
}