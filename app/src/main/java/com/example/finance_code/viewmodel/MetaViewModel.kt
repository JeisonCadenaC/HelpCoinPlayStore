package com.example.finance_code.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.MetaDB
import com.example.finance_code.data.MetaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MetaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MetaRepository
    val allMetas: androidx.lifecycle.LiveData<List<MetaDB>>

    init {
        val metaDao = AppDB.getDatabase(application).metaDao()
        repository = MetaRepository(metaDao)
        allMetas = repository.allMetas
    }

    fun insert(metaDB: MetaDB) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(metaDB)
    }

    fun update(metaDB: MetaDB) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(metaDB)
    }

    fun delete(metaDB: MetaDB) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(metaDB)
    }
}