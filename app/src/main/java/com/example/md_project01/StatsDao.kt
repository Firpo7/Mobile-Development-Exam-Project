package com.example.md_project01

import android.util.Log
import androidx.room.*
import java.util.*


@Dao
interface StatsDao {
    @Query("SELECT * FROM stats")
    fun getAll(): List<Stats>

    @Query("SELECT * FROM stats WHERE date > (:from)")
    fun getLastNDays(from: Date): List<Stats>

    @Query("SELECT * FROM stats WHERE date = :choice")
    fun getDayStat(choice: Date): Stats?

    @Insert
    fun insert(stat: Stats): Long

    @Insert
    fun insertAll(vararg stats: Stats)

    @Delete
    fun delete(stat: Stats)

    @Update
    fun update(stat: Stats)

    @Transaction
    fun upsert(stat: Stats) {
        val s = getDayStat(stat.date)
        Log.d("UPSERT ##### s", s.toString())
        Log.d("UPSERT ##### stat", stat.toString())
        if (s == null) {
            insert(stat)
        } else {
            stat.distance += s.distance
            update(stat)
        }
    }
}