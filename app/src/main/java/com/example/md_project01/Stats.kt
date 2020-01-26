package com.example.md_project01

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "stats")
data class Stats(
    @PrimaryKey val date: Date,
    @ColumnInfo(name = "distance") var distance: Long
)