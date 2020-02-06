package com.example.md_project01

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.random.Random



class MainActivity : BaseActivity() {

    private var isForecastInit = false
    private val DAY_LAYOUTS: IntArray = intArrayOf(R.id.day0,R.id.day1,R.id.day2,R.id.day3,R.id.day4,R.id.day5,R.id.day6)
    private lateinit var mChart: BarChart
    private var DAYS_TO_SHOW: Long = 7L
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (BuildConfig.DEBUG) findViewById<Button>(R.id.mainactivity_populate_stat_db).visibility = View.VISIBLE
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

        sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        DAYS_TO_SHOW = sharedPreferences.getLong(PREF_DAYS_TO_SHOW, DAYS_TO_SHOW)

        initializeChart()
        updateForecast()
    }

    override fun onStart() {
        super.onStart()
        updateChart()
    }

    private fun setChartValues(stats: List<Stats>) {
        if ( stats.isEmpty() ){
            mChart.description.isEnabled = true
            return
        }
        mChart.description.isEnabled = false
        val m = mutableMapOf<String, Stats>()
        val now = System.currentTimeMillis()
        var tmp = now - DAYS_1 * (DAYS_TO_SHOW-1)
        var k : String
        val valuesYList = mutableListOf<BarEntry>()
        val barEntryLabels = mutableListOf<String>()

        for ( s in stats) {
            m[sdf_statDB.format(s.date)] = s
        }

        for ( i in 0 until DAYS_TO_SHOW) {
            k = sdf_statDB.format( Date(tmp) )
            barEntryLabels.add(k)
            if ( !m.containsKey(k) ) {
                valuesYList.add( BarEntry(i.toFloat(), 0f ) )
            } else {
                valuesYList.add( BarEntry(i.toFloat(), m[k]!!.distance.toFloat()) )
            }
            tmp += DAYS_1
        }

        val barYSet = BarDataSet(valuesYList, "m")
        barYSet.color = if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
                            resources.getColor(R.color.color_dark_orange, theme)
                        else
                            resources.getColor(R.color.color_dark_orange)
        
        val data = BarData(barYSet)
        mChart.xAxis.valueFormatter = IndexAxisValueFormatter(barEntryLabels)
        mChart.data = data

        mChart.notifyDataSetChanged()
        mChart.invalidate()
    }

    private fun initializeChart() {
        mChart = findViewById(R.id.mainactivity_barchart_statistics)
        //mChart.setBackgroundColor(Color.WHITE)
        val d = Description()
        d.text = getResourceString(R.string.no_chart_data_found)
        mChart.description = d
        mChart.description.isEnabled = false
        mChart.legend.isEnabled = false
        mChart.setTouchEnabled(false)
        mChart.axisLeft.setDrawLabels(false)
        mChart.animateY(500)

        val xAxis = mChart.xAxis
        xAxis.textSize = 9f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)

        mChart.data = BarData()
    }

    private fun updateChart() {
        MyRetrieveTask(this@MainActivity, Date(System.currentTimeMillis() - DAYS_1 * DAYS_TO_SHOW), ::setChartValues).execute()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateForecast()
            }
        }
    }

    private fun getDayName(i: Int): String {
        return getResourceString(when(i) {
            1 -> R.string.day_sunday
            2 -> R.string.day_monday
            3 -> R.string.day_tuesday
            4 -> R.string.day_wednesday
            5 -> R.string.day_thursday
            6 -> R.string.day_friday
            7 -> R.string.day_saturday
            else -> R.string.day_error
        })
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
        else{
            Log.d("[MainActivity]","location = null")
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
                val currConstraintLayout = findViewById<ConstraintLayout>(DAY_LAYOUTS[i])
                currConstraintLayout.layoutParams.width = frameWidth

                (currConstraintLayout.getChildAt(0) as TextView).text = getDayName( ( (currDayOfWeek + i) % 7 ) + 1 )
            }
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
        if (BuildConfig.DEBUG)
            for (i in 0..365) {
                val date = System.currentTimeMillis() - DAYS_1 * i
                MyInsertTask(this@MainActivity, Stats(Date(date), abs(Random.nextLong()%1000))).execute()
            }
    }

    private fun setDaysToShow(value: Long) {
        DAYS_TO_SHOW = value
        val e = sharedPreferences.edit()
        e.putLong(PREF_DAYS_TO_SHOW, DAYS_TO_SHOW)
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
