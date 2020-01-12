package com.example.md_project01

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.Polyline
import com.esri.arcgisruntime.geometry.SpatialReference
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.WrapAroundMode
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.android.synthetic.main.activity_running.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class RunningActivity : BaseActivity() {

    private lateinit var lastLocation: Location

    @Volatile
    private var distance = 0.0
    private lateinit var locationDisplay: LocationDisplay

    private var locationCallback: LocationCallback? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var graphicsOverlay: GraphicsOverlay
    private var lastGraphic: Graphic? = null

    private var pathService: PathService? = null
    //private val locationSettingsRequest: LocationSettingsRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        
        getLocationWithCallback { location: Location? ->
            initializeMap(location)
        }

        this.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult) // why? this. is. retarded. Android.
                updateDistance(locationResult.lastLocation)
            }
        }

        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_MS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_MS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }
    override fun onResume() {
        super.onResume()
        mapView.resume()
    }
    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
        shutdownScheduledTask()
    }

    private fun shutdownScheduledTask() {
        fusedLocationClient.removeLocationUpdates(this.locationCallback)
    }

    private fun initializeMap(location: Location?) {
        val lat: Double
        val lon: Double
        if (location != null) {
            lat = location.latitude
            lon = location.longitude
            showToast("lat: ${location.latitude}; lon: ${location.longitude}")
        } else {
            lat = 44.36; lon = 8.58 // this is random
            showToast("lat, lon: $lat, $lon")
        }
        val map = ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, lat, lon, 18)
        mapView.map = map

        graphicsOverlay = GraphicsOverlay()
        mapView!!.graphicsOverlays.add(graphicsOverlay)

        mapView.wrapAroundMode = WrapAroundMode.DISABLED
        locationDisplay = mapView.locationDisplay
    }

    private fun addPathLayer(ps: PathService) {
        val points = PointCollection(SpatialReference.create(GCS_WGS84))

        for (i in ps.latitudes.indices) {
            points.add(ps.longitudes[i], ps.latitudes[i])
        }

        val polyline = Polyline(points)
        val lineSymbol = SimpleLineSymbol(
            SimpleLineSymbol.Style.SOLID,
            Color.argb(255, 255, 255, 255), 5.0f
        )

        val graphic = Graphic(polyline, lineSymbol)

        graphicsOverlay.graphics.remove(lastGraphic)
        graphicsOverlay.graphics.add(graphic)
        lastGraphic = graphic
    }

    private fun setTextView(textView: TextView, text: String?) {
        if ( text != null )
            textView.text = distance.toInt().toString()
    }

    private fun updateDistance(location: Location?) {
        if (location != null) {
            pathService?.addPoint(location)
            if (::lastLocation.isInitialized) {
                distance += getStepDistance(lastLocation, location)
                lastLocation = location
            } else
                lastLocation = location
        } else {
            Log.d("RUNNING SCHEDULED TASK", "null Location!!")
        }
        Log.d("RUNNING SCHEDULED TASK", distance.toString())
        setTextView(findViewById(R.id.runningactivity_textview_distance), distance.toString())
    }

    fun startRunning(@Suppress("UNUSED_PARAMETER") v: View) {
        if(::locationDisplay.isInitialized) {
            if (!locationDisplay.isStarted) {
                pathService = PathService(System.currentTimeMillis(), this@RunningActivity)
                locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
                locationDisplay.startAsync()
                fusedLocationClient.requestLocationUpdates(
                    this.locationRequest,
                    this.locationCallback,
                    Looper.myLooper()
                )
            }
        } else {
            //TODO: do something better
            showToast("Unable to start running, is location enable?")
        }
    }

    private fun savePathMade() {
        if (checkPermissions(REQUEST_PERMISSION_READWRITE_ID)) {
            if (pathService != null) {
                if (pathService!!.save(distance.toLong()).not())
                    showToast("ERROR WHILE WRITING THE FILE")
            }
            requestPermissions(REQUEST_PERMISSION_READWRITE_ID)
        }
    }

    fun stopRunning(@Suppress("UNUSED_PARAMETER") view: View) {
        if(::locationDisplay.isInitialized)
            if (locationDisplay.isStarted) {
                shutdownScheduledTask()
                savePathMade()
                locationDisplay.stop()
            }
    }

    //DEBUG FUNCTION
    fun checkGetLocation(@Suppress("UNUSED_PARAMETER") view: View) {
        if (checkPermissions(REQUEST_PERMISSION_LOCATION_ID)) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        showToast(
                            if (location != null) {
                                "lat: ${location.latitude}; lon: ${location.longitude}"
                            } else {
                                "null Received!!"
                            }
                        )
                    }
            }
        } else {
            requestPermissions(REQUEST_PERMISSION_LOCATION_ID)
        }
    }

    private fun loadPathFromJSON(json: String): PathService? {
        val ps = PathService(ctx = this@RunningActivity)

        val jsonObj = JSONObject(json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1))
        val latJson = jsonObj.getJSONArray("latitudes")
        val lonJson = jsonObj.getJSONArray("longitudes")
        for (i in 0 until latJson.length()) {
            try {
                ps.latitudes.add(latJson[i].toString().toDouble())
                ps.longitudes.add(lonJson[i].toString().toDouble())
            } catch (e: java.lang.Exception) {
                return null
            }
        }
        ps.distanceMade = jsonObj.getString("distanceMade").toLong()

        return ps
    }

    fun showPathHistories(@Suppress("UNUSED_PARAMETER") v: View) {
        val builder = AlertDialog.Builder(this@RunningActivity)
        builder.setTitle("Choose a path")

        val fileList = PathService.getPastPathFilesList(this@RunningActivity)?.filter { f ->
            try {
                f.name.split(".")[0].toLong()

                /*
                 * TODO: check that files contain a valid JSON (?)
                 */

                true
            } catch (e: NumberFormatException) {
                false
            }

        }

        if (fileList != null && fileList.isNotEmpty()) {
            val fileNames = Array(fileList.size) { i ->
                val timestamp = fileList[i].name.split(".")[0].toLong()
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
                sdf.format(Date(timestamp))
            }

            builder.setItems(fileNames) { _, which ->
                val ps = loadPathFromJSON(fileList[which].readText())
                if (ps != null) {
                    addPathLayer(ps)
                }
            }

            val dialog = builder.create()
            dialog.show()
        } else {
            showToast("No previous paths to show")
        }
    }

    companion object {
        private const val UPDATE_INTERVAL_MS: Long = 1000
        private const val FASTEST_UPDATE_INTERVAL_MS: Long = 1000
        private const val GCS_WGS84 = 4326 // Geographic coordinate systems return from a GPS device

    }
}