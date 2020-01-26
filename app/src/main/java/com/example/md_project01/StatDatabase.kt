package com.example.md_project01

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(entities = [Stats::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StatDatabase : RoomDatabase() {
    abstract fun statDao(): StatsDao

    companion object {
        var statDB: StatDatabase? = null
        val DB_NAME = "stat-db"

        fun getInstance(context: Context): StatDatabase? {
            if (null == statDB) {
                statDB = buildDatabaseInstance(context)
            }
            return statDB
        }

        private fun buildDatabaseInstance(context: Context): StatDatabase {
            return Room.databaseBuilder(
                context,
                StatDatabase::class.java,
                DB_NAME
            )
                .allowMainThreadQueries().build()
        }
    }
}