package com.help.finance_code.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.help.finance_code.data.Movimiento
import com.help.finance_code.data.MovimientoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MovimientoViewModel(private val repository: MovimientoRepository) : ViewModel() {

    val movimientos = repository.obtenerTodos().asLiveData()

    val saldoTotal = repository.obtenerSaldoTotalFlow().asLiveData()

    fun insertar(movimiento: Movimiento): Job = viewModelScope.launch {
        repository.insertar(movimiento)
    }

    fun actualizar(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.actualizar(movimiento)
        }
    }

    fun eliminar(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.eliminar(movimiento)
        }
    }
}