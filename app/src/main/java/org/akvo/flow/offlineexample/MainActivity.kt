package org.akvo.flow.offlineexample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.makeText
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.offline.OfflineRegion.OfflineRegionObserver

import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "ADD_MAPBOX_TOKEN")
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val pixelRatio = resources.displayMetrics.density
        val minZoom = 12.534885392938495 - 2
        val maxZoom = 12.534885392938495 + 2
        val latLng = LatLng(41.61414036629601, 2.6635049693965414)
        val bounds = LatLngBounds.Builder()
            .include(computeOffset(latLng, 500.0, 45.0)) // Northeast
            .include(computeOffset(latLng, 500.0, 225.0)) // Southwest
            .build()
        val definition = OfflineTilePyramidRegionDefinition(
            Style.MAPBOX_STREETS, bounds, minZoom, maxZoom, pixelRatio
        )
        val regionName = "${latLng.latitude}, ${latLng.longitude}"
        val metadata = getRegionMetadata(regionName)
        OfflineManager.getInstance(this).createOfflineRegion(definition, metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {

                    Log.d(TAG, "Offline region created: $regionName")

                    offlineRegion.setObserver(object : OfflineRegionObserver {
                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            Log.e(TAG, "too many tiles, limit: $limit")
                        }

                        override fun onStatusChanged(status: OfflineRegionStatus?) {
                            Log.d(TAG, "Status changed: ${status!!.downloadState}")
                            if (status.downloadState == 0) {
                                makeText(
                                    applicationContext,
                                    "Region downloaded",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onError(error: OfflineRegionError?) {
                            Log.d(TAG, "onError: ${error!!.message}")
                        }

                    })
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "Error: $error")
                }
            })

        fab.setOnClickListener {
            startActivity(Intent(this, SatelliteLandSelectActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun computeOffset(from: LatLng, distance: Double, heading: Double): LatLng {
        var distance = distance
        var heading = heading
        distance /= EARTH_RADIUS
        heading = Math.toRadians(heading)
        // http://williams.best.vwh.net/avform.htm#LL
        val fromLat = Math.toRadians(from.latitude)
        val fromLng = Math.toRadians(from.longitude)
        val cosDistance = cos(distance)
        val sinDistance = sin(distance)
        val sinFromLat = sin(fromLat)
        val cosFromLat = cos(fromLat)
        val sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * cos(heading)
        val dLng = atan2(
            sinDistance * cosFromLat * sin(heading),
            cosDistance - sinFromLat * sinLat
        )
        return LatLng(Math.toDegrees(asin(sinLat)), Math.toDegrees(fromLng + dLng))
    }

    fun getRegionMetadata(regionName: String): ByteArray {
        val jsonObject = JSONObject()
        jsonObject.put(JSON_FIELD_REGION_NAME, regionName)
        val json = jsonObject.toString()
        return json.toByteArray(charset(JSON_CHARSET))
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val EARTH_RADIUS = 6371009
        private const val JSON_CHARSET = "UTF-8"
        private const val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    }
}
