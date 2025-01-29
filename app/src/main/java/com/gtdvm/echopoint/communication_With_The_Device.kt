package com.gtdvm.echopoint

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.gtdvm.echopoint.bluetoothService.BluetoothServices
import com.gtdvm.echopoint.bluetoothService.CommandsOptions
import com.gtdvm.echopoint.utils.Timer


class CommunicationWithTheDevice : AppCompatActivity() {
    private lateinit var bluetoothServices: BluetoothServices
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication_with_the_device)

        //setting appBar
        val communicationAppBar: Toolbar = findViewById(R.id.CommunicationDeviceAppBar)
        setSupportActionBar(communicationAppBar)
        supportActionBar?.title = this.getString(R.string.CommunicationAppBarTitle)

//initialize the timer
        timer = Timer{
            onTimerExpired()
        }

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
                timer.startTimer()
            } else {
                notificationMessages.text = data
                //timer.stopTimer()
            }
        }
        //notificationMessages.text = connectingToDeviceFormMacAddres //"se așteaptă notificările"
        bluetoothServices.connectToDevice(connectingToDeviceFormMacAddres!!)

        callSoundButton.setOnClickListener {
            callSoundButton.visibility = View.GONE
            stopCallButton.visibility = View.VISIBLE
            bluetoothServices.writeBleCharacteristic(CommandsOptions.START_COLL_VALUE)
            timer.stopTimer()
        }

        stopCallButton.setOnClickListener {
            stopCallButton.visibility = View.GONE
            callSoundButton.visibility = View.VISIBLE
bluetoothServices.writeBleCharacteristic(CommandsOptions.STOP_COLL_VALUE)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
finishActivity()
            }
        })
    }

    private fun finishActivity(){
        bluetoothServices.disConnect()
        startActivity(Intent(this@CommunicationWithTheDevice, MainActivity::class.java))
        finishAffinity()
    }

    private fun onTimerExpired(){
        Log.d("CommunicationWithTheDevice", "Timer expirat - se deconectează dispozitivul BLE.")
        finishActivity()
    }


}