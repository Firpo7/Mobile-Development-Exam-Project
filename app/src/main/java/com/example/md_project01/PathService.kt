package com.example.md_project01

import android.content.Context
import android.location.Location
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class PathService(private val timestamp: Long, ctx: Context) {
    private var distanceMade: Long = 0
    private val latitudes: ArrayList<Double> = ArrayList()
    private val longitudes: ArrayList<Double> = ArrayList()

    private val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

    private fun checkSaveDir(): Boolean {
        val saveDir = File(dir)
        if (!saveDir.exists()) {
            return saveDir.mkdir()
        }
        return true
    }

    fun addPoint(location: Location) {
        latitudes.add( location.latitude )
        longitudes.add( location.longitude)
    }

    fun save(distance: Long): Boolean {
        distanceMade = distance
        Log.d("[PathService]", "distanceMade: $distanceMade")
        Log.d("[PathService]", "latitudes: $latitudes")
        Log.d("[PathService]", "longitudes: $longitudes")
        Log.d("[PathService]", this.toString() )

        val sdf = SimpleDateFormat("dd_MM_yyyy-HH_mm", Locale.US)
        val filename = "${sdf.format(Date(timestamp))}.json"
        Log.d("[PathService]", "$dir/$filename")
        return if (checkSaveDir()) {
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
}