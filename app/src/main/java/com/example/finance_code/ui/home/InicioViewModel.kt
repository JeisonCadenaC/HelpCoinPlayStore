package com.example.finance_code.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.finance_code.data.AppDB
import com.example.finance_code.data.Recordatorio
import com.example.finance_code.data.RecordatorioDao
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val recordatorioDao: RecordatorioDao
    val todosLosRecordatorios: LiveData<List<Recordatorio>>
    private val userEmail: String

    init {
        userEmail = FirebaseAuth.getInstance().currentUser?.email
            ?: throw IllegalStateException("ViewModel: Email de usuario no puede ser nulo")

        val database = AppDB.getDatabase(application, userEmail)
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