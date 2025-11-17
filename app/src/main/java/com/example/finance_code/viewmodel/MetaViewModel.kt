package com.example.finance_code.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.MetaDB
import com.example.finance_code.data.MetaRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MetaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MetaRepository
    val allMetas: androidx.lifecycle.LiveData<List<MetaDB>>
    private val userEmail: String

    init {
        userEmail = FirebaseAuth.getInstance().currentUser?.email
            ?: throw IllegalStateException("ViewModel: Email de usuario no puede ser nulo")

        val metaDao = AppDB.getDatabase(application, userEmail).metaDao()
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