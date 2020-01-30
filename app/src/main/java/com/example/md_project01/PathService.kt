package com.example.md_project01

import android.location.Location
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt



fun getStepDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 6371000.0 //meters
    val dLat = Math.toRadians((lat2 - lat1))
    val dLng = Math.toRadians((lng2 - lng1))
    val a = sin(dLat/2) * sin(dLat/2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng/2) * sin(dLng/2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (earthRadius * c)
}

fun getStepDistance(prev: Location?, new: Location): Double {
    return if (prev != null)
        getStepDistance(lat1 = prev.latitude, lat2 = new.latitude, lng1 = prev.longitude, lng2 =  new.longitude)
    else
        0.0
}


class PathService(private val timestamp: Long = 0L, val dir: String/*ctx: Context*/) {
    @Volatile
    var distanceMade: Double = 0.0
    var lastLocation: Location? = null
    val latitudes: ArrayList<Double> = ArrayList()
    val longitudes: ArrayList<Double> = ArrayList()

    //val d_latitudes: ArrayList<Double> = ArrayList()
    //val d_longitudes: ArrayList<Double> = ArrayList()
    private val buffer = PositionBuffer(10, 5)


    //private val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

    fun addPoint(location: Location): Double? {
        Log.d("[PathService]", "coord=${location.latitude},${location.longitude}")
        //d_latitudes.add(location.latitude)
        //d_longitudes.add(location.longitude)
        val midp = buffer.add(Point(location.latitude, location.longitude))
        if(midp != null){
            Log.d("[PathService]", "ADD! coord=${midp.lat},${midp.lon}")
            latitudes.add(midp.lat)
            longitudes.add(midp.lon)

            if (lastLocation != null)
                distanceMade += getStepDistance(lastLocation, location)

            lastLocation = location
            return distanceMade
        }
        return null
    }

    fun save(): Boolean {
        if (timestamp == 0L || latitudes.isEmpty() || longitudes.isEmpty()) return false
        //save() and addPoint() are thread safe?

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
        //return "{\"distanceMade\": \"$distanceMade\",\"latitudes\":$latitudes,\"longitudes\":$longitudes}"
        mal.lock()
        try {
            return "{\"distanceMade\": \"$distance\",\"latitudes\":$_latitudes,\"longitudes\":$_longitudes}"
        }
        finally {
            mal.unlock()
        }
    }

    /* DEBUG STUFF (maybe can help in future)
    fun d_save(): Boolean {
        if (timestamp == 0L) return false

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
    */

    /*🡓🡓🡓 Test for "static" data 🡓🡓🡓*/

    fun addNewPoint(location: Location): Double? {
        Log.d("[PathService]", "coord=${location.latitude},${location.longitude}")
        val midp = buffer.add(Point(location.latitude, location.longitude))
        if(midp != null){
            Log.d("[PathService]", "ADD! coord=${midp.lat},${midp.lon}")

            mal.lock()
            _latitudes.add(midp.lat)
            _longitudes.add(midp.lon)

            if (lastLocation != null)
                distance += getStepDistance(lastLocation, location)

            mal.unlock()

            lastLocation = location
            return distanceMade
        }
        return null
    }

    companion object {
        private val _longitudes: MutableList<Double> = mutableListOf()
        private val _latitudes: MutableList<Double> = mutableListOf()
        private var distance = 0.0
        private val mal = ReentrantLock()

        fun getLatitude(): MutableList<Double>{
            mal.lock() // C was here!
            try{
                return _latitudes //TODO: copy?
            }
            finally {
                mal.unlock()
            }
        }

        fun getLongitude(): MutableList<Double>{
            mal.lock()
            try{
                return _longitudes //TODO: copy?
            }
            finally {
                mal.unlock()
            }
        }

        fun getDistance(): Double{
            mal.lock()
            try{
                return distance
            }
            finally {
                mal.unlock()
            }
        }

        fun clearData(){
            mal.lock()
            _latitudes.clear()
            _longitudes.clear()
            distance *= 0
            mal.unlock()
        }

        /*🡑🡑🡑 Test for "static" data 🡑🡑🡑*/

        fun getPastPathFilesList(dir: String): List<File>? {
            //val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()
            if (checkDir(dir)) {
                val dest = File(dir)
                return dest.listFiles()?.filter { f ->
                    try {
                        f.name.split(".")[0].toLong()

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