package com.example.finance_code.data
import androidx.room.Entity
import androidx.room.PrimaryKey

//Aquí va la entidad (la tabla).

@Entity(tableName = "movimientos")
    data class Movimiento(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val descripcion: String,
        val cantidad: Double,
        val tipo: Int,
        val fecha: String,
        val categoria: String
    )

