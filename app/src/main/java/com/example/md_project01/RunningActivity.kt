package com.example.md_project01

import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.WrapAroundMode
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.android.synthetic.main.activity_running.*


class RunningActivity : BaseActivity() {

    private lateinit var lastLocation: Location
    private var distance = 0.0
    private lateinit var locationDisplay: LocationDisplay

    private var locationCallback: LocationCallback? = null
    private lateinit var locationRequest: LocationRequest
    //private val locationSettingsRequest: LocationSettingsRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        
        getLocationWithCallback { location: Location? ->
            initializeMap(location)
        }

        this.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult) // why? this. is. retarded. Android.
                updateDistance(locationResult.lastLocation)
            }
        }

        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_MS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_MS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

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
        shutdownScheduledTask()
    }

    private fun shutdownScheduledTask() {
        fusedLocationClient.removeLocationUpdates(this.locationCallback)
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

    fun startRunning(@Suppress("UNUSED_PARAMETER") v: View) {
        if(::locationDisplay.isInitialized) {
            if (!locationDisplay.isStarted)
                locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
            locationDisplay.startAsync()
            fusedLocationClient.requestLocationUpdates(this.locationRequest, this.locationCallback, Looper.myLooper())
        }
        else {
            //TODO: do something better
            showToast("Unable to start running, is location enable?")
        }
    }

    fun stopRunning(@Suppress("UNUSED_PARAMETER") view: View) {
        shutdownScheduledTask()
        if(::locationDisplay.isInitialized)
            locationDisplay.stop()
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
        } else {
            requestPermissions()
        }
    }

    companion object {
        private const val UPDATE_INTERVAL_MS: Long = 1000
        private const val FASTEST_UPDATE_INTERVAL_MS: Long = 1000

    }
}