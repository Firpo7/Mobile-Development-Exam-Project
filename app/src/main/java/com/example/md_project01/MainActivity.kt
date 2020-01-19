package com.example.md_project01

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.GpsStatus
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class MainActivity : BaseActivity() {

    val DAY_LAYOUTS: IntArray = intArrayOf(R.id.day0,R.id.day1,R.id.day2,R.id.day3,R.id.day4,R.id.day5,R.id.day6)
    private var isForecastInit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addLocationListener {
            if(isLocationEnabled()){
                showToast("Location enabled")
                updateForecast()
            }
            else{
                //TODO: notify better
                showToast("Location disabled")
            }
        }
        updateForecast()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateForecast()
            }
        }
    }

    private fun getDayName(i: Int): String {
        return when(i) {
            1 -> "Sun"
            2 -> "Mon"
            3 -> "Tue"
            4 -> "Wed"
            5 -> "Thu"
            6 -> "Fri"
            7 -> "Sat"
            else -> "Err"
        }
    }

    private fun Context.resIdByName(resIdName: String?, resType: String): Int {
        resIdName?.let {
            return resources.getIdentifier(it, resType, packageName)
        }
        throw Resources.NotFoundException()
    }

    private fun setImageView(view: ImageView, forecast: String) {
        Log.d("[setImageView]", "code: $forecast")
        val id = applicationContext.resIdByName(forecast, "drawable")
        Log.d("[setImageView]", "id: $id")

        view.setImageResource(id)
    }

    private fun setImageViews(results: ArrayList<String>) {
        Log.d("[setImageViews]", "STARTING CYCLING RESULTS")
        for ( i in 0 until results.size) {
            setImageView(
                findViewById<FrameLayout>(DAY_LAYOUTS[i]).getChildAt(1) as ImageView,
                results[i]
            )
        }
    }

    private fun setSuggestionTextView(forecast: String) {
        val textView = findViewById<TextView>(R.id.mainactivity_textview_run_suggestion)

        setTextView(
            textView,
            when(forecast[0]){
                'a', 'c' -> resources.getString(R.string.mainactivity_textview_run_suggestion_good)
                else -> resources.getString(R.string.mainactivity_textview_run_suggestion_bad)
            }
        )
    }

    private fun doForecast(location: Location?) {
        if(location != null) {
            val forecast = ArrayList<String>()
            val sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val prefLatitude = sharedPreferences.getString(PREF_LATITUDE, "0.0")!!.toDouble()
            val prefLongitude = sharedPreferences.getString(PREF_LONGITUDE, "0.0")!!.toDouble()
            val prefTimeLastForecast = sharedPreferences.getLong(PREF_TIME_LAST_FORECAST, 0)

            if ((prefLatitude != 0.0 || prefLongitude != 0.0) &&
                prefTimeLastForecast != 0L &&
                abs(System.currentTimeMillis() - prefTimeLastForecast) < DELAY_FORECAST &&
                getStepDistance(
                    lat1 = prefLatitude,
                    lng1 = prefLongitude,
                    lat2 = location.latitude,
                    lng2 = location.longitude
                ) < MAX_DISTANCE_SAME_FORECAST
            ) {

                val prefForecast = sharedPreferences.getString(PREF_FORECAST, "[]")
                forecast.addAll(parseStringToForecastsList(prefForecast!!))

            }

            if ( forecast.isNotEmpty() ) {
                Log.d("[MainActivity]", "Loading forecasts from sharedPreferences")
                setImageViews(forecast)
                setSuggestionTextView(forecast[0])
            } else {
                Log.d("[MainActivity]", "Loading forecasts from API")
                QueryWeatherService.doForecast(
                    location.latitude,
                    location.longitude
                ) { results: ArrayList<String> ->
                    setImageViews(results)
                    setSuggestionTextView(results[0])

                    val e: SharedPreferences.Editor = sharedPreferences.edit()
                    e.putString(PREF_FORECAST, results.toString())
                    e.putLong(PREF_TIME_LAST_FORECAST, System.currentTimeMillis())
                    Log.d("[DO_FORECAST_CALLBACK]", results.toString())
                    e.apply()
                }
            }

            val e: SharedPreferences.Editor = sharedPreferences.edit()
            e.putString(PREF_LATITUDE, location.latitude.toString())
            e.putString(PREF_LONGITUDE, location.longitude.toString())
            e.apply()
        }
    }

    private fun initForecast(){
        val b: Button = findViewById(R.id.reload_forecast)

        if(checkPermissions(REQUEST_PERMISSION_LOCATION_ID)){
            b.visibility = View.GONE
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            val width = displayMetrics.widthPixels
            //val height = displayMetrics.heightPixels

            val frameWidth = width / DAY_LAYOUTS.size

            val currDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

            for (i in DAY_LAYOUTS.indices) {
                val currFrame = findViewById<FrameLayout>(DAY_LAYOUTS[i])

                currFrame.layoutParams.width = frameWidth
                currFrame.layoutParams.height = frameWidth + 25.px

                (currFrame.getChildAt(0) as TextView).text = getDayName( ( (currDayOfWeek + i) % 7 ) + 1 )
            }
            isForecastInit = true
        }
        else{
            b.visibility = View.VISIBLE
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
        }
    }

    fun updateForecast(v: View? = null){
        if(!isForecastInit)
            initForecast()
        if(isForecastInit) { //no one ELSE here, please
            getLocationWithCallback { location: Location? ->
                doForecast(location)
            }
        }
    }

    fun startRunPressed(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!checkPermissions(REQUEST_PERMISSION_LOCATION_ID)){
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
        }
        else if (!checkPermissions(REQUEST_PERMISSION_READWRITE_ID)){
            requestPermissions(REQUEST_PERMISSION_READWRITE_ID)
        }
        else{
            val runningIntent = Intent(this, RunningActivity::class.java)
            startActivity(runningIntent)
        }
    }

}
