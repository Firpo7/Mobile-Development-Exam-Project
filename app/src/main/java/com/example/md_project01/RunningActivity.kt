package com.example.md_project01

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.esri.arcgisruntime.geometry.Point
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
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import kotlinx.android.synthetic.main.content_running.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock


class RunningActivity : BaseActivity() {
    private var dir: String = ""// = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()
//    private var lastLocation: Location? = null

    private lateinit var locationDisplay: LocationDisplay

//    private var locationCallback: LocationCallback? = null
//    private lateinit var locationRequest: LocationRequest
    private lateinit var graphicsOverlay: GraphicsOverlay
    private var lastLineGraphic: Graphic? = null
    private var lastStartPointGraphic: Graphic? = null
    private var lastEndPointGraphic: Graphic? = null
    private var pathTrace: Intent? = null
    private lateinit var locationListener: BroadcastReceiver// = LocationReceiver(this)
    private var distanceMade = 0.0
    private var wasRunning: Boolean = false


    //@Volatile
    //private var pathService: PathService? = null
    private val sharedCounterLock = ReentrantLock()
    private var currentButtonState: ButtonState = ButtonState.START
    //private val locationSettingsRequest: LocationSettingsRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        super.LOG_TAG = "RunningActivity"
        Log.d("[$LOG_TAG]", "onCreate()")

        dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

        val toolbar: Toolbar  = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        getLocationWithCallback { location: Location? ->
            initializeMap(location)
        }

        locationListener = LocationReceiver(this)
/*
        this.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult) // why? this. is. retarded. Android.
                updateDistance(locationResult.lastLocation)
            }
        }
*/
/*
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_MS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_MS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
*/


    }

    override fun onPause() {
        super.onPause()
        Log.d("[$LOG_TAG]", "onPause()")
        unregisterReceiver(locationListener)
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        Log.d("[$LOG_TAG]", "onResume()")
        registerReceiver(locationListener, IntentFilter(PathTraceService.NOTIFY))
        if (wasRunning) {
            wasRunning = false
            locationDisplay.startAsync()
        }
        mapView.resume()
    }

    override fun onStop() {
        super.onStop()
        Log.d("[$LOG_TAG]", "onStop()")
        if (locationDisplay.isStarted) {
            wasRunning = true
            locationDisplay.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[$LOG_TAG]", "onDestroy()")
        mapView.dispose()
        shutdownScheduledTask()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_show_path_histories) {
            showPathHistories()
            return true
        }

        if (id == R.id.action_delete_some_paths) {
            deleteSavedPath()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun shutdownScheduledTask() {
        //fusedLocationClient.removeLocationUpdates(this.locationCallback)
        MyInsertTask(this@RunningActivity, Stats(Date(System.currentTimeMillis()), distanceMade.toLong())).execute()
        if(pathTrace != null) stopService(pathTrace)
    }

    private fun initializeMap(location: Location?) {
        var lat = 0.0
        var lon = 0.0
        if (location != null) {
            lat = location.latitude
            lon = location.longitude
        } else {
            showToast( getResourceString(R.string.error_location_disabled) )
        }
        val map = ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, lat, lon, 18)
        mapView.map = map

        graphicsOverlay = GraphicsOverlay()
        mapView!!.graphicsOverlays.add(graphicsOverlay)

        mapView.wrapAroundMode = WrapAroundMode.DISABLED
        locationDisplay = mapView.locationDisplay
    }

    private fun addPathLayer(ps: PathTraceService) {
        val points = PointCollection(SpatialReference.create(GCS_WGS84))
        var meanLongitudes = 0.0
        var meanLatitudes = 0.0
        val numCoordinates = ps.latitudes.size

        setTextView(
            findViewById(R.id.runningactivity_textview_distance),
            ps.distanceMade.toLong().toString()
        )

        for (i in 0 until numCoordinates) {
            points.add(ps.longitudes[i], ps.latitudes[i])
            meanLongitudes += ps.longitudes[i]
            meanLatitudes += ps.latitudes[i]
        }

        meanLongitudes /= ps.longitudes.size
        meanLatitudes /= ps.latitudes.size

        val pathLine = Polyline(points)
        val lineSymbol = SimpleLineSymbol(
            SimpleLineSymbol.Style.SOLID,
            Color.argb(255, 255, 255, 255), 5.0f
        )
        val startPoint =
            Point(ps.longitudes[0], ps.latitudes[0], SpatialReference.create(GCS_WGS84))
        val endPoint = Point(
            ps.longitudes[numCoordinates - 1],
            ps.latitudes[numCoordinates - 1],
            SpatialReference.create(GCS_WGS84)
        )
        val greenCircleSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.argb(255, 0, 255, 0), 10f)
        val redCircleSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.argb(255, 255, 0, 0), 10f)

        val lineGraphic = Graphic(pathLine, lineSymbol)
        val startPointGraphic = Graphic(startPoint, greenCircleSymbol)
        val endPointGraphic = Graphic(endPoint, redCircleSymbol)

        graphicsOverlay.graphics.removeAll(
            listOf(
                lastLineGraphic,
                lastStartPointGraphic,
                lastEndPointGraphic
            )
        )
        graphicsOverlay.graphics.addAll(
            listOf(
                lineGraphic,
                startPointGraphic,
                endPointGraphic
            )
        )

        lastLineGraphic = lineGraphic
        lastStartPointGraphic = startPointGraphic
        lastEndPointGraphic = endPointGraphic

        val centerPoint = Point(meanLongitudes, meanLatitudes, SpatialReference.create(GCS_WGS84))
        mapView.setViewpointCenterAsync(centerPoint, 2500.0)
        mapView.resume()
    }


    private fun updateDistance(distance: Double){
        distanceMade = distance
        setTextView(
            findViewById(R.id.runningactivity_textview_distance),
            distance.toLong().toString()
        )
    }

