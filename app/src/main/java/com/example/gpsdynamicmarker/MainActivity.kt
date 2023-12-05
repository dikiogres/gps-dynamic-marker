package com.example.gpsdynamicmarker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Calendar

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mylocationManager: LocationManager? = null
    private var mylocationListener: LocationListener? = null
    private var mMap: GoogleMap? = null
    private var txtLat: TextView? = null
    private var txtLong: TextView? = null
    private var txtTime: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        val locationPermissionRequest =
            registerForActivityResult<Array<String>, Map<String, Boolean>>(
                ActivityResultContracts.RequestMultiplePermissions(),
                ActivityResultCallback<Map<String, Boolean>> { result: Map<String, Boolean> ->
                    val fineLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false
                    )
                    val coarseLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION, false
                    )
                    if (fineLocationGranted) {
                        // Precise location access granted.
                    } else if (coarseLocationGranted) {
                        // Only approximate location access granted.
                    } else {
                        // No location access granted.
                    }
                }
            )
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        mylocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mylocationListener = LokasiListener()
        txtLat = findViewById(R.id.tvLat)
        txtLong = findViewById(R.id.tvLong)
        txtTime = findViewById(R.id.tvUpdate)
        val start = findViewById<Button>(R.id.btnStart)
        start.setOnClickListener(op)
        val stop = findViewById<Button>(R.id.btnStop)
        stop.setOnClickListener(op)
    }

    private inner class LokasiListener : LocationListener {
        private var updatesAllowed = true
        override fun onLocationChanged(location: Location) {
            updateCameraAndTextViews(location)

            // Place a new marker in the new location only if updates are allowed
            if (updatesAllowed) {
                placeMarker(location)
                updatesAllowed = false // Prevent further updates
            }
        }

        fun areUpdatesAllowed(): Boolean {
            return updatesAllowed
        }

        fun allowUpdates() {
            updatesAllowed = true
        }

        fun preventFurtherUpdates() {
            updatesAllowed = false
        }

        private fun updateCameraAndTextViews(location: Location) {
            txtLat!!.text = "Lat: " + location.latitude.toString()
            txtLong!!.text = "Long: " + location.longitude.toString()
            val currentTime = Calendar.getInstance().time
            txtTime!!.text = "Last Update: $currentTime"
            val latitude = location.latitude
            val longitude = location.longitude
            val zoom = 20f

            // Update the camera
            val newLocation = LatLng(latitude, longitude)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, zoom))
        }

        private fun placeMarker(location: Location) {
            val latitude = location.latitude
            val longitude = location.longitude

            // Place a new marker in the new location
            val newLocation = LatLng(latitude, longitude)
            mMap!!.addMarker(MarkerOptions().position(newLocation).title("New Marker"))
        }
    }

    var op = View.OnClickListener { view ->
        if (view.id == R.id.btnStart) {
            updateGPS()
        } else if (view.id == R.id.btnStop) {
            stopGPS()
        }
    }

    private fun stopGPS() {
        mylocationListener?.let { mylocationManager?.removeUpdates(it) } // Use safe call operator (?)

        // Allow updates when the "Start Update" button is pressed again
        if (mylocationListener is MainActivity.LokasiListener) {
            val lokasiListener = mylocationListener as MainActivity.LokasiListener
            lokasiListener.allowUpdates()
        }

        // Get the last known location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val lastLocation =
                mylocationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                // Place a new marker in the last known location
                val latitude = lastLocation.latitude
                val longitude = lastLocation.longitude
                val zoom = 20f
                val lastKnownLocation = LatLng(latitude, longitude)
                mMap!!.addMarker(
                    MarkerOptions().position(lastKnownLocation).title("Last Known Location")
                )
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLocation, zoom))
            } else {
                Toast.makeText(this, "Last known location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGPS() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mylocationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 1000, 0f,
            mylocationListener!!
        )
    }

    private fun gotoPeta(lat: Double, lng: Double, z: Float) {
        val LokasiBaru = LatLng(lat, lng)
        mMap!!.addMarker(MarkerOptions().position(LokasiBaru).title("Marker in $lat: $lng"))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LokasiBaru, z))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in ITS
        val ITS = LatLng(-7.2819705, 112.795323)
        mMap!!.addMarker(MarkerOptions().position(ITS).title("Marker in ITS"))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(ITS, 15f))
    }
}