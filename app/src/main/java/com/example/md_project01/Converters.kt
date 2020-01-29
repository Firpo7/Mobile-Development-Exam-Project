package com.example.md_project01

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*

class Converters {
    @TypeConverter
    fun fromDayStringToDate(value: String): Date? {
        return sdf.parse(value)
    }

    @TypeConverter
    fun dateToDayString(date: Date): String? {
        return sdf.format(date)
    }
    companion object {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}