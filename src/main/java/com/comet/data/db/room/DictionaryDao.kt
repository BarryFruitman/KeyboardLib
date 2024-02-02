package com.comet.data.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionaries")
    fun getAll(): List<Dictionary>

    @Insert
    fun insertAll(vararg dictionaries: Dictionary)
}