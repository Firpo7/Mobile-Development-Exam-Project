package com.example.md_project01

import android.graphics.Color
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
import com.esri.arcgisruntime.mapping.view.MapView

class MapViewWrapper(private val mapView: MapView) {

    private var graphicsOverlay: GraphicsOverlay
    private var locationDisplay: LocationDisplay

    init {
        locationDisplay = mapView.locationDisplay
        graphicsOverlay = GraphicsOverlay()
    }

    fun initializeMap(lat: Double, lon: Double) {
        mapView.map = ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, lat, lon, 18)
        mapView.graphicsOverlays.add(graphicsOverlay)
        mapView.wrapAroundMode = WrapAroundMode.DISABLED

        locationDisplay = mapView.locationDisplay
        locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
    }

    fun addPathLayer(ps: PathTraceService) {
        val points = PointCollection(spatialReference)
        var meanLongitudes = 0.0
        var meanLatitudes = 0.0
        val numCoordinates = ps.latitudes.size

        for (i in 0 until numCoordinates) {
            points.add(ps.longitudes[i], ps.latitudes[i])
            meanLongitudes += ps.longitudes[i]
            meanLatitudes += ps.latitudes[i]
        }

        meanLongitudes /= numCoordinates
        meanLatitudes /= numCoordinates

        val pathLine = Polyline(points)
        val startPoint = Point(ps.longitudes[0], ps.latitudes[0], spatialReference)
        val endPoint = Point(ps.longitudes[numCoordinates - 1], ps.latitudes[numCoordinates - 1], spatialReference)

        removeAllGraphics()

        addAllGraphics(
            listOf(
                Graphic(pathLine, lineSymbol),
                Graphic(startPoint, greenCircleSymbol),
                Graphic(endPoint, redCircleSymbol)
            )
        )

        val centerPoint = Point(meanLongitudes, meanLatitudes, spatialReference)
        mapView.setViewpointCenterAsync(centerPoint, 2500.0)
        mapView.resume()
    }

    private fun addAllGraphics(graphicList: List<Graphic>) {
        graphicsOverlay.graphics.addAll(graphicList)
    }

    private fun removeAllGraphics() {
        graphicsOverlay.graphics.clear()
    }

    fun dispose() {
        mapView.dispose()
    }

    fun pause() {
        mapView.pause()
    }

    fun resume() {
        mapView.resume()
    }

    fun startLocationDisplay() {
        if(!locationDisplay.isStarted)
            locationDisplay.startAsync()
    }

    fun stopLocationDisplay() {
        if(locationDisplay.isStarted)
            locationDisplay.stop()
    }

    companion object {
        private const val GCS_WGS84 = 4326 // Geographic coordinate systems returned from a GPS device
        private val spatialReference = SpatialReference.create(GCS_WGS84)

        val lineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.WHITE, 5.0f)
        val greenCircleSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 10f)
        val redCircleSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10f)
    }

}