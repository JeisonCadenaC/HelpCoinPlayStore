package com.example.finance_code.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metas_table")
data class MetaDB(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val montoObjetivo: Double,
    val montoActual: Double = 0.0,
    val fechaLimite: Long? = null,
    val completada: Boolean = false,
    val fechaCreacion: Long? = null
)