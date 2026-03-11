package com.example.finance_code.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val emoji: String,
    val colorHex: String,
    val esPersonalizada: Boolean = false,
    val palabrasClave: String = ""
): Parcelable