package com.gtdvm.echopoint

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider

class ScanAndCommunicationSelectedDevice : AppCompatActivity() {
    private lateinit var bluetoothServices: BluetoothServices
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var iBeaconsView: IBeaconsView
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

        bluetoothServices = BluetoothServices(this)
        val messageTextView: TextView = findViewById(R.id.MessageText)
        val callButton: Button = findViewById(R.id.callButton)
        val stopButton: Button = findViewById(R.id.stopCallButton)
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        notificationViewModel.notificationData.observe(this) {data ->
            messageTextView.text = data
        }
iBeaconsView = ViewModelProvider(this)[IBeaconsView::class.java]
        iBeaconsView.beacons.observe(this){deviceBeacon ->
            macAddresByCandedateDevice = deviceBeacon[0].macAddress!!
            bluetoothServices.stopScan()
            bluetoothServices.connectToDevice(macAddresByCandedateDevice)
        }

        messageTextView.text = getString(R.string.startBle)

        bluetoothServices.startBluetoothScan()

        callButton.setOnClickListener{
callButton.visibility = View.GONE
            bluetoothServices.writeBleCharacteristic(bluetoothServices.currentBleConnection!!, CommandsOptions.START_COLL_VALUE)
            stopButton.visibility = View.VISIBLE
        }

        stopButton.setOnClickListener {
stopButton.visibility = View.GONE
            bluetoothServices.writeBleCharacteristic(bluetoothServices.currentBleConnection!!, CommandsOptions.STOP_COLL_VALUE)
            callButton.visibility = View.VISIBLE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
 if (bluetoothServices.currentBleConnection != null){
     bluetoothServices.disConnect()
     finish()
 } else{
     bluetoothServices.stopScan()
     finish()
 }
            }
        })


    }
}