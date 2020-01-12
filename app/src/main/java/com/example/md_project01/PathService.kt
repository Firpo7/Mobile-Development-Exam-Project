package com.example.md_project01

import android.content.Context
import android.location.Location
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import kotlin.collections.ArrayList


class PathService(private val timestamp: Long = 0L, ctx: Context) {
    var distanceMade: Long = 0
    val latitudes: ArrayList<Double> = ArrayList()
    val longitudes: ArrayList<Double> = ArrayList()

    private val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

    fun addPoint(location: Location) {
        latitudes.add( location.latitude )
        longitudes.add( location.longitude)
    }

    fun save(distance: Long): Boolean {
        if (timestamp == 0L) return false

        distanceMade = distance
        Log.d("[PathService]", "distanceMade: $distanceMade")
        Log.d("[PathService]", "latitudes: $latitudes")
        Log.d("[PathService]", "longitudes: $longitudes")
        Log.d("[PathService]", this.toString())

        val filename = "$timestamp.json"
        Log.d("[PathService]", "$dir/$filename")
        return if (checkDir(dir)) {
            // directory exists or already created
            val dest = File(dir, filename)
            try {
                // response is the data written to file
                PrintWriter(dest).use { out -> out.println(this.toString()) }
                true
            } catch (e: Exception) {
                // handle the exception
                Log.d("Write File Failed", e.message)
                false
            }

        } else {
            // directory creation is not successful
            false
        }
    }

    override fun toString(): String {
        return "{\"distanceMade\": \"$distanceMade\",\"latitudes\":$latitudes,\"longitudes\":$longitudes}"
    }

    companion object {
        fun getPastPathFilesList(ctx: Context): Array<File>? {
            val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()
            if (checkDir(dir)) {
                val dest = File(dir)
                return dest.listFiles()
            }

            return null
        }

        private fun checkDir(dir: String): Boolean {
            val saveDir = File(dir)
            if (!saveDir.exists()) {
                return saveDir.mkdir()
            }
            return true
        }
    }
}