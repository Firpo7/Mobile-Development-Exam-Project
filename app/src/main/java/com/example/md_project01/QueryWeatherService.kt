package com.example.md_project01

import android.util.Log
import org.json.*
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header

class QueryWeatherService() {
    val WEATHERBIT_TOKEN = "9234f1e48c8345d59ec2fac5879955e3"
    val BASE_URL = "http://api.weatherbit.io/v2.0/forecast/daily"
    val DAYS_TO_QUERY = 7

    private fun performRequest(urlString: String, callback: (result: ArrayList<String>) -> Unit) {

        val client = AsyncHttpClient()
        val forecasts = ArrayList<String>()

        //Log.d("LOG_RESOURCES", R.drawable.a01d.toString())

        client.get(urlString, object: JsonHttpResponseHandler()
        {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, jsonObject: JSONObject?) {
                if(jsonObject != null) {
                    Log.d("QUERY", jsonObject.toString())

                    val arr = jsonObject.getJSONArray("data")
                    for ( i in 0 until arr.length())
                    {
                        val curr = arr.getJSONObject(i)
                        forecasts.add(curr.getJSONObject("weather").getString("icon"))
                        Log.d("$i forecast result", curr.toString())
                    }

                    callback(forecasts)
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>?, e: Throwable, jsonObject: JSONObject?) {
                Log.e("QUERY failed", "$statusCode ${e.message}")
            }
        })

    }


    fun doForecast(lat: Double, lon: Double, callback: (result: ArrayList<String>) -> Unit) {
        val forgeURL = "${BASE_URL}?days=${DAYS_TO_QUERY}&lat=${lat}&lon=${lon}&key=${WEATHERBIT_TOKEN}"
        performRequest(forgeURL, callback)
    }

}