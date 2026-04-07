package com.help.finance_code.data

import androidx.lifecycle.LiveData

class MetaRepository(private val metaDao: MetaDao) {

    val allLocalMetas: LiveData<List<MetaDB>> = metaDao.getAllLocalMetas()

    suspend fun insert(metaDB: MetaDB) = metaDao.insert(metaDB)

    suspend fun update(metaDB: MetaDB) = metaDao.update(metaDB)

    suspend fun delete(metaDB: MetaDB) = metaDao.delete(metaDB)
}