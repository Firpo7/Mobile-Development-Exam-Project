package com.example.md_project01

import android.content.Context
import android.location.Location
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.util.*
import kotlin.collections.ArrayList


class PathService(private val timestamp: Long = 0L, ctx: Context) {
    @Volatile
    var distanceMade: Double = 0.0
    val latitudes: ArrayList<Double> = ArrayList()
    val longitudes: ArrayList<Double> = ArrayList()

    val d_latitudes: ArrayList<Double> = ArrayList()
    val d_longitudes: ArrayList<Double> = ArrayList()
    private val buffer = PositionBuffer(10, 5)

    private val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

    fun addPoint(location: Location) {
        //Log.d("## addPoint ##", "lat=${location.latitude}, lon=${location.longitude}")
        val midp = buffer.add(Point(location.latitude, location.longitude))
        if(midp != null){
            latitudes.add(midp.lat)
            longitudes.add(midp.lon)
            //Log.d("## addPoint ##", "!+ lat=${midp.lat}, lon=${midp.lon}")
        }
        d_latitudes.add(location.latitude)
        d_longitudes.add(location.longitude)
    }

    fun save(): Boolean {
        if (timestamp == 0L) return false

        //distanceMade = distance
     /*   Log.d("[PathService]", "distanceMade: $distanceMade")
        Log.d("[PathService]", "latitudes: $latitudes")
        Log.d("[PathService]", "longitudes: $longitudes")
        Log.d("[PathService]", this.toString())*/

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
                Log.d("Write File Failed", e.message.toString())
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

    fun d_save(): Boolean {
        if (timestamp == 0L) return false

        //distanceMade = distance
        /*   Log.d("[PathService]", "distanceMade: $distanceMade")
           Log.d("[PathService]", "latitudes: $latitudes")
           Log.d("[PathService]", "longitudes: $longitudes")
           Log.d("[PathService]", this.toString())*/

        val filename = "d_$timestamp.json"
        Log.d("[PathService]", "$dir/$filename")
        return if (checkDir(dir)) {
            // directory exists or already created
            val dest = File(dir, filename)
            try {
                // response is the data written to file
                PrintWriter(dest).use { out -> out.println(this.d_toString()) }
                true
            } catch (e: Exception) {
                // handle the exception
                Log.d("Write File Failed", e.message.toString())
                false
            }

        } else {
            // directory creation is not successful
            false
        }
    }

    fun d_toString(): String {
        return "{\"distanceMade\": \"$distanceMade\",\"latitudes\":$d_latitudes,\"longitudes\":$d_longitudes}"
    }

    companion object {
        fun getPastPathFilesList(ctx: Context): List<File>? {
            val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()
            if (checkDir(dir)) {
                val dest = File(dir)
                return dest.listFiles()?.filter { f ->
                    try {
                        f.name.split(".")[0]//.toLong()

                        /*
                         * TODO: check that files contain a valid JSON (?)
                         */

                        true
                    } catch (e: NumberFormatException) {
                        false
                    }
                }
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

class PositionBuffer(private val bufSize: Int, private val flushDim: Int){
    private var buffer: LinkedList<Point> = LinkedList()

    fun add(p: Point): Point? {
        buffer.addLast(p)
        if(buffer.size == bufSize){
            val midp = this.midPoint()
            this.flush(flushDim)
            return midp
        }
        return null
    }

    fun midPoint(): Point {
        val p = Point(0.0,0.0)
        for(x in buffer){
            p.lat += x.lat
            p.lon += x.lon
        }
        p.lat /= buffer.size
        p.lon /= buffer.size
        return p
        //TODO: remove outlier from result (?)
    }

    fun flush(n: Int){
        if(buffer.size >= n) {
            for (i in 0 until n - 1)
                buffer.removeFirst()
        }
        else buffer.clear()
    }
}

class Point(var lat: Double, var lon: Double)