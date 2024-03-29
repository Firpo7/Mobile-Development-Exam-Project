package com.example.md_project01

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import java.util.*
import kotlin.collections.ArrayList


open class BaseActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var broadcastRec: BroadcastReceiver
    protected var LOG_TAG = "BaseActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG,"onCreate()")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG,"onResume()")
        if(::broadcastRec.isInitialized)
            registerReceiver(broadcastRec, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG,"onPause()")
        if(::broadcastRec.isInitialized)
            unregisterReceiver(broadcastRec)
    }

    protected fun checkPermissions(permission: Int): Boolean {
        return when(permission){
            REQUEST_PERMISSION_READWRITE_ID -> (
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    )
            REQUEST_PERMISSION_LOCATION_ID -> (
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    )
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

    protected fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    protected fun getLocationWithCallback(callback: ( location: Location? ) -> Unit) : Boolean {
        return if (checkPermissions(REQUEST_PERMISSION_LOCATION_ID)) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        callback(location)
                    }
                true
            } else {
                showToastErrorLocationDisabled()
                false
            }
        } else {
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
            false
        }
    }

    protected fun addLocationListener(callback: () -> Unit) {
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
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun showToastErrorLocationDisabled() {
        showToast(getResourceString(R.string.error_location_disabled))
    }

    class MyInsertTask internal constructor(context: Context, private val stat: Stats): AsyncTask<Void?, Void?, Boolean>() {
        private var db: StatDatabase? = openDB(context)

        override fun doInBackground(vararg objs: Void?): Boolean {
            db ?: return false
            db!!.statDao().upsert(stat)
            return true
        }

    }

    class MyRetrieveTask internal constructor(context: Context, private val from: Date, private val callback: (stats: List<Stats> ) -> Unit): AsyncTask<Void?, Void?, List<Stats>?>() {
        private var db: StatDatabase? = openDB(context)

        override fun doInBackground(vararg voids: Void?): List<Stats>? {
            return db?.statDao()?.getLastNDays(from)
        }

        override fun onPostExecute(stats: List<Stats>?) {
            if (stats != null) {
                callback(stats)
            } else {
                Log.d("[MyRetrieveTask]", "Error while retrieving datas in DB")
            }
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

        fun openDB(context: Context): StatDatabase? {
            return StatDatabase.getInstance(context)
        }
    }

}


