package com.gtdvm.echopoint

//import androidx.lifecycle.ViewModelProvider
                        import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
                        import android.view.View
                        import android.widget.Button
                        import android.widget.TextView
                        import android.widget.Toast
                        import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
                        import com.gtdvm.echopoint.bluetoothService.IBeaconDeviceScanningService
                        import com.gtdvm.echopoint.adapters.BleDevicesAdapter
                        import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier


class ListDevices : AppCompatActivity() {
    private val devicesFound = mutableListOf<IBeacon>()
    private lateinit var iBeaconDeviceScanningService: IBeaconDeviceScanningService
    private lateinit var recyclerView: RecyclerView
    private lateinit var bleDevicesAdapter: BleDevicesAdapter
    private lateinit var messageDialogText: TextView


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_devices)
        //initialize recyclerView
        recyclerView = findViewById(R.id.resultScannerDevices)
        recyclerView.layoutManager = LinearLayoutManager (this)
        //initialize the Ble adapter with the click event for each device found
        bleDevicesAdapter = BleDevicesAdapter (this) { device ->
            onDeviceClick(device)
        }
        recyclerView.adapter = bleDevicesAdapter
        iBeaconDeviceScanningService = application as IBeaconDeviceScanningService
        //I set up a Live Data observer for the signaling data
        val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(iBeaconDeviceScanningService.myIBeaconsRegion)
        regionViewModel.regionState.observe(this, monitoringObserver)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)

         messageDialogText = findViewById(R.id.MessageTextDialog)
        messageDialogText.visibility = View.INVISIBLE
        val stopScaning: Button = findViewById(R.id.stopScaning)
        stopScaning.setOnClickListener {
            val beaconManager = BeaconManager.getInstanceForApplication(this)
            beaconManager.stopRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
            beaconManager.stopMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        // override the back button event to stop scanning and close the activity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val beaconManager = BeaconManager.getInstanceForApplication(this@ListDevices)
                beaconManager.stopRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
                beaconManager.stopMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        //check if all permissions are accepted
        if (!BeaconScanPermissionsActivity.allPermissionsGranted(this, true)){
            // permissions are not supported and prompt the user
            val intent = Intent(this, BeaconScanPermissionsActivity::class.java)
            intent.putExtra("backgroundAccessRequested", true)
            startActivity(intent)
        } else {
            //permissions are accepted and start foreground service and scan
            if (BeaconManager.getInstanceForApplication(this).monitoredRegions.isEmpty()){
                (application as IBeaconDeviceScanningService).setupBeaconScanning()
                val beaconManager = BeaconManager.getInstanceForApplication(this)
                beaconManager.startMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
                beaconManager.startRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
            }
            if (BeaconManager.getInstanceForApplication(this).rangedRegions.isEmpty()){
                val beaconManager = BeaconManager.getInstanceForApplication(this)
                beaconManager.startRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
                beaconManager.startMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
            }
        }
    }

    // the livedata object of the monitor callback
    private val monitoringObserver = Observer<Int> {state ->
        if (state == MonitorNotifier.OUTSIDE){
            Log.d("RESULT_SCAN", "nu este nimic în jur")
            recyclerView.visibility =View.INVISIBLE
        messageDialogText.visibility = View.VISIBLE
            messageDialogText.text = this.getString(R.string.startBle)
        } else {
            Log.d("RESULT_SCAN", "ceva este înapropriere")
            recyclerView.visibility = View.VISIBLE
            messageDialogText.visibility = View.INVISIBLE
        }
    }

    //the livedata object from the callback range
    private val rangingObserver = Observer<Collection<Beacon>> {beacons ->
        Log.d("SearchFor", "callback to range")
        devicesFound.clear()
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()){
            beacons.sortedBy { it.distance }
                .map { beacon ->
                    Log.d("RESULT_SCAN", "Nume ${beacon.bluetoothName} mac adresa ${beacon.bluetoothAddress}")
                    //check if the device is selected and create the IBeacon object by putting it in the list
                    if (SelectedDevice.isSelectedDevice(beacon.id2.toInt(), beacon.id3.toInt())){
                        val iBeacon = IBeacon(beacon.bluetoothAddress).apply {
                            uuid = beacon.id1.toString()
                            major = beacon  .id2.toInt()
                            minor = beacon.id3.toInt()
                            rssi = beacon.rssi
                        }
                        devicesFound.add(iBeacon)
                    } else {
Toast.makeText(applicationContext, "selecția DVS nu este în apropiere", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        bleDevicesAdapter.updateDevices(devicesFound)
    }

    //function on click
    private fun onDeviceClick(device: IBeacon) {
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.stopRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
        beaconManager.stopMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
        val intentConnecting = Intent(applicationContext, CommunicationWithTheDevice::class.java)
        val selectedDevice = device.macAddress
        intentConnecting.putExtra("connectingTo", selectedDevice)
        startActivity(intentConnecting)
        finishAffinity()
    }


}

