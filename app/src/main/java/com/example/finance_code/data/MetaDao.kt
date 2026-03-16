package com.example.finance_code.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MetaDao {

    @Query("SELECT * FROM metas_table WHERE usuarios = '' OR usuarios = '[]' ORDER BY orden ASC, completada ASC, fechaLimite IS NULL, fechaLimite")
    fun getAllLocalMetas(): LiveData<List<MetaDB>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metaDB: MetaDB)

    @Update
    suspend fun update(metaDB: MetaDB)

    @Delete
    suspend fun delete(metaDB: MetaDB)
}