package com.example.finance_code.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "metas_table")
data class MetaDB(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val montoObjetivo: Double = 0.0,
    val montoActual: Double = 0.0,
    val fechaLimite: Long? = null,
    val completada: Boolean = false,
    val fechaCreacion: Long? = System.currentTimeMillis(),
    val usuarios: List<String> = emptyList(),
    val invitaciones: List<String> = emptyList(),
    val imagenUrl: String? = null,
    val orden: Int = 0
)