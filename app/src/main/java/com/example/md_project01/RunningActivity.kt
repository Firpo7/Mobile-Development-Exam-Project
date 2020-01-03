package com.example.md_project01

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_running.*
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.WrapAroundMode
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.*


class RunningActivity : BaseActivity() {

    private lateinit var lastLocation: Location
    private var distance = 0.0
    private lateinit var locationDisplay: LocationDisplay
    private lateinit var scheduleTaskExecutor: ScheduledExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        
        getLocationWithCallback ( ::initializeMap )

        scheduleTaskExecutor = Executors.newScheduledThreadPool(1)
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }
    override fun onResume() {
        super.onResume()
        mapView.resume()
    }
    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
    }

    private fun initializeMap(location: Location?) {
        val lat: Double
        val lon: Double
        if (location != null) {
            lat = location.latitude
            lon = location.longitude
            showToast("lat: ${location.latitude}; lon: ${location.longitude}")
        } else {
            lat = 44.36; lon = 8.58 // this is random
            showToast("lat, lon: $lat, $lon")
        }
        val map = ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, lat, lon, 18)
        mapView.map = map

        mapView.wrapAroundMode = WrapAroundMode.DISABLED
        locationDisplay = mapView.locationDisplay
    }

    private fun setDistanceTextView(distance: Double?) {
        if ( distance != null )
            findViewById<TextView>(R.id.runningactivity_textview_distance).text = distance.toInt().toString()
    }

    private fun getStepDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 //meters
        val dLat = Math.toRadians((lat2 - lat1))
        val dLng = Math.toRadians((lng2 - lng1))
        val a =
            sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(
                Math.toRadians(lat2)
            ) *
                    sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c)
    }

    private fun getStepDistance(prev: Location, new: Location): Double {
        return getStepDistance(lat1 = prev.latitude, lat2 = new.latitude, lng1 = prev.longitude, lng2 =  new.longitude)
    }

    private fun updateDistance(location: Location?) {
        if (location != null) {
            if(::lastLocation.isInitialized) {
                distance += getStepDistance(lastLocation, location)
                lastLocation = location
            } else
                lastLocation = location
        } else {
            Log.d("RUNNING SCHEDULED TASK", "null Location!!")
        }
        Log.d("RUNNING SCHEDULED TASK", distance.toString())
        setDistanceTextView(distance)
    }

    fun startRunning(@Suppress("UNUSED_PARAMETER")v: View) {
        locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
        if (!locationDisplay.isStarted)
            locationDisplay.startAsync()

        /*This schedules a runnable task every second*/
        scheduleTaskExecutor.scheduleAtFixedRate( {
            runOnUiThread {
                getLocationWithCallback( ::updateDistance )
            }
        }, 0, 5, TimeUnit.SECONDS)

    }

    fun stopRunning(@Suppress("UNUSED_PARAMETER") view: View) {
        if( !scheduleTaskExecutor.isShutdown && !scheduleTaskExecutor.isTerminated)
            scheduleTaskExecutor.shutdown()
    }

    //DEBUG FUNCTION
    fun checkGetLocation(@Suppress("UNUSED_PARAMETER")view: View) {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        showToast(
                            if (location != null) {
                                "lat: ${location.latitude}; lon: ${location.longitude}"
                            } else {
                                "null Received!!"
                            }
                        )
                    }
            }
        }
    }
}