package com.example.finance_code.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

//Aquí van las funciones (insertar, leer, etc.).
//DAO = Data Access Object
//Acceso a los objetos de datos

@Dao //Le dice a Room que esta interface es el puente hacia la base de datos.
interface MovimientoDao {
    //Porque estas funciones corren en corrutinas (es decir, en segundo plano, para que la app no se trabe).
    @Insert
    suspend fun insertar(movimiento: Movimiento)
    @Query("SELECT * FROM movimientos")
    fun obtenerTodos(): kotlinx.coroutines.flow.Flow<List<Movimiento>>


}