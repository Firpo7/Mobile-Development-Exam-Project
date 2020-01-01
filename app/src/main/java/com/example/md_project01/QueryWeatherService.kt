package com.example.md_project01

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.json.*
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header

class QueryWeatherService(private val dayImageViews: IntArray, private val ctx: Context) {
    val WEATHERBIT_TOKEN = "9234f1e48c8345d59ec2fac5879955e3"
    val BASE_URL = "http://api.weatherbit.io/v2.0/forecast/daily"

    private fun Context.resIdByName(resIdName: String?, resType: String): Int {
        resIdName?.let {
            return resources.getIdentifier(it, resType, packageName)
        }
        throw Resources.NotFoundException()
    }

    private fun setImageView(view: ImageView, forecast: String) {
        Log.d("[setImageView]", "code: $forecast")
        val id = ctx.resIdByName(forecast, "drawable")
        Log.d("[setImageView]", "id: $id")

        //view.setImageDrawable(context.getDrawable(id))
        view.setImageResource(id)
        //view.setImageDrawable(ContextCompat.getDrawable(context, id))
    }

    private fun performRequest(urlString: String): ArrayList<String> {

        var client = AsyncHttpClient()
        var forecasts = ArrayList<String>()

        Log.d("LOG_RESOURCES", R.drawable.a01d.toString())
        Log.d("LOG_RESOURCES", R.drawable.a02d.toString())


        client.get(urlString, object: JsonHttpResponseHandler()
        {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, jsonObject: JSONObject?) {
                if(jsonObject != null) {
                    Log.d("QUERY", jsonObject.toString())

                    var arr = jsonObject.getJSONArray("data");
                    for ( i in 0 until arr.length())
                    {
                        var post_id = arr.getJSONObject(i)
                        forecasts.add(post_id.getJSONObject("weather").getString("description"))
                        Log.d("post_id", post_id.toString())
                        setImageView(
                            LayoutInflater.from(ctx).inflate(R.layout.activity_main, null).findViewById<FrameLayout>(dayImageViews[i]).getChildAt(1) as ImageView,
                            post_id.getJSONObject("weather").getString("icon")
                        )

                    }

                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>?, e: Throwable, jsonObject: JSONObject?) {
                Log.e("QUERY failed", "${statusCode} ${e.message}")
            }
        })

        return forecasts
    }


    fun doForecast(days: Int, lat: Double, lon: Double): ArrayList<String> {
        var forgeURL = "${BASE_URL}?days=${days}&lat=${lat}&lon=${lon}&key=${WEATHERBIT_TOKEN}"
        return performRequest(forgeURL)
    }

}