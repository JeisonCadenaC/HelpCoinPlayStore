package com.example.finance_code.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val descripcion: String,
    val cantidad: Double,
    val tipo: Int,
    val fecha: String,
    val categoria: String,
    val categoriaId: Long = 5L,
    val hora: String = "00:00:00"
): Parcelable