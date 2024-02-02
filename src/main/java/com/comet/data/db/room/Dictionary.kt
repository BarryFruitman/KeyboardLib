package com.comet.data.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionaries")
data class Dictionary(
    @PrimaryKey
    val language: String,
    val version: Float,
    val filename: String,
    val size: Int
)