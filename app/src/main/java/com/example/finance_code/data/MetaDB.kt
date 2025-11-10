package com.example.finance_code.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metas_table")
data class MetaDB(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val montoObjetivo: Double,
    val montoActual: Double = 0.0,
    val fechaLimite: Long? = null,  // fecha límite opcional (en milisegundos)
    val completada: Boolean = false // cambia a true cuando se cumple la meta
)