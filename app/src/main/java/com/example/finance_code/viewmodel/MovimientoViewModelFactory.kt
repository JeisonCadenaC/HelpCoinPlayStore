package com.example.finance_code.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finance_code.data.MovimientoRepository

class MovimientoViewModelFactory(private val repository: MovimientoRepository) : ViewModelProvider.Factory{

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovimientoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovimientoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
 }