/*
    private fun updateDistance(location: Location?) {
        if (location != null) {
            try {
                sharedCounterLock.lock()
                val distanceUpdated = pathService!!.addPoint(location)

                Log.d("RUNNING SCHEDULED TASK", pathService!!.distanceMade.toString())
                if(distanceUpdated != null)
                    setTextView(
                        findViewById(R.id.runningactivity_textview_distance),
                        distanceUpdated.toLong().toString()
                    )
                Log.d("setTextView", "OK")
            } finally {
                sharedCounterLock.unlock()
            }
        } else {
            Log.d("RUNNING SCHEDULED TASK", "null Location!!")
        }
    }
*/
    fun buttonStartStop(@Suppress("UNUSED_PARAMETER") v: View) {
        currentButtonState = when(currentButtonState) {
            ButtonState.START -> {
                val button = findViewById<Button>(R.id.runningactivity_button_start)
                button.text = resources.getText(R.string.runningactivity_button_stop)
                button.setBackgroundResource(R.color.color_dark_orange)
                startRunning()
                ButtonState.STOP
            }
            ButtonState.STOP -> {
                stopRunning()
                val button = findViewById<Button>(R.id.runningactivity_button_start)
                button.text = resources.getText(R.string.runningactivity_button_start)
                button.setBackgroundResource(R.color.color_teal)
                ButtonState.START
            }
        }
    }

    private fun startRunning() {
        if(::locationDisplay.isInitialized) {
            if (!locationDisplay.isStarted) {
                try {
                    sharedCounterLock.lock()
                    pathTrace = Intent(this, PathTraceService::class.java)
                    pathTrace!!.putExtra(PathTraceService.EXTRA_DIR, dir)
                    startService(pathTrace)
                } finally {
                    sharedCounterLock.unlock()
                }

                locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
                locationDisplay.startAsync()
                /*fusedLocationClient.requestLocationUpdates(
                    this.locationRequest,
                    this.locationCallback,
                    Looper.myLooper()
                )
                */
            }
        } else {
            //TODO: do something better
            showToast( getResourceString(R.string.error_location_disabled) )
        }
    }

    private fun stopRunning() {
        if(::locationDisplay.isInitialized)
            if (locationDisplay.isStarted) {
                //saveStats()
                shutdownScheduledTask()
                locationDisplay.stop()
                //savePathMade()
            }
    }


    private fun loadPathFromJSON(json: String): PathTraceService? {
        val ps = PathTraceService()

        val jsonObj = JSONObject(json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1))

        try {
            ps.distanceMade = jsonObj.getDouble("distanceMade")
        } catch (e: Exception) {
            return null
        }

        val latJson = jsonObj.getJSONArray("latitudes")
        val lonJson = jsonObj.getJSONArray("longitudes")

        for (i in 0 until latJson.length()) {
            try {
                ps.latitudes.add(latJson[i].toString().toDouble())
                ps.longitudes.add(lonJson[i].toString().toDouble())
            } catch (e: Exception) {
                return null
            }
        }

        return ps
    }

    private fun deleteSavedPath() {
        val filesToDelete = mutableSetOf<File>()
        val builder = AlertDialog.Builder(this@RunningActivity)
        builder.setTitle( getResourceString(R.string.runningactivity_title_dialog_deletepath) )

        val fileList = PathTraceService.getPastPathFilesList(dir)

        if (fileList != null && fileList.isNotEmpty()) {
            val fileNames = Array(fileList.size) { i ->
                val timestamp = fileList[i].name.split(".")[0].toLong()
                sdf.format(Date(timestamp))
            }

            builder.setMultiChoiceItems(fileNames, BooleanArray(fileList.size) { false } ) { _, which, isChecked ->
                if (isChecked) {
                    filesToDelete.add(fileList[which])
                } else {
                    filesToDelete.remove(fileList[which])
                }
            }

            builder.setPositiveButton( getResourceString(R.string.delete) ) { _, _ ->
                // user clicked OK
                for ( f in filesToDelete) {
                    f.delete()
                }
            }

            builder.setNegativeButton( getResourceString(R.string.cancel), null)

            val dialog = builder.create()
            dialog.show()
        }

    }

    private fun showPathHistories() {
        val builder = AlertDialog.Builder(this@RunningActivity)
        builder.setTitle( getResourceString(R.string.runningactivity_title_dialog_choosepath) )

        val fileList = PathTraceService.getPastPathFilesList(dir)

        if (fileList != null && fileList.isNotEmpty()) {
            val fileNames = Array(fileList.size) { i ->
                val timestamp = fileList[i].name.split(".")[0].toLong()
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
            showToast(getResourceString(R.string.runningactivity_toast_no_past_path_found) )
        }
    }

    class LocationReceiver(private val ra: RunningActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val dist = intent.getDoubleExtra(PathTraceService.EXTRA_DISTANCE, 0.0)
            if(dist>0) ra.updateDistance(dist)
        }
    }



    companion object {
//        private const val UPDATE_INTERVAL_MS: Long = 1000
//        private const val FASTEST_UPDATE_INTERVAL_MS: Long = 1000
        private const val GCS_WGS84 = 4326 // Geographic coordinate systems returned from a GPS device
        private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

        private enum class ButtonState {START, STOP}
    }
}