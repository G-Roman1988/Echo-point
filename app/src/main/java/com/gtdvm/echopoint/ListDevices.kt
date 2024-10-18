package com.gtdvm.echopoint

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
//import com.gtdvm.echopoint.BeaconScanPermissionsActivity
//import com.gtdvm.echopoint.bluetoothService.BluetoothServices
import com.gtdvm.echopoint.bluetoothService.IBeaconDeviceScanningService

class ListDevices : AppCompatActivity() {
    //private lateinit var bluetoothServices: BluetoothServices
    private lateinit var iBeaconDeviceScanningService: IBeaconDeviceScanningService
    private lateinit var recyclerView: RecyclerView
    private lateinit var bleDevicesAdapter: BleDevicesAdapter
    private lateinit var iBeaconsView: IBeaconsView

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_devices)
//bluetoothServices = BluetoothServices(this)
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

        iBeaconDeviceScanningService = application as IBeaconDeviceScanningService

        //I set up a Live Data observer for the signaling data
val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(iBeaconDeviceScanningService.myIBeaconsRegion)
regionViewModel.regionState.observe(this, monitoringObserver)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)

        //check if all permissions are accepted
        if (!BeaconScanPermissionsActivity.allPermissionsGranted(this, true)){
            // permissions are not supported and prompt the user
            val intent = Intent(this, BeaconScanPermissionsActivity::class.java)
            intent.putExtra("backgroundAccessRequested", true)
            startActivity(intent)
        } else {
            //permissions are accepted and start foreground service and scan
            if (BeaconManager.getInstanceForApplication(this).monitoredRegions.size == 0){
                (application as IBeaconDeviceScanningService).setupBeaconScanning()
            }
        }

val stopScaning: Button = findViewById(R.id.stopScaning)
        stopScaning.setOnClickListener {

            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            finish()
        }
    })
    }

    val monitoringObserver = Observer<Int> {state ->
if (state == MonitorNotifier.OUTSIDE){
Log.d("RESULT_SCAN", "nu este nimic în jur")
} else {
    Log.d("RESULT_SCAN", "ceva este înapropriere")
}
    }

    val rangingObserver = Observer<Collection<Beacon>> {beacons ->
if (BeaconManager.getInstanceForApplication(this).rangedRegions.size > 0){
beacons.sortedBy { it.distance }
    .map {
Log.d("RESULT_SCAN", "Nume ${it.bluetoothName} mac adresa ${it.bluetoothAddress}")
    }
}
    }

private fun onDeviceClick(device: IBeacon) {

val intentConnecting = Intent(applicationContext, CommunicationWithTheDevice::class.java)
    val selectedDevice = device.macAddress
    intentConnecting.putExtra("connectingTo", selectedDevice)
    startActivity(intentConnecting)
    finishAffinity()

}
}

