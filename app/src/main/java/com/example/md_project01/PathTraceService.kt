package com.example.md_project01

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*


class PathTraceService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest = LocationRequest()
    var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult) // why? this. is. retarded. Android.
            updateTrace(locationResult.lastLocation)
        }
    }
    private lateinit var ps: PathService


    override fun onCreate() {
        super.onCreate()
        Log.d("[PathTraceService]", "create")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("[PathTraceService]", "Start")
        ps = PathService(
            System.currentTimeMillis(),
            intent!!.getStringExtra(EXTRA_DIR)!!
        )
        fusedLocationClient.requestLocationUpdates(
            this.locationRequest,
            this.locationCallback,
            Looper.myLooper()
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[PathTraceService]", "Destroy!")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        ps.save()
        ps.d_save() //DEBUG
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updateTrace(loc: Location?){
        if(loc != null){
            val newDistance = ps.addPoint(loc)

            if(newDistance != null){ //notify to running activity
                val intent = Intent()
                intent.action = NOTIFY
                intent.putExtra(EXTRA_DISTANCE, newDistance)
                sendBroadcast(intent)
            }
        }
    }


    companion object{
        val NOTIFY = "notify"
        val EXTRA_DIR = "dir"
        val EXTRA_DISTANCE = "distance"
    }

}
