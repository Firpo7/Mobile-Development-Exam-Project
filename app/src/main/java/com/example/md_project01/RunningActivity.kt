package com.example.md_project01

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_running.*
import kotlin.math.sqrt
import kotlin.math.pow

class RunningActivity : BaseActivity() {

    private lateinit var lastLocation: Location
    private var distance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)

        val map = ArcGISMap(Basemap.Type.TOPOGRAPHIC, 34.056295, -117.195800, 16)
        // set the map to be displayed in the layout's MapView
        mapView.map = map
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

    private fun getStepDistance(prev: Location, new: Location): Double {
        return sqrt(
            (prev.latitude - new.latitude).pow(2) + (prev.longitude - new.longitude).pow(2)
        )
    }

    fun startRunning(@Suppress("UNUSED_PARAMETER")v: View) {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            val s = "lat: ${location.latitude}; lon: ${location.longitude}"
                            //findViewById<TextView>(R.id.currLocation).text = s
                            Log.d("[Running Activity]", "altitude: ${location.altitude}")
                            Log.d("[Running Activity]", "longitude: ${location.longitude}")
                            Log.d("[Running Activity]", "latitude: ${location.latitude}")
                            Log.d("[Running Activity]", "time: ${location.time}")
                            if(::lastLocation.isInitialized) {
                                distance += getStepDistance(lastLocation, location)
                            }
                            lastLocation = location
                        } else {
                            Log.d("LOCATION ERROR", "null received")
                        }
                    }
            } else {
                showToast("Turn on location", Toast.LENGTH_LONG)
            }
        } else {
            Log.d("PERMISSIONS NOT GRANTED", "NO NO")
            showToast("Permissions not granted", Toast.LENGTH_LONG)
            requestPermissions()
        }
    }
}