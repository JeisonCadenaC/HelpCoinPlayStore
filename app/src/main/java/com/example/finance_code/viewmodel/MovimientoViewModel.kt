package com.example.finance_code.viewmodel

import androidx.lifecycle.ViewModel
import com.example.finance_code.data.Movimiento
import com.example.finance_code.data.MovimientoRepository

class MovimientoViewModel(private val repository: MovimientoRepository) : ViewModel() {
    val movimientos = repository.obtenerTodos()
    suspend fun insertar(movimiento: Movimiento) {
            repository.insertar(movimiento)
        }
    fun obtenerTodos(): kotlinx.coroutines.flow.Flow<List<Movimiento>> {
        return repository.obtenerTodos()
        }
}
