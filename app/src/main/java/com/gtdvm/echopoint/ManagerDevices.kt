package com.gtdvm.echopoint

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.location.LocationManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat



class ManagerDevices(private val activity: AppCompatActivity) {

    private val bluetoothRequestCode = 1001
    private val notificationRequestCode = 2001
    private var bluetoothPermissionCallback: BluetoothPermissionCallback? = null
    private var isBluetoothEnabled = false
    private val blePermissionSmallerAndroid12 = arrayOf (Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    @SuppressLint ("InlinedApi")
    private val blePermissionBiggerAndroid12 = arrayOf (Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)


    interface BluetoothPermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied()
        fun onBluetoothEnabled()
        fun onBluetoothDisabled()
        fun onLocationEnabled()
        fun onLocationDisabled()
    }


    private val locationActivityResultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
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

    fun areBluetoothPermissionsGranted (): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        return result
    }

    fun isLocationActive (): Boolean {
        val locationManager = activity.getSystemService (Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d("LOCATION-MANAGER", "locationManager is : $isLocationEnabled")
        return isLocationEnabled
    }

    fun arePermissionLocationGiven (): Boolean {
        return areBluetoothPermissionsGranted()
    }

    fun requestLocationPermission (callback: BluetoothPermissionCallback) {
        if (arePermissionLocationGiven()) {
            callback.onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), bluetoothRequestCode)
        }
    }

    fun requestActivateLocation (callback: BluetoothPermissionCallback) {
        if (isLocationActive()) {
            callback.onLocationEnabled()
        } else {
            val enableLocationIntent = Intent (Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            bluetoothPermissionCallback =callback
            locationActivityResultLauncher.launch (enableLocationIntent)
        }
    }

    fun onRequestPermissionsResult (requestCode: Int, grantResults: IntArray, callback: BluetoothPermissionCallback) {
        if (requestCode == bluetoothRequestCode) {
            val allPermissionsGranted = grantResults.all {it == PackageManager.PERMISSION_GRANTED}
            if (grantResults.isNotEmpty() && allPermissionsGranted) {
                callback.onPermissionGranted()
            }else {
                callback.onPermissionDenied()
            }
        }
    }

    fun requestBluetoothPermissions (callback: BluetoothPermissionCallback) {
        if (areBluetoothPermissionsGranted()){
            callback.onPermissionGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(activity, blePermissionBiggerAndroid12, bluetoothRequestCode)
            } else{
                ActivityCompat.requestPermissions(activity, blePermissionSmallerAndroid12, bluetoothRequestCode)
            }
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

    fun notificationsAreAccepted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.areNotificationsEnabled()
        }
        else{
return NotificationManagerCompat.from(activity).areNotificationsEnabled()
        }
    }

    fun requestNotificationPermission(){
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequestCode)
}
}
    }



}