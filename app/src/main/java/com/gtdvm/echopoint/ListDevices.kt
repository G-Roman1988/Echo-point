package com.gtdvm.echopoint

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider

class ListDevices : AppCompatActivity() {
    private lateinit var bluetoothServices: BluetoothServices
    private lateinit var recyclerView: RecyclerView
    private lateinit var bleDevicesAdapter: BleDevicesAdapter
    private lateinit var iBeaconsView: IBeaconsView

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_devices)
bluetoothServices = BluetoothServices(this)
        recyclerView = findViewById(R.id.resultScannerDevices)
        recyclerView.layoutManager = LinearLayoutManager (this)
        bleDevicesAdapter = BleDevicesAdapter (this) { device ->
onDeviceClick(device)
        }
        recyclerView.adapter = bleDevicesAdapter
        iBeaconsView = ViewModelProvider(this)[IBeaconsView::class.java]
        iBeaconsView.beacons.observe(this) {deviceList ->
bleDevicesAdapter.updateDevices(deviceList)
        }
bluetoothServices.startBluetoothScan()

val stopScaning: Button = findViewById(R.id.stopScaning)
        stopScaning.setOnClickListener {
bluetoothServices.stopScan()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            bluetoothServices.stopScan()
            finish()
        }
    })
    }

private fun onDeviceClick(device: IBeacon) {
    bluetoothServices.stopScan()
val intentConnecting = Intent(applicationContext, CommunicationWithTheDevice::class.java)
    val selectedDevice = device.macAddress
    intentConnecting.putExtra("connectingTo", selectedDevice)
    startActivity(intentConnecting)
    finishAffinity()

}
}

