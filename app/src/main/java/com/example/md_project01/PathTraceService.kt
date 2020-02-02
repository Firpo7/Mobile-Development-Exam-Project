package com.example.md_project01

//import android.R
import android.app.*
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.File
import java.io.PrintWriter
import java.util.*


class PathTraceService : Service() {
    private val CHANNEL_ID = "PathTraceServiceChannel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest = LocationRequest()
    var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult) // why? this. is. retarded. Android.
            updateTrace(locationResult.lastLocation)
        }
    }
    val longitudes: MutableList<Double> = mutableListOf()
    val latitudes: MutableList<Double> = mutableListOf()
    var distanceMade = 0.0
    private val buffer = PositionBuffer(10,5)
    private var lastLocation: Location? = null
    private lateinit var dir: String
    private var timestamp: Long = 0


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("[PathTraceService]", "createNotificationChannel")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground PathTraceService Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(channel)
        }
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("[PathTraceService]", "create")
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("[PathTraceService]", "Start")
        dir = intent!!.getStringExtra(EXTRA_DIR)!!
        timestamp = System.currentTimeMillis()
        fusedLocationClient.requestLocationUpdates(
            this.locationRequest,
            this.locationCallback,
            Looper.myLooper()
        )

        val pendingIntent = PendingIntent.getActivity(this,0, Intent(this, RunningActivity::class.java),  PendingIntent.FLAG_UPDATE_CURRENT)
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("You are running!")
            .setContentText("Tap here to open the app")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(2020, notification)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[PathTraceService]", "Destroy!")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        save()
        //ps.d_save() //DEBUG
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updateTrace(loc: Location?){
        Log.d("[PathTraceService]","updateTrace(${loc!=null})")
        if(loc != null){
            val newDistance = addNewPoint(loc)

            if(newDistance != null){ //notify to running activity
                Log.d("[PathTraceService]","sendBroadcast! (dist=$newDistance)")
                val intent = Intent(NOTIFY)
                intent.putExtra(EXTRA_DISTANCE, newDistance)
                sendBroadcast(intent)
            }
        }
    }

    private fun addNewPoint(location: Location): Double? {
        Log.d("[PathTraceService]", "coord=${location.latitude},${location.longitude}")
        val midp = buffer.add(Point(location.latitude, location.longitude))
        if(midp != null){
            Log.d("[PathTraceService]", "ADD! coord=${midp.lat},${midp.lon}")

            latitudes.add(midp.lat)
            longitudes.add(midp.lon)

            if (lastLocation != null)
                distanceMade += getStepDistance(lastLocation, location)

            lastLocation = location
            return distanceMade
        }
        return null
    }


    private fun save(): Boolean {
        if (timestamp == 0L || latitudes.isEmpty() || longitudes.isEmpty()) return false
        //save() and addPoint() are thread safe?

        val filename = "$timestamp.json"
        Log.d("[PathService]", "$dir/$filename")
        return if (checkDir(dir)) {
            // directory exists or already created
            val dest = File(dir, filename)
            try {
                // response is the data written to file
                PrintWriter(dest).use { out -> out.println(toJSON()) }
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

    private fun toJSON(): String {
        return "{\"distanceMade\": \"${distanceMade}\",\"latitudes\":${latitudes},\"longitudes\":${longitudes}}"
    }

    companion object{
        val NOTIFY = "notify"
        val EXTRA_DIR = "dir"
        val EXTRA_DISTANCE = "distance"


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
