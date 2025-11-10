package com.example.finance_code.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MetaViewModelFactory(private val application: Application)
    : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}