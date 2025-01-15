package com.gtdvm.echopoint

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.gtdvm.echopoint.bluetoothService.IBeaconDeviceScanningService
import com.gtdvm.echopoint.bluetoothService.BluetoothServices
import com.gtdvm.echopoint.utils.Timer
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier


class ScanAndCommunicationSelectedDevice : AppCompatActivity() {
    private lateinit var bluetoothServices: BluetoothServices
    private lateinit var iBeaconDeviceScanningService: IBeaconDeviceScanningService
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var timer: Timer
    private var macAddresByCandedateDevice: String = ""

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_and_communication_selected_device)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//setting appbar
        val scaningCommunicationAppBar: Toolbar = findViewById(R.id.ScaningCommunicationAppBar)
        setSupportActionBar(scaningCommunicationAppBar)
        supportActionBar?.title = this.getString(R.string.ScaningCommunicationAppBarTitle)

//initialize the timer
        timer = Timer{
            onTimerExpired()
        }

        iBeaconDeviceScanningService = application as IBeaconDeviceScanningService
        bluetoothServices = BluetoothServices(this)
        val messageTextView: TextView = findViewById(R.id.MessageText)
        val callButton: Button = findViewById(R.id.callButton)
        val stopButton: Button = findViewById(R.id.stopCallButton)
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        notificationViewModel.notificationData.observe(this) {data ->
            if (data == CommandsOptions.STOP_COLL){
            stopButton.visibility = View.GONE
            callButton.visibility = View.VISIBLE
                timer.startTimer()
            } else{
                messageTextView.text = data
            }
        }
        messageTextView.text = getString(R.string.startBle)
        //create the region and retrieve the monitoring, range of live data objects
        val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(iBeaconDeviceScanningService.myIBeaconsRegion)
        regionViewModel.regionState.observe(this, monitoringObserver)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)

        callButton.setOnClickListener{
            callButton.visibility = View.GONE
            bluetoothServices.writeBleCharacteristic(CommandsOptions.START_COLL_VALUE)
            stopButton.visibility = View.VISIBLE
            timer.stopTimer()
        }

        stopButton.setOnClickListener {
            stopButton.visibility = View.GONE
            bluetoothServices.writeBleCharacteristic(CommandsOptions.STOP_COLL_VALUE)
            callButton.visibility = View.VISIBLE
        }

        // override the Back button event to log out of the device if it is connected
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bluetoothServices.isConnected()){
                    bluetoothServices.disConnect()
                    finish()
                } else{
                    iBeaconDeviceScanningService.stopScaningForeGroundServices()
                    finish()
                }
            }
        })
    }

    // override the summary function
    override fun onResume() {
        super.onResume()
        //check if all permissions are accepted
        if (!BeaconScanPermissionsActivity.allPermissionsGranted(this, true)) {
            // permissions are not supported and prompt the user
            val intent = Intent(this, BeaconScanPermissionsActivity::class.java)
            intent.putExtra("backgroundAccessRequested", true)
            startActivity(intent)
        } else {
            //permissions are accepted and start foreground service and scan
            if (    BeaconManager.getInstanceForApplication(this).monitoredRegions.isEmpty()) {
                (application as IBeaconDeviceScanningService).setupBeaconScanning()
                val beaconManager = BeaconManager.getInstanceForApplication(this)
                beaconManager.startMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
                beaconManager.startRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
            }
            if (    BeaconManager.getInstanceForApplication(this).rangedRegions.isEmpty()) {
                val beaconManager = BeaconManager.getInstanceForApplication(this)
                beaconManager.startRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
                beaconManager.startMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
            }
        }
    }

    private fun onTimerExpired(){
        Log.d("CommunicationWithTheDevice", "Timer expirat - se deconectează dispozitivul BLE.")
        bluetoothServices.disConnect()
    }

    // the livedata object of the monitor callback
    private val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.OUTSIDE){
            Log.d("RESULT_SCAN", "there is nothing around")
        } else {
            Log.d("SCANING", "something is appropriation")
        }
    }

    //the livedata object from the callback range
    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
            beacons.sortedBy { it.distance }
                .map { beacon ->
                    if (SelectedDevice.isSelectedDevice(beacon.id2.toInt(), beacon.id3.toInt())){
                        macAddresByCandedateDevice = beacon.bluetoothAddress
                        iBeaconDeviceScanningService.stopScaningForeGroundServices()
                        //val beaconManager = BeaconManager.getInstanceForApplication(this)
                        //beaconManager.stopRangingBeacons(iBeaconDeviceScanningService.myIBeaconsRegion)
                        //beaconManager.stopMonitoring(iBeaconDeviceScanningService.myIBeaconsRegion)
                        bluetoothServices.connectToDevice(macAddresByCandedateDevice)
                    }
                }
        }
    }



}