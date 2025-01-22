package com.gtdvm.echopoint

import android.app.Activity
import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.location.LocationManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*


class ManagerDevices(private val activity: AppCompatActivity) {
    private var bluetoothPermissionCallback: BluetoothPermissionCallback? = null
    private var isBluetoothEnabled = false

    interface BluetoothPermissionCallback {
        fun onBluetoothEnabled()
        fun onBluetoothDisabled()
        fun onLocationEnabled()
        fun onLocationDisabled()
    }


    private val locationActivityResultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result ->
        val resultCode = result.resultCode
        val callback = bluetoothPermissionCallback
        Log.d("LOCATIONACTIVITYRESULTLAUNCHER", "result is :$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            callback?.onLocationEnabled()
        } else {
            callback?.onLocationDisabled()
        }
    }

    private val bluetoothActivityResultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result->
        val resultCode = result.resultCode
        val callback = bluetoothPermissionCallback
        //isBluetoothEnabled = result == Activity.RESULT_OK
        if (resultCode == Activity.RESULT_OK) {
            isBluetoothEnabled =true
            callback?.onBluetoothEnabled()
        } else {
            isBluetoothEnabled =false
            callback?.onBluetoothDisabled()
        }
    }

    fun isLocationActive (): Boolean {
        val locationManager = activity.getSystemService (Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d("LOCATION-MANAGER", "locationManager is : $isLocationEnabled")
        return isLocationEnabled
    }

    fun requestActivateLocation (callback: BluetoothPermissionCallback) {
        if (isLocationActive()) {
            callback.onLocationEnabled()
        } else {
            val locationRequest = LocationRequest.Builder(10000L)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMinUpdateIntervalMillis(500L)
                .build()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(activity)
            val task = settingsClient.checkLocationSettings(builder.build())
            task.addOnSuccessListener { //response ->
                callback.onLocationEnabled()
            }
            task.addOnFailureListener{exception ->
                if (exception is ResolvableApiException){
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        locationActivityResultLauncher.launch(intentSenderRequest)
                        bluetoothPermissionCallback = callback
                    } catch (sendEx: IntentSender.SendIntentException){
                        sendEx.printStackTrace()
                    }
                }
            }

            /*val enableLocationIntent = Intent (Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            bluetoothPermissionCallback =callback
            locationActivityResultLauncher.launch (enableLocationIntent)*/
        }
    }

    fun requestEnableBluetooth (callback: BluetoothPermissionCallback) {
        if (isBluetoothEnabled()) {
            callback.onBluetoothEnabled()
        } else {
            val enableBtIntent = Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothPermissionCallback = callback
            bluetoothActivityResultLauncher.launch (enableBtIntent)
        }
    }

    fun isBluetoothEnabled (): Boolean {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter?.isEnabled == true
    }



}
