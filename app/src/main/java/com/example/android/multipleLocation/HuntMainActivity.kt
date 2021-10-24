/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.treasureHunt

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.IntentSender
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProviders
//import com.example.android.treasureHunt.databinding.ActivityHuntMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.location.*


import android.widget.EditText

import android.view.ViewGroup

import android.view.LayoutInflater
import android.view.View
import androidx.databinding.Bindable
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CircleOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import android.R.attr.name
import android.R.attr.name
import android.R.attr.name
import com.example.android.multipleLocation.ModelPreferencesManager


/**
 * The Treasure Hunt app is a single-player game based on geofences.
 *
 * This app demonstrates how to create and remove geofences using the GeofencingApi. Uses an
 * BroadcastReceiver to monitor geofence transitions and creates notification and finishes the game
 * when the user enters the final geofence (destination).
 *
 * This app requires a device's Location settings to be turned on. It also requires
 * the ACCESS_FINE_LOCATION permission and user consent. For geofences to work
 * in Android Q, app also needs the ACCESS_BACKGROUND_LOCATION permission and user consent.
 */

class HuntMainActivity : AppCompatActivity() , OnMapReadyCallback {

   // var listItems = arrayOfNulls<String>(0)
   private var instance: HuntMainActivity? = null
    var  mapFragment : SupportMapFragment? = null
    var listItems: ArrayList<String> = ArrayList()
    lateinit var   listView : ListView
    lateinit  var arrayAdapter: ArrayAdapter<*>
    // private lateinit var binding: ActivityHuntMainBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var viewModel: GeofenceViewModel
    lateinit var locationManager: LocationManager
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q



    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        print("david")
       if(!GeofencingConstants.LANDMARK_DATA.isEmpty() )   {
           val israel = LatLng(GeofencingConstants.LANDMARK_DATA[0].latLong.latitude, GeofencingConstants.LANDMARK_DATA[0].latLong.longitude)
           val zoomLevel = 14.0f
           googleMap.addMarker(
               MarkerOptions()
                   .position(israel)
                   .title("Marker in Israel")
           )
           // [START_EXCLUDE silent]
           googleMap.moveCamera(CameraUpdateFactory.newLatLng(israel))
           googleMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
           val circleOptions = CircleOptions()
           circleOptions.center(israel)
           circleOptions.radius(300.0)
           circleOptions.fillColor(Color.BLUE)
           circleOptions.strokeWidth(6f)
           googleMap.addCircle(circleOptions
           )

       }
        // [END_EXCLUDE]
    }

    @SuppressLint("MissingPermission")
    public fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    Log.d("TAG", "zaviiiiiiid" +  location.toString())
                    GeofencingConstants.LANDMARK_DATA.add(LandmarkDataObject(
                        "Home",
                        R.string.your_location_hint,
                        R.string.your_location,
                        LatLng(location!!.latitude.toDouble(), location!!.longitude.toDouble())))
                    ModelPreferencesManager.put(GeofencingConstants.LANDMARK_DATA, "KEY_LOCATION")

                    mapFragment?.getMapAsync(this)

                    listItems.add(LatLng(location.latitude, location.longitude).toString())



                    arrayAdapter.notifyDataSetChanged()

                    if (location == null) {
                        requestNewLocationData()
                    } else {
                      //  findViewById<TextView>(R.id.latTextView).text = location.latitude.toString()
                      //  findViewById<TextView>(R.id.lonTextView).text = location.longitude.toString()
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            var mLastLocation: Location = locationResult.lastLocation
            Log.d("TAG", "daviiiiiiid" +  mLastLocation.toString())
            GeofencingConstants.LANDMARK_DATA.add(LandmarkDataObject(
                "Home",
                R.string.your_location_hint,
                R.string.your_location,
                LatLng(mLastLocation.latitude, mLastLocation.longitude)))



            listItems.add(LatLng(mLastLocation.latitude, mLastLocation.longitude).toString())



            arrayAdapter.notifyDataSetChanged()






            //findViewById<TextView>(R.id.latTextView).text = mLastLocation.latitude.toString()
            //findViewById<TextView>(R.id.lonTextView).text = mLastLocation.longitude.toString()
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }


    public fun getInstance(): HuntMainActivity? {
        return instance
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // binding = DataBindingUtil.setContentView(this, R.layout.activity_hunt_main)
        instance = this
        ModelPreferencesManager.with(this.application)
        setContentView(R.layout.activity_hunt_main)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)




        viewModel = ViewModelProviders.of(this, SavedStateViewModelFactory(this.application,
            this)).get(GeofenceViewModel::class.java)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        geofencingClient = LocationServices.getGeofencingClient(this)

        // Create channel for notifications
        createChannel(this )
        GeofencingConstants.LANDMARK_DATA = ModelPreferencesManager.get< MutableList<LandmarkDataObject>>("KEY_LOCATION") !!

        val users = GeofencingConstants.LANDMARK_DATA


        for (i in 0 until users.size) {
            val recipe = users[i]
            listItems.add(recipe.latLong.toString())

        }
        listView = findViewById<ListView>(R.id.mobile_list)

        arrayAdapter = ArrayAdapter(this,
            android.R.layout.simple_list_item_1, listItems)

        listView.adapter = arrayAdapter
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        getLastLocation()
        mapFragment?.getMapAsync(this)

    }

    override fun onStart() {
        super.onStart()
        checkPermissionsAndStartGeofencing()
    }

    /*
 *  When we get the result from asking the user to turn on device location, we call
 *  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
 *  we don't resolve the check to keep the user from seeing an endless loop.
 */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    /*
     *  When the user clicks on the notification, this method will be called, letting us know that
     *  the geofence has been triggered, and it's time to move to the next one in the treasure
     *  hunt.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val extras = intent?.extras
        if(extras != null){
            if(extras.containsKey(GeofencingConstants.EXTRA_GEOFENCE_INDEX)){
                viewModel.updateHint(extras.getInt(GeofencingConstants.EXTRA_GEOFENCE_INDEX))
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            // Permission denied.
            Snackbar.make(
                findViewById(android.R.id.content),
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /**
     * This will also destroy any saved state in the associated ViewModel, so we remove the
     * geofences here.
     */
    override fun onDestroy() {
        super.onDestroy()
        removeGeofences()
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissionsAndStartGeofencing() {
        if (viewModel.geofenceIsActive()) return
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@HuntMainActivity,
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofenceForClue()
            }
        }
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            this@HuntMainActivity,
            permissionsArray,
            resultCode
        )
    }

    public fun addGeofenceForClueNextIndex() {
        if (viewModel.geofenceIsActive()) return
        val currentGeofenceIndex = viewModel.nextGeofenceIndex()
        if(currentGeofenceIndex >= GeofencingConstants.NUM_LANDMARKS) {
            removeGeofences()
            viewModel.geofenceActivated()
            return
        }
        val currentGeofenceData = GeofencingConstants.LANDMARK_DATA[currentGeofenceIndex]

        // Build the Geofence Object
        val geofence = Geofence.Builder()
            // Set the request ID, string to identify the geofence.
            .setRequestId(currentGeofenceData.id)
            // Set the circular region of this geofence.
            .setCircularRegion(currentGeofenceData.latLong.latitude,
                currentGeofenceData.latLong.longitude,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            // Set the expiration duration of the geofence. This geofence gets
            // automatically removed after this period of time.
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

            // Add the geofences to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        // First, remove any existing geofences that use our pending intent
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            // Regardless of success/failure of the removal, add the new geofence
            addOnCompleteListener {
                // Add the new geofence request with the new geofence
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@addOnCompleteListener
                }
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        // Geofences added.
                        Toast.makeText(this@HuntMainActivity, R.string.geofences_added,
                            Toast.LENGTH_SHORT)
                            .show()
                        Log.e("Add Geofence", geofence.requestId)
                        // Tell the viewmodel that we've reached the end of the game and
                        // activated the last "geofence" --- by removing the Geofence.
                        viewModel.geofenceActivated()
                    }
                    addOnFailureListener {
                        // Failed to add geofences.
                        Toast.makeText(this@HuntMainActivity, R.string.geofences_not_added,
                            Toast.LENGTH_SHORT).show()
                        if ((it.message != null)) {
                            Log.w(TAG, it.message!!)
                        }
                    }
                }
            }
        }
    }

    /*
     * Adds a Geofence for the current clue if needed, and removes any existing Geofence. This
     * method should be called after the user has granted the location permission.  If there are
     * no more geofences, we remove the geofence and let the viewmodel know that the ending hint
     * is now "active."
     */
    public fun addGeofenceForClue() {
        if (viewModel.geofenceIsActive()) return
        val currentGeofenceIndex = viewModel.nextGeofenceIndex()
        if(currentGeofenceIndex >= GeofencingConstants.NUM_LANDMARKS) {
            removeGeofences()
            viewModel.geofenceActivated()
            return
        }
        val currentGeofenceData = GeofencingConstants.LANDMARK_DATA[currentGeofenceIndex]

        // Build the Geofence Object
        val geofence = Geofence.Builder()
            // Set the request ID, string to identify the geofence.
            .setRequestId(currentGeofenceData.id)
            // Set the circular region of this geofence.
            .setCircularRegion(currentGeofenceData.latLong.latitude,
                currentGeofenceData.latLong.longitude,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            // Set the expiration duration of the geofence. This geofence gets
            // automatically removed after this period of time.
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

            // Add the geofences to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        // First, remove any existing geofences that use our pending intent
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            // Regardless of success/failure of the removal, add the new geofence
            addOnCompleteListener {
                // Add the new geofence request with the new geofence
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@addOnCompleteListener
                }
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        // Geofences added.
                        Toast.makeText(this@HuntMainActivity, R.string.geofences_added,
                            Toast.LENGTH_SHORT)
                            .show()
                        Log.e("Add Geofence", geofence.requestId)
                        // Tell the viewmodel that we've reached the end of the game and
                        // activated the last "geofence" --- by removing the Geofence.
                        viewModel.geofenceActivated()
                    }
                    addOnFailureListener {
                        // Failed to add geofences.
                        Toast.makeText(this@HuntMainActivity, R.string.geofences_not_added,
                            Toast.LENGTH_SHORT).show()
                        if ((it.message != null)) {
                            Log.w(TAG, it.message!!)
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    private fun removeGeofences() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                Log.d(TAG, getString(R.string.geofences_removed))
                Toast.makeText(applicationContext, R.string.geofences_removed, Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.d(TAG, getString(R.string.geofences_not_removed))
            }
        }
    }
    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "HuntMainActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1