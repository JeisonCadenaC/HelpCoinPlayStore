package com.example.finance_code.data
// Aquí van las consultas

class MovimientoRepository (private val movimientoDao: MovimientoDao){
    fun obtenerTodos():kotlinx.coroutines.flow.Flow<List<Movimiento>> {
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

}