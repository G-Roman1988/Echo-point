package com.gtdvm.echopoint.bluetoothService

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.UUID
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
//import org.altbeacon.beacon.permissions.BeaconScanPermissionsActivity
import com.gtdvm.echopoint.NotificationViewModel
import com.gtdvm.echopoint.R


class BluetoothServices (private val activity: AppCompatActivity) {
    private val notificationViewNodel: NotificationViewModel by lazy { ViewModelProvider(activity)[NotificationViewModel::class.java] }
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var personalBleService: BluetoothGattService? = null
    private var notificationCaracteristic: BluetoothGattCharacteristic? = null
    private var writeCaracteristic: BluetoothGattCharacteristic? = null
    private var descriptorBLE2902: BluetoothGattDescriptor? = null
    private val servicesUUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val writeCharacteristicUUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val notificationUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val descriptorBLE2902UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("InlinedApi")
    fun connectToDevice (deviceMacAddres: String) {
        Log.d(TAG, "connection to device with mac address was called: $deviceMacAddres")
        val bleScaner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScaner == null && bluetoothAdapter == null){
            Log.d(TAG, "Not supported ble adapter")
            return
        }
        val scanCallback = object : ScanCallback(){
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                Log.d(TAG, "I'm looking for the device")
                if (result.device.address == deviceMacAddres){
                    Log.d(TAG, "device found and trying to connect to the device")
                    if ((activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) || (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)){
                        //activity.requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), BLE_CONNECT_REQUEST_CODE)
                        bleScaner?.stopScan(this)
                        val device = result.device
                        try {
                            bluetoothGatt = device.connectGatt(activity, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
                        } catch (e: SecurityException){
                            Log.d(TAG, "cannot connect secure exception :$e")
                            notificationViewNodel.showNotificationData("sa produs o eroare la connectare : $e")
                        }
                    }
                }
            }
        }
        if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)){
            bleScaner?.startScan(scanCallback)
            Log.d(TAG, "scan started")
        } else {
            notificationViewNodel.showNotificationData("este android 11")
        }
    }

    //callback from the connection (callback)
    private val gattCallback = object : BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(TAG, "onConnectionStateChange: status=$status newState=$newState")
            if (newState == BluetoothGatt.STATE_CONNECTED){
                Log.d(TAG, "connection successful")
                notificationViewNodel.showNotificationData(activity.getString(R.string.Message_After_Connecting))
                if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)){
                    Log.d(TAG, "I discover the service")
                    gatt?.discoverServices()
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                Log.d(TAG, "connection ended")
                notificationViewNodel.showNotificationData(activity.getString(R.string.Message_after_Deconnection))
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "callback to discovery service")
            if (status == BluetoothGatt.GATT_SUCCESS){
                personalBleService = gatt?.getService(servicesUUID)
                Log.d(TAG, "the service is obtained")
                personalBleService?.let {
                    notificationCaracteristic = personalBleService?.getCharacteristic(notificationUUID)
                    Log.d(TAG, "the characteristic is obtained")
                    if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)){
                        Log.d(TAG, "enable notification on feature")
                        if (notificationCaracteristic != null){
                            gatt?.setCharacteristicNotification(notificationCaracteristic, true)
                            Log.d(TAG, "notification has been successfully enabled in the characteristic")
                            descriptorBLE2902 = notificationCaracteristic?.getDescriptor(descriptorBLE2902UUID)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt?.writeDescriptor(descriptorBLE2902!!, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                descriptorBLE2902?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt?.writeDescriptor(descriptorBLE2902)
                            }
                        } else {
                            Log.d(TAG, "feature notification is not enabled that it is null")
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.d(TAG, "the values in the notification feature have changed in the new API")
            handleCharacteristicChanged(characteristic, value)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "the values in the notification feature have changed in the old API")
            characteristic.value?.let {
                handleCharacteristicChanged(characteristic, it)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, "the feature was successfully written")
            } else{
                Log.d(TAG, "an error occurred while writing the feature is error: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, "BLE2902 descriptor was successfully written")
            } else {
                Log.d(TAG, "BLE2902 descriptor write failed")
            }
        }
    }

    fun disConnect (){
        if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)){
            bluetoothGatt?.let {
                Log.d(TAG, "disconnecting this device")
                it.disconnect()
//                it.close()
//                bluetoothGatt = null
            }
        }
    }

    @Suppress("DEPRECATION")
    fun writeBleCharacteristic(sendCommand: ByteArray){
        writeCaracteristic = personalBleService?.getCharacteristic(writeCharacteristicUUID)
        writeCaracteristic?.let {
            if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bluetoothGatt?.writeCharacteristic(it, sendCommand, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else{
                    it.value = sendCommand
                    it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    bluetoothGatt?.writeCharacteristic(writeCaracteristic)
                }
            }
        }
    }

    private fun handleCharacteristicChanged (characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (characteristic == notificationCaracteristic){
            val receverData = value.decodeToString()
            Log.d(TAG, "is: $receverData")
            notificationViewNodel.showNotificationData(receverData)

        }

    }

    fun isConnected(): Boolean {
        return bluetoothGatt != null
    }

    companion object{
        const val TAG = "Bluetooth_Service"
    }

}