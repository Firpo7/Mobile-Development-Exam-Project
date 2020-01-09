package com.example.md_project01

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


open class BaseActivity : AppCompatActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    protected fun checkPermissions(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    protected fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSION_LOCATION_ID
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode ==
            REQUEST_PERMISSION_LOCATION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Granted. Start getting the location information
                return
            }
        }
    }

    protected fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    protected fun getLocationWithCallback(callback: ( location: Location? ) -> Unit) {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        callback(location)
                    }
            }
            else {
                //TODO: show something better?
                showToast("Enable location to see weather and position")
            }
        } else {
            requestPermissions()
        }
    }

    protected fun parseStringToForecastsList(s: String): ArrayList<String> {
        val ret = ArrayList<String>()
        if(s.length > 4) {
            val rgx = REGEX_FORECAST_CODES
            var m = rgx.find(s)
            do {
                ret.add(m?.value.toString())
            } while ({ m = m?.next(); m }() != null)
        }
        return ret
    }

    fun showToast(msg: String?) {
        if (msg != null && msg.isNotEmpty())
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val REQUEST_PERMISSION_LOCATION_ID = 42
        val REGEX_FORECAST_CODES = "[acdfrstu][0-9]{2}[dn]".toRegex()
        const val DELAY_FORECAST = 1000 * 60 * 60 * 6 // 6 hours in milliseconds
        const val MAX_DISTANCE_SAME_FORECAST = 5000
        const val PREFS = "prefs"
        const val PREF_LATITUDE = "latitudeForecast"
        const val PREF_LONGITUDE = "longitudeForecast"
        const val PREF_TIME_LAST_FORECAST = "timeLastForecast"
        const val PREF_FORECAST = "forecast"

        val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
        val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

        fun getStepDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
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

        fun getStepDistance(prev: Location, new: Location): Double {
            return getStepDistance(lat1 = prev.latitude, lat2 = new.latitude, lng1 = prev.longitude, lng2 =  new.longitude)
        }
    }
}
