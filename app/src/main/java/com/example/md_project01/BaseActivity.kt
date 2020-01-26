package com.example.md_project01

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
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

    protected fun checkPermissions(permission: Int): Boolean {
        return when(permission){
            REQUEST_PERMISSION_READWRITE_ID -> (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            REQUEST_PERMISSION_LOCATION_ID -> (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            else -> false
        }
    }

    protected fun requestPermissions(code: Int) {
        val permissions = when(code) {
            REQUEST_PERMISSION_READWRITE_ID -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            REQUEST_PERMISSION_LOCATION_ID -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else -> arrayOf()
        }

        if(permissions.isEmpty())
            return

        ActivityCompat.requestPermissions(
            this,
            permissions,
            code
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
        if (checkPermissions(REQUEST_PERMISSION_LOCATION_ID)) {
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
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
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

    protected fun setTextView(textView: TextView, text: String?) {
        if ( text != null )
            textView.text = text
    }

    fun showToast(msg: String?) {
        if (msg != null && msg.isNotEmpty())
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    class MyInsertTask internal constructor(context: Context, private val stat: Stats): AsyncTask<Void?, Void?, Boolean>() {

        //private val activityReference: WeakReference<MainActivity> = WeakReference(context)
        var db: StatDatabase? = null

        override fun doInBackground(vararg objs: Void?): Boolean {
            db ?: return false
            db!!.statDao().upsert(stat)
            return true
        }

        // onPostExecute runs on main thread
        override fun onPostExecute(bool: Boolean) {

        }

        init {
            db = openDB(context)
        }
    }

    class MyRetrieveTask internal constructor(context: Context, private val from: Date, private val callback: (stats: List<Stats> ) -> Unit): AsyncTask<Void?, Void?, List<Stats>?>() {
        var db: StatDatabase? = null

        override fun doInBackground(vararg voids: Void?): List<Stats>? {
            db ?: return null
            Log.d("RetrieveTask ######", "from: $from")
            return db!!.statDao().getLastNDays(from)
        }

        override fun onPostExecute(stats: List<Stats>?) {
            if (stats != null && stats.isNotEmpty()) {
                callback(stats)
            } else {
                Log.d("MyRetrieveTask #####", "No data found in DB")
            }
        }

        init {
            db = openDB(context)
        }

    }

    companion object {
        const val REQUEST_PERMISSION_LOCATION_ID = 42
        const val REQUEST_PERMISSION_READWRITE_ID = 1337
        val REGEX_FORECAST_CODES = "[acdfrstu][0-9]{2}[dn]".toRegex()
        const val DELAY_FORECAST = 1000 * 60 * 60 * 6 // 6 hours in milliseconds
        const val MAX_DISTANCE_SAME_FORECAST = 5000
        const val PREFS = "prefs"
        const val PREF_LATITUDE = "latitudeForecast"
        const val PREF_LONGITUDE = "longitudeForecast"
        const val PREF_TIME_LAST_FORECAST = "timeLastForecast"
        const val PREF_FORECAST = "forecast"
        const val DAYS_1 = 1000L * 60 * 60 * 24//  day in milliseconds
        const val DAYS_7 = 1000L * 60 * 60 * 24 * 7 // 7 days in milliseconds
        const val DAYS_30 = 1000L * 60 * 60 * 24 * 30 // 30 days in milliseconds
        const val DAYS_365 = 1000L * 60 * 60 * 24 * 365 // 365 days in milliseconds

        val sdf_statDB = SimpleDateFormat("dd-MM-yyyy", Locale.US)

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

        fun getStepDistance(prev: Location?, new: Location): Double {
            return if (prev != null)
                getStepDistance(lat1 = prev.latitude, lat2 = new.latitude, lng1 = prev.longitude, lng2 =  new.longitude)
            else
                0.0
        }

        fun openDB(context: Context): StatDatabase? {
            return StatDatabase.getInstance(context)
        }
    }
}
