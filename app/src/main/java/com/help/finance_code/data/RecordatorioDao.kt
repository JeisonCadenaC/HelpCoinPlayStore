package com.help.finance_code.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecordatorioDao {

    @Insert
    suspend fun insertar(recordatorio: Recordatorio)

    @Delete
    suspend fun eliminar(recordatorio: Recordatorio)


    @Query("SELECT * FROM recordatorios ORDER BY fechaMillis ASC")
    fun obtenerTodos(): LiveData<List<Recordatorio>>
}