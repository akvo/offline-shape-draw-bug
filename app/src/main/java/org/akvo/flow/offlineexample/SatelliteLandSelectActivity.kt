package org.akvo.flow.offlineexample

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.util.*

/**
 * Use map click location to select an area of land on satellite photos and draw the selected area
 * with a CircleLayer, LineLayer, and FillLayer.
 */
class SatelliteLandSelectActivity : AppCompatActivity(), OnMapReadyCallback {
    private var fillLayerPointList: MutableList<Point> = ArrayList()
    private var lineLayerPointList: MutableList<Point> = ArrayList()
    private var circleLayerFeatureList: MutableList<Feature> = ArrayList()
    private var listOfList: MutableList<List<Point>> = ArrayList()
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var circleSource: GeoJsonSource? = null
    private var fillSource: GeoJsonSource? = null
    private var lineSource: GeoJsonSource? = null
    private var textSource: GeoJsonSource? = null
    private var firstPointOfPolygon: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, "ADD_MAPBOX_TOKEN")

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_satellite_land_select)

        mapView = findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            val cameraPosition = CameraPosition.Builder()
                .target(LatLng(41.61414036629601, 2.6635049693965414))
                .zoom(12.534885392938495)
                .build()
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            // Add sources to the map
            circleSource = initCircleSource(style)
            fillSource = initFillSource(style)
            lineSource = initLineSource(style)

            // Add layers to the map
            initCircleLayer(style)
            initLineLayer(style)
            initFillLayer(style)
            initCircleLayerSelected(style)

            initFloatingActionButtonClickListeners()
        }
    }

    /**
     * Set the button click listeners
     */
    private fun initFloatingActionButtonClickListeners() {
        val clearBoundariesFab = findViewById<Button>(R.id.clear_button)
        clearBoundariesFab.setOnClickListener { clearEntireMap() }

        val dropPinFab = findViewById<FloatingActionButton>(R.id.drop_pin_button)
        dropPinFab.setOnClickListener {
            // Use the map click location to create a Point object
            val mapTargetPoint = Point
                .fromLngLat(
                    mapboxMap!!.cameraPosition.target.longitude,
                    mapboxMap!!.cameraPosition.target.latitude
                )

            // Make note of the first map click location so that it can be used to create a closed polygon later on
            if (circleLayerFeatureList.size == 0) {
                firstPointOfPolygon = mapTargetPoint
            }

            // Add the click point to the circle layer and update the display of the circle layer data
            circleLayerFeatureList.add(Feature.fromGeometry(mapTargetPoint))
            circleSource?.setGeoJson(FeatureCollection.fromFeatures(circleLayerFeatureList))
            textSource?.setGeoJson(FeatureCollection.fromFeatures(circleLayerFeatureList))

            // Add the click point to the line layer and update the display of the line layer data
            when {
                circleLayerFeatureList.size < 3 -> lineLayerPointList.add(mapTargetPoint)
                circleLayerFeatureList.size == 3 -> {
                    lineLayerPointList.add(mapTargetPoint)
                    lineLayerPointList.add(this.firstPointOfPolygon!!)
                }
                circleLayerFeatureList.size >= 4 -> {
                    lineLayerPointList.removeAt(circleLayerFeatureList.size - 1)
                    lineLayerPointList.add(mapTargetPoint)
                    lineLayerPointList.add(this.firstPointOfPolygon!!)
                }

                // Add the click point to the fill layer and update the display of the fill layer data
            }
            lineSource?.setGeoJson(
                FeatureCollection.fromFeatures(
                    arrayOf(
                        Feature.fromGeometry(
                            LineString.fromLngLats(lineLayerPointList)
                        )
                    )
                )
            )

            // Add the click point to the fill layer and update the display of the fill layer data
            when {
                circleLayerFeatureList.size < 3 -> fillLayerPointList.add(mapTargetPoint)
                circleLayerFeatureList.size == 3 -> {
                    fillLayerPointList.add(mapTargetPoint)
                    firstPointOfPolygon?.let { fillLayerPointList.add(it) }
                }
                circleLayerFeatureList.size >= 4 -> {
                    fillLayerPointList.removeAt(fillLayerPointList.size - 1)
                    fillLayerPointList.add(mapTargetPoint)
                    firstPointOfPolygon?.let { fillLayerPointList.add(it) }
                }
            }
            listOfList = ArrayList()
            listOfList.add(fillLayerPointList)
            val finalFeatureList = ArrayList<Feature>()
            finalFeatureList.add(Feature.fromGeometry(Polygon.fromLngLats(listOfList)))
            val newFeatureCollection = FeatureCollection.fromFeatures(finalFeatureList)
            fillSource?.setGeoJson(newFeatureCollection)
        }
    }

    /**
     * Remove the drawn area from the map by resetting the FeatureCollections used by the layers' sources
     */
    private fun clearEntireMap() {
        fillLayerPointList = ArrayList()
        circleLayerFeatureList = ArrayList()
        lineLayerPointList = ArrayList()
        circleSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
        lineSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
        fillSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
    }

    /**
     * Set up the CircleLayer source for showing map click points
     */
    private fun initCircleSource(loadedMapStyle: Style): GeoJsonSource {
        val circleFeatureCollection = FeatureCollection.fromFeatures(arrayOf())
        val circleGeoJsonSource = GeoJsonSource(
            CIRCLE_SOURCE_ID,
            circleFeatureCollection
        )
        loadedMapStyle.addSource(circleGeoJsonSource)
        return circleGeoJsonSource
    }

    /**
     * Set up the CircleLayer for showing polygon click points
     */
    private fun initCircleLayer(loadedMapStyle: Style) {
        val circleLayer = CircleLayer(
            CIRCLE_LAYER_ID,
            CIRCLE_SOURCE_ID
        )
        circleLayer.setProperties(
            circleRadius(7f),
            circleColor(Color.parseColor("#d004d3")) //purple circle
        )
        loadedMapStyle.addLayer(circleLayer)
    }

    private fun initCircleLayerSelected(loadedMapStyle: Style) {
        val symbolLayer = SymbolLayer(
            CIRCLE_LAYER_ID_SELECTED,
            CIRCLE_SOURCE_ID
        )
        symbolLayer.setProperties(
            textField(Expression.literal("point")),
            textSize(12f),
            textOffset(arrayOf(0f, -2.0f)),
            textColor(Color.parseColor("#FF0000")),
            textAllowOverlap(true),
            textIgnorePlacement(true)
        )
        loadedMapStyle.addLayerAbove(symbolLayer, CIRCLE_LAYER_ID)
    }

    /**
     * Set up the FillLayer source for showing map click points
     */
    private fun initFillSource(loadedMapStyle: Style): GeoJsonSource {
        val fillFeatureCollection = FeatureCollection.fromFeatures(arrayOf())
        val fillGeoJsonSource = GeoJsonSource(FILL_SOURCE_ID, fillFeatureCollection)
        loadedMapStyle.addSource(fillGeoJsonSource)
        return fillGeoJsonSource
    }

    /**
     * Set up the FillLayer for showing the set boundaries' polygons
     */
    private fun initFillLayer(loadedMapStyle: Style) {
        val fillLayer = FillLayer(
            FILL_LAYER_ID,
            FILL_SOURCE_ID
        )
        fillLayer.setProperties(
            fillOpacity(.6f),
            fillColor(Color.parseColor("#00e9ff")) //fill light blue
        )
        loadedMapStyle.addLayerBelow(fillLayer, LINE_LAYER_ID)
    }

    /**
     * Set up the LineLayer source for showing map click points
     */
    private fun initLineSource(loadedMapStyle: Style): GeoJsonSource {
        val lineFeatureCollection = FeatureCollection.fromFeatures(arrayOf())
        val lineGeoJsonSource = GeoJsonSource(LINE_SOURCE_ID, lineFeatureCollection)
        loadedMapStyle.addSource(lineGeoJsonSource)
        return lineGeoJsonSource
    }

    /**
     * Set up the LineLayer for showing the set boundaries' polygons
     */
    private fun initLineLayer(loadedMapStyle: Style) {
        val lineLayer = LineLayer(
            LINE_LAYER_ID,
            LINE_SOURCE_ID
        )
        lineLayer.setProperties(
            lineColor(Color.WHITE), //white line
            lineWidth(5f)
        )
        loadedMapStyle.addLayerBelow(lineLayer, CIRCLE_LAYER_ID)
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    public override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    companion object {

        private const val CIRCLE_SOURCE_ID = "circle-source-id"
        private const val FILL_SOURCE_ID = "fill-source-id"
        private const val LINE_SOURCE_ID = "line-source-id"
        private const val CIRCLE_LAYER_ID = "circle-layer-id"
        private const val CIRCLE_LAYER_ID_SELECTED = "circle-layer-id-selected"
        private const val FILL_LAYER_ID = "fill-layer-polygon-id"
        private const val LINE_LAYER_ID = "line-layer-id"
    }
}