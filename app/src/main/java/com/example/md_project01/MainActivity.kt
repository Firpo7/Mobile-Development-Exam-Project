package com.example.md_project01

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : BaseActivity() {

    val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val DAY_LAYOUTS: IntArray = intArrayOf(R.id.day0,R.id.day1,R.id.day2,R.id.day3,R.id.day4,R.id.day5,R.id.day6)
    val WEATHERBIT_TOKEN = "9234f1e48c8345d59ec2fac5879955e3"
    val BASE_URL = "http://api.weatherbit.io/v2.0/forecast/daily"

    var lon: Double = 0.0
    var lat: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val frameWidth = width / DAY_LAYOUTS.size

        val currDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

        for ( i in DAY_LAYOUTS.indices) {
            val currFrame = findViewById<FrameLayout>(DAY_LAYOUTS[i])

            currFrame.layoutParams.width = frameWidth
            currFrame.layoutParams.height = frameWidth + 25.px

            (currFrame.getChildAt(0) as TextView).text = getDayName( ( (currDayOfWeek + i) % 7 ) + 1 )
        }

        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.d("[Running Activity]", "altitude: ${location.altitude}")
                            Log.d("[Running Activity]", "longitude: ${location.longitude}")
                            Log.d("[Running Activity]", "latitude: ${location.latitude}")
                            Log.d("[Running Activity]", "time: ${location.time}")
                            lon = location.longitude
                            lat = location.latitude
                        }
                    }
            } else {
                showToast("Turn on location")
            }
        } else {
            Log.d("PERMISSIONS NOT GRANTED", "NO NO")
            showToast("Permissions not granted")
            requestPermissions()
        }

        doForecast(7, lat,lon)

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


    private fun performRequest(urlString: String) {

        val client = AsyncHttpClient()
        var forecasts = ArrayList<String>()

        Log.d("LOG_RESOURCES", R.drawable.a01d.toString())
        Log.d("LOG_RESOURCES", R.drawable.a02d.toString())


        client.get(urlString, object: JsonHttpResponseHandler()
        {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, jsonObject: JSONObject?) {
                if(jsonObject != null) {
                    Log.d("QUERY", jsonObject.toString())

                    val arr = jsonObject.getJSONArray("data")
                    for ( i in 0 until arr.length())
                    {
                        val d = arr.getJSONObject(i)
                        Log.d("post_id", d.toString())
                        setImageView(
                            findViewById<FrameLayout>(DAY_LAYOUTS[i]).getChildAt(1) as ImageView,
                            d.getJSONObject("weather").getString("icon")
                        )

                    }

                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>?, e: Throwable, jsonObject: JSONObject?) {
                Log.e("QUERY failed", "$statusCode ${e.message}")
            }
        })

    }

    private fun doForecast(days: Int, lat: Double, lon: Double) {
        val forgeURL = "${BASE_URL}?days=${days}&lat=${lat}&lon=${lon}&key=${WEATHERBIT_TOKEN}"
        performRequest(forgeURL)
    }

    fun startRunPressed(@Suppress("UNUSED_PARAMETER") view: View) {
        val runningIntent = Intent(
            this,
            RunningActivity::class.java)

        startActivity(runningIntent)
    }

}
