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
                        import androidx.appcompat.widget.Toolbar
                        import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
                        import com.gtdvm.echopoint.bluetoothService.IBeaconDeviceScanningService
                        import com.gtdvm.echopoint.adapters.BleDevicesAdapter
                        import com.gtdvm.echopoint.bluetoothService.IBeacon
                        import com.gtdvm.echopoint.utils.AuxiliaryFunctions
                        import com.gtdvm.echopoint.utils.TextToSpeechHelper
                        import com.gtdvm.echopoint.viewmodel.BeaconViewModel
//                        import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
//import org.altbeacon.beacon.MonitorNotifier


class ListDevices : AppCompatActivity() {
    private lateinit var iBeaconDeviceScanningService: IBeaconDeviceScanningService
    private lateinit var recyclerView: RecyclerView
    private lateinit var bleDevicesAdapter: BleDevicesAdapter
    private lateinit var messageDialogText: TextView
private lateinit var textToSpeechHelper: TextToSpeechHelper
    private val beaconViewModel: BeaconViewModel get() = iBeaconDeviceScanningService.beaconViewModel

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_devices)
        // set appBar
        val listDevicesAppBar: Toolbar = findViewById(R.id.ListDevicesAppBar)
        setSupportActionBar(listDevicesAppBar)
        supportActionBar?.title = this.getString(R.string.ListDevicesAppBarTitle)

        messageDialogText = findViewById(R.id.MessageTextDialog)
        messageDialogText.text = this.getString(R.string.Scaning_BLE)

        //initialize the speech synthesizer
        textToSpeechHelper = TextToSpeechHelper(this)

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
        /*regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(iBeaconDeviceScanningService.myIBeaconsRegion)
    regionViewModel?.regionState?.observeForever(monitoringObserver)
regionViewModel ?.rangedBeacons?.   observeForever(rangingObserver)*/

// Observer 1: state of the region —
        beaconViewModel.regionStatus.observe(this, regionStatusObserver)
// Observer 2: processed list —
        beaconViewModel.deviceList.observe(this, deviceListObserver)
        // Observer 3: event per device —
        beaconViewModel.deviceStatus.observe(this, deviceStatusObserver)
        textToSpeechHelper.toSpeak(this.getString(R.string.Scaning_BLE))

        val stopScaning: Button = findViewById(R.id.stopScaning)
        stopScaning.setOnClickListener {
            textToSpeechHelper.releaseOfTtsResources()
            iBeaconDeviceScanningService.stopScaningForeGroundServices()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        // override the back button event to stop scanning and close the activity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                textToSpeechHelper.releaseOfTtsResources()
                iBeaconDeviceScanningService.stopScaningForeGroundServices()
                finish()
            }
        })
    }

    // The activity returns to the foreground
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "the application is back in the foreground")
        textToSpeechHelper.stopObservingViewModel()
        Log.d(TAG, "onResume - the activity retrieves the announcements")
        //check if all permissions are accepted
        if (!BeaconScanPermissionsActivity.allPermissionsGranted(this, true)){
            // permissions are not supported and prompt the user
            val intent = Intent(this, BeaconScanPermissionsActivity::class.java)
            intent.putExtra("backgroundAccessRequested", true)
            startActivity(intent)
        } else {
            //permissions are accepted and start foreground service and scan
            if (BeaconManager.getInstanceForApplication(this).monitoredRegions.isEmpty()){
                iBeaconDeviceScanningService.setupBeaconScanning()
                //val beaconManager = BeaconManager.getInstanceForApplication(this)
                //beaconManager.startMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
                //beaconManager.startRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
            }
            if (BeaconManager.getInstanceForApplication(this).rangedRegions.isEmpty()){
                iBeaconDeviceScanningService.setupBeaconScanning()
                //val beaconManager = BeaconManager.getInstanceForApplication(this)
                //beaconManager.startRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
                //beaconManager.startMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
            }
        }
    }

    // The activity runs in the background: TTS picks up new device announcements
    override fun onPause() {
        super.onPause()
        textToSpeechHelper.startObservingViewModel(beaconViewModel)
        Log.d(TAG, "onPause - TTS picks up the announcements")
        Log.d(TAG, "the application is in the background")
    }

    // We remove Forever observers and release TTS when the activity is destroyed.
    override fun onDestroy() {
        super.onDestroy()
        //regionViewModel?.regionState?.removeObserver(monitoringObserver)
        //regionViewModel?.rangedBeacons?.removeObserver(rangingObserver)
        Log.d(TAG, "onDestroy - observers removed")
        textToSpeechHelper.releaseOfTtsResources()
    }

    // the livedata object of the monitor callback
    private val regionStatusObserver = Observer<BeaconViewModel.RegionStatus> {status ->
        val message = when (status){
            BeaconViewModel.RegionStatus.INSIDE -> getString(R.string.Has_Been_Identified)
                BeaconViewModel.RegionStatus.OUTSIDE -> getString(R.string.startBle)
        }
        messageDialogText.text = message
        textToSpeechHelper.toSpeak(message)
        }

    //the livedata object from the callback range
    private val deviceListObserver = Observer<List<IBeacon>> {devices ->
        Log.d("SearchFor", "callback to range")
bleDevicesAdapter.updateDevices(devices)
        /* devicesFound.clear()
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
                        messageDialogText.text = this.getString(R.string.Has_Been_Identified)
                    } else {
                        messageDialogText.text = this.getString(R.string.message_selected_device_is_not_nearby)
                        textToSpeechHelper.toSpeak(this.getString(R.string.message_selected_device_is_not_nearby))
                    }
                }
        }
        bleDevicesAdapter.updateDevices(devicesFound)*/
    }

    // Observer : event per device
    private val deviceStatusObserver = Observer<List<BeaconViewModel.DeviceStatusEvent>> { events ->
    events.forEach { event ->
when (event.status) {
    BeaconViewModel.DeviceStatus.FOUND ->
        Toast.makeText(this, getString(
         R.string.DeviceWidgetList, AuxiliaryFunctions.getDeviceAnnouncementText(event.device),
            getString(R.string.Has_Been_Identified)),
            Toast.LENGTH_SHORT).show()
    BeaconViewModel.DeviceStatus.LOST ->
        Toast.makeText(this, getString(
            R.string.DeviceWidgetList, AuxiliaryFunctions.getDeviceAnnouncementText(event.device),
            getString(R.string.notification_device_lost_message)),
            Toast.LENGTH_SHORT).show()
}
    }
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

   private companion object {
         const val TAG = "ListDevices"
    }

}

