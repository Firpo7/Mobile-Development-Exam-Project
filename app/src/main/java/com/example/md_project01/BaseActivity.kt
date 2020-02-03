package com.example.md_project01

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


open class BaseActivity : AppCompatActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var broadcastRec: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    override fun onResume() {
        super.onResume()
        if(::broadcastRec.isInitialized)
            registerReceiver(broadcastRec, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun onPause() {
        super.onPause()
        if(::broadcastRec.isInitialized)
            unregisterReceiver(broadcastRec)
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
            REQUEST_PERMISSION_LOCATION_ID -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            REQUEST_PERMISSION_READWRITE_ID -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
                showToast( getResourceString(R.string.error_location_disabled) )
            }
        } else {
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
        }
    }

    protected fun addLocationListener(callback: () -> Unit){
        broadcastRec = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action!!.matches("android.location.PROVIDERS_CHANGED".toRegex()))
                    callback()
            }
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

    protected fun getResourceString(i: Int): String {
        return resources.getString(i)
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

        init {
            db = openDB(context)
        }
    }

    class MyRetrieveTask internal constructor(context: Context, private val from: Date, private val callback: (stats: List<Stats> ) -> Unit): AsyncTask<Void?, Void?, List<Stats>?>() {
        var db: StatDatabase? = null

        override fun doInBackground(vararg voids: Void?): List<Stats>? {
            db ?: return null
            Log.d("[MyRetrieveTask]", "retrieving from: $from")
            return db!!.statDao().getLastNDays(from)
        }

        override fun onPostExecute(stats: List<Stats>?) {
            if (stats != null) {
                callback(stats)
            } else {
                Log.d("MyRetrieveTask #####", "Error while retrieving datas in DB")
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
        const val PREF_DAYS_TO_SHOW = "days_to_show"
        const val DAYS_1 = 1000L * 60 * 60 * 24//  day in milliseconds

        val sdf_statDB = SimpleDateFormat("dd-MM-yyyy", Locale.US)

        val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
        val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

        fun openDB(context: Context): StatDatabase? {
            return StatDatabase.getInstance(context)
        }

    }

}


