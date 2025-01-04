package com.gtdvm.echopoint

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
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

        val communicationAppBar: Toolbar = findViewById(R.id.CommunicationDeviceAppBar)
        setSupportActionBar(communicationAppBar)
        supportActionBar?.title = this.getString(R.string.CommunicationAppBarTitle)

 val connectingToDeviceFormMacAddres = intent.getStringExtra("connectingTo")
        val notificationMessages: TextView = findViewById(R.id.NotificationText)
        val callSoundButton: Button = findViewById(R.id.CallButton)
        val stopCallButton: Button = findViewById(R.id.StopCallButton)
        bluetoothServices = BluetoothServices(this)
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        notificationViewModel.notificationData.observe(this) {data ->
            if (data == CommandsOptions.STOP_COLL){
                stopCallButton.visibility = View.GONE
                callSoundButton.visibility = View.VISIBLE
            } else {
                notificationMessages.text = data
            }
        }
        //notificationMessages.text = connectingToDeviceFormMacAddres //"se așteaptă notificările"
        bluetoothServices.connectToDevice(connectingToDeviceFormMacAddres!!)

        callSoundButton.setOnClickListener {
            callSoundButton.visibility = View.GONE
            stopCallButton.visibility = View.VISIBLE
            bluetoothServices.writeBleCharacteristic(CommandsOptions.START_COLL_VALUE)
        }

        stopCallButton.setOnClickListener {
            stopCallButton.visibility = View.GONE
            callSoundButton.visibility = View.VISIBLE
bluetoothServices.writeBleCharacteristic(CommandsOptions.STOP_COLL_VALUE)
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