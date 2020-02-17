package com.example.md_project01

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.android.synthetic.main.content_running.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class RunningActivity : BaseActivity() {
    private lateinit var locationListener: BroadcastReceiver
    private var dir: String = ""
    private var pathTrace: Intent? = null
    private var distanceMade = 0.0
    private var wasRunning: Boolean = false
    private var currentButtonState: ButtonState = ButtonState.START
    private lateinit var mv: MapViewWrapper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)
        super.LOG_TAG = "RunningActivity"
        Log.d("[$LOG_TAG]", "onCreate()")

        dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()

        val toolbar: Toolbar  = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        getLocationWithCallback {
                location: Location? -> initializeMap(location)
        }

        mv = MapViewWrapper(mapView)

        locationListener = LocationReceiver(this)
    }

    override fun onPause() {
        super.onPause()
        Log.d("[$LOG_TAG]", "onPause()")
        unregisterReceiver(locationListener)
        mv.pause()
    }

    override fun onResume() {
        super.onResume()
        Log.d("[$LOG_TAG]", "onResume()")
        registerReceiver(locationListener, IntentFilter(PathTraceService.NOTIFY))
        if (wasRunning) {
            mv.startLocationDisplay()
        }
        mv.resume()
    }

    override fun onStop() {
        super.onStop()
        Log.d("[$LOG_TAG]", "onStop()")
        if (wasRunning) {
            mv.stopLocationDisplay()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[$LOG_TAG]", "onDestroy()")
        mv.dispose()
        if (wasRunning)
            shutdownScheduledTask()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_show_path_histories -> {
                showPathHistories()
                true
            }
            R.id.action_delete_some_paths -> {
                deleteSavedPath()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shutdownScheduledTask() {
        if(pathTrace != null) {
            MyInsertTask(this@RunningActivity, Stats(Date(System.currentTimeMillis()), distanceMade.toLong())).execute()
            stopService(pathTrace)
        }
    }

    private fun initializeMap(location: Location?) {
        if (location != null) {
            mv.initializeMap(location.latitude, location.longitude)
        } else {
            showToastErrorLocationDisabled()
            mv.initializeMap(0.0, 0.0)
        }
    }

    private fun loadPastPath(ps: PathTraceService) {
        setTextView(
            findViewById(R.id.runningactivity_textview_distance),
            ps.distanceMade.toLong().toString()
        )

        mv.addPathLayer(ps)
    }

    private fun updateDistance(distance: Double){
        distanceMade = distance
        setTextView(
            findViewById(R.id.runningactivity_textview_distance),
            distance.toLong().toString()
        )
    }

    fun buttonStartStop(@Suppress("UNUSED_PARAMETER") v: View) {
        currentButtonState = when(currentButtonState) {
            ButtonState.START -> {
                if (isLocationEnabled()) {
                    val button = findViewById<Button>(R.id.runningactivity_button_start)
                    button.text = resources.getText(R.string.runningactivity_button_stop)
                    button.setBackgroundResource(R.color.color_dark_orange)
                    startRunning()
                    ButtonState.STOP
                } else {
                    showToastErrorLocationDisabled()
                    ButtonState.START
                }
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
        if(!wasRunning) {
            pathTrace = Intent(this, PathTraceService::class.java)
            pathTrace!!.putExtra(PathTraceService.EXTRA_DIR, dir)
            startService(pathTrace)

            wasRunning = true

            mv.startLocationDisplay()
            setTextView(
                findViewById(R.id.runningactivity_textview_distance),
                "0"
            )

        }
    }

    private fun stopRunning() {
        if (wasRunning) {
            wasRunning = false
            shutdownScheduledTask()
            mv.stopLocationDisplay()
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

    private fun getFileNamesFromFileList(fileList: List<File>): Array<String> {
        return Array(fileList.size) { i ->
            val timestamp = fileList[i].name.split(".")[0].toLong()
            sdf.format(Date(timestamp))
        }
    }

    private fun deleteSavedPath() {
        val filesToDelete = mutableSetOf<File>()
        val builder = AlertDialog.Builder(this@RunningActivity)
        builder.setTitle( getResourceString(R.string.runningactivity_title_dialog_deletepath) )

        val fileList = PathTraceService.getPastPathFilesList(dir)
        if (fileList != null && fileList.isNotEmpty()) {
            val fileNames = getFileNamesFromFileList(fileList)

            builder.setMultiChoiceItems(fileNames, BooleanArray(fileList.size) { false } ) { _, which, isChecked ->
                if (isChecked) {
                    filesToDelete.add(fileList[which])
                } else {
                    filesToDelete.remove(fileList[which])
                }
            }

            builder.setPositiveButton( getResourceString(R.string.delete) ) { _, _ ->
                for ( f in filesToDelete) f.delete()
            }

            builder.setNegativeButton( getResourceString(R.string.cancel), null)

            val dialog = builder.create()
            dialog.show()
        } else {
            showToast(getResourceString(R.string.runningactivity_toast_no_past_path2delete_found))
        }
    }

    private fun showPathHistories() {
        val builder = AlertDialog.Builder(this@RunningActivity)
        builder.setTitle(getResourceString(R.string.runningactivity_title_dialog_choosepath))

        val fileList = PathTraceService.getPastPathFilesList(dir)
        if (fileList != null && fileList.isNotEmpty()) {
            val fileNames = getFileNamesFromFileList(fileList)

            builder.setItems(fileNames) { _, which ->
                val ps = loadPathFromJSON(fileList[which].readText())
                if (ps != null)
                    loadPastPath(ps)
            }

            val dialog = builder.create()
            dialog.show()
        } else {
            showToast(getResourceString(R.string.runningactivity_toast_no_past_path_found))
        }
    }

    class LocationReceiver(private val ra: RunningActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val dist = intent.getDoubleExtra(PathTraceService.EXTRA_DISTANCE, 0.0)
            if(dist>0)
                ra.updateDistance(dist)
        }
    }


    companion object {
        private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        private enum class ButtonState {START, STOP}
    }
}