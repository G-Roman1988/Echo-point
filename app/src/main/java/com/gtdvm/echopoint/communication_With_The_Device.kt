package com.gtdvm.echopoint

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
//import io.reactivex.rxjava3.disposables.Disposable
//import io.reactivex.rxjava3.schedulers.Schedulers
//import android.util.Log
//import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gtdvm.echopoint.bluetoothService.BluetoothServices

//import androidx.lifecycle.get


class CommunicationWithTheDevice : AppCompatActivity() {
    private lateinit var bluetoothServices: BluetoothServices
    private lateinit var notificationViewModel: NotificationViewModel
    //private var notificationDisposable: Disposable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication_with_the_device)
 val connectingToDeviceFormMacAddres = intent.getStringExtra("connectingTo")
        val notificationMessages: TextView = findViewById(R.id.NotificationText)
bluetoothServices = BluetoothServices(this)
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        notificationViewModel.notificationData.observe(this) {data ->
        notificationMessages.text = data
        }
val callSoundButton: Button = findViewById(R.id.CallButton)
        val stopCallButton: Button = findViewById(R.id.StopCallButton)
        //notificationMessages.text = connectingToDeviceFormMacAddres //"se așteaptă notificările"
bluetoothServices.connectToDevice(connectingToDeviceFormMacAddres.toString())

        callSoundButton.setOnClickListener {
            callSoundButton.visibility = View.GONE
            stopCallButton.visibility = View.VISIBLE
            bluetoothServices.writeBleCharacteristic(bluetoothServices.currentBleConnection!!, CommandsOptions.START_COLL_VALUE)
        }

        stopCallButton.setOnClickListener {
            stopCallButton.visibility = View.GONE
            callSoundButton.visibility = View.VISIBLE
bluetoothServices.writeBleCharacteristic(bluetoothServices.currentBleConnection!!, CommandsOptions.STOP_COLL_VALUE)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                bluetoothServices.disConnect()
                startActivity(Intent(this@CommunicationWithTheDevice, MainActivity::class.java))
                finishAffinity()

            }
        })



    }


}