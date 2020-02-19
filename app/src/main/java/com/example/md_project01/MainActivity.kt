package com.example.md_project01

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.random.Random


class MainActivity : BaseActivity() {

    private val WEEK_DAYS = listOf(
        R.string.day_sunday,
        R.string.day_monday,
        R.string.day_tuesday,
        R.string.day_wednesday,
        R.string.day_thursday,
        R.string.day_friday,
        R.string.day_saturday
    )
    private val DAY_LAYOUTS: IntArray = intArrayOf(R.id.day0,R.id.day1,R.id.day2,R.id.day3,R.id.day4,R.id.day5,R.id.day6)
    private var DAYS_TO_SHOW = 7
    private var isForecastInit = false
    private lateinit var barChartViewWrapper: ChartViewWrapper
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        super.LOG_TAG = "MainActivity"
        sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (BuildConfig.DEBUG) findViewById<Button>(R.id.mainactivity_populate_stat_db).visibility = View.VISIBLE

        addLocationListener {
            if(isLocationEnabled()){
                showToast("Location enabled")
                updateForecast()
            } else {
                showToast("Location disabled")
            }
        }

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val frameWidth = displayMetrics.widthPixels / DAY_LAYOUTS.size
        val currDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

        for (i in DAY_LAYOUTS.indices) {
            val currConstraintLayout = findViewById<ConstraintLayout>(DAY_LAYOUTS[i])
            currConstraintLayout.layoutParams.width = frameWidth
            (currConstraintLayout.getChildAt(0) as TextView).text = getDayName( (currDayOfWeek + i) % 7 )
        }

        barChartViewWrapper = ChartViewWrapper(
            findViewById(R.id.mainactivity_barchart_statistics),
            ContextCompat.getColor(this@MainActivity, R.color.color_dark_orange)
        )
        barChartViewWrapper.setDescriptionText(getResourceString(R.string.no_chart_data_found))
        DAYS_TO_SHOW = sharedPreferences.getInt(PREF_DAYS_TO_SHOW, DAYS_TO_SHOW)

        updateForecast()
    }

    override fun onStart() {
        super.onStart()
        updateChart()
    }

    private fun setChartValues(stats: List<Stats>) {
        if ( stats.isEmpty() ){
            barChartViewWrapper.setBarChartDescription(true)
            barChartViewWrapper.setChartValues(DAYS_TO_SHOW)
            return
        }
        barChartViewWrapper.setBarChartDescription(false)
        barChartViewWrapper.setChartValues(DAYS_TO_SHOW, stats)
    }

    private fun updateChart() {
        MyRetrieveTask(this@MainActivity, Date(System.currentTimeMillis() - DAYS_1 * DAYS_TO_SHOW)) { setChartValues(it) }.execute()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateForecast()
            }
        }
    }

    private fun getDayName(i: Int): String {
        return getResourceString(WEEK_DAYS[i])
    }

    private fun Context.resIdByName(resIdName: String?, resType: String): Int {
        resIdName?.let {
            return resources.getIdentifier(it, resType, packageName)
        }
        throw Resources.NotFoundException()
    }

    private fun setImageView(view: ImageView, forecast: String) {
        val id = applicationContext.resIdByName(forecast, "drawable")

        view.setImageResource(id)
    }

    private fun setImageViews(results: ArrayList<String>) {
        for ( i in 0 until results.size) {
            setImageView(
                findViewById<ConstraintLayout>(DAY_LAYOUTS[i]).getChildAt(1) as ImageView,
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
        textView.visibility = View.VISIBLE
    }

    private fun setForecastResults(forecast: ArrayList<String>) {
        setImageViews(forecast)
        setSuggestionTextView(forecast[0])
    }

    private fun doForecast(location: Location?) {
        if(location != null) {
            val forecast = ArrayList<String>()
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
                setForecastResults(forecast)
                Log.d(LOG_TAG, "doForecast(): LOADING FROM sharedPreferences")
            } else {
                QueryWeatherService.doForecast(
                    location.latitude,
                    location.longitude
                ) { results: ArrayList<String> ->
                    Log.d(LOG_TAG, "doForecast(): LOADING FROM API, results: $results")
                    setForecastResults(results)

                    val e: SharedPreferences.Editor = sharedPreferences.edit()
                    e.putString(PREF_FORECAST, results.toString())
                    e.putLong(PREF_TIME_LAST_FORECAST, System.currentTimeMillis())
                    e.putString(PREF_LATITUDE, location.latitude.toString())
                    e.putString(PREF_LONGITUDE, location.longitude.toString())
                    e.apply()
                }
            }
        }
        else{
            Log.d(LOG_TAG,"location = null")
        }
    }

    private fun initForecast(){
        val b: Button = findViewById(R.id.reload_forecast)

        if(checkPermissions(REQUEST_PERMISSION_LOCATION_ID)){
            b.visibility = View.INVISIBLE
            findViewById<LinearLayout>(R.id.mainactivity_linearlayout_forecast).visibility = View.VISIBLE
            isForecastInit = true
        }
        else{
            b.visibility = View.VISIBLE
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
        }
    }

    fun updateForecast(@Suppress("UNUSED_PARAMETER") v: View? = null){
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

    fun insertRandomValuesInDB(@Suppress("UNUSED_PARAMETER") v: View) {
        if (BuildConfig.DEBUG) {
            for (i in 0..365) {
                val date = System.currentTimeMillis() - DAYS_1 * i
                MyInsertTask(this@MainActivity, Stats(Date(date), abs(Random.nextLong()%1000))).execute()
            }
            updateChart()
        }
    }

    private fun setDaysToShow(value: Int) {
        DAYS_TO_SHOW = value
        val e = sharedPreferences.edit()
        e.putInt(PREF_DAYS_TO_SHOW, DAYS_TO_SHOW)
        e.apply()
        updateChart()
    }

    fun set7Days(@Suppress("UNUSED_PARAMETER")  v: View) {
        setDaysToShow(7)
    }

    fun set30Days(@Suppress("UNUSED_PARAMETER")  v: View) {
        setDaysToShow(30)
    }

    fun set365Days(@Suppress("UNUSED_PARAMETER")  v: View) {
        setDaysToShow(365)
    }

}
