package com.example.finance_code.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Recordatorio
import com.example.finance_code.data.RecordatorioDao
import kotlinx.coroutines.launch

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val recordatorioDao: RecordatorioDao
    val todosLosRecordatorios: LiveData<List<Recordatorio>>

    init {

        val database = AppDB.getDatabase(application)
        recordatorioDao = database.recordatorioDao()

        todosLosRecordatorios = recordatorioDao.obtenerTodos()
    }


    fun insertarRecordatorio(recordatorio: Recordatorio) {

        viewModelScope.launch {
            recordatorioDao.insertar(recordatorio)
        }
    }


    fun eliminarRecordatorio(recordatorio: Recordatorio) {
        viewModelScope.launch {
            recordatorioDao.eliminar(recordatorio)
        }
    }
}