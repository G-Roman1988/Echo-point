package com.gtdvm.echopoint.bluetoothService

import  android.annotation.SuppressLint
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.RxBleDevice
import androidx.appcompat.app.AppCompatActivity
//import android.content.Context
//import android.os.RemoteException
import android.util.Log
//import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
//import com.gtdvm.echopoint.IBeacon
//import com.gtdvm.echopoint.IBeaconsView
import com.gtdvm.echopoint.NotificationViewModel
import com.gtdvm.echopoint.R
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.core.Observable
import java.util.UUID


class BluetoothServices(private val activity: AppCompatActivity) {
    private var rxBleClient: RxBleClient = RxBleClient.create(activity)
    var currentBleConnection: RxBleConnection? = null
    private var selectedRxBleDeviceClient: RxBleDevice? = null
    private var connectionDisposable: Disposable? = null
    //private val compositeDisposable = CompositeDisposable()
    private var personalBleService: BluetoothGattService? = null
    private var notificationCaracteristic: BluetoothGattCharacteristic? = null
    private var writeCaracteristic: BluetoothGattCharacteristic? = null
    private var descriptorBLE2902: BluetoothGattDescriptor? = null
   //private val devicesFound = mutableListOf<IBeacon>()

    //private val bleUUID = "A134D0B2-1DA2-1BA7-C94C-E8E00C9F7A2D" //"2D7A9F0C-E0E8-4CC9-A71B-A21DB2D034A1"
    private val servicesUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    private val writeCharacteristicUUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
    private val notificationUUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
    private val descriptorBLE2902UUID = "00002902-0000-1000-8000-00805f9b34fb"


    private val notificationViewNodel: NotificationViewModel by lazy { ViewModelProvider(activity)[NotificationViewModel::class.java] }
    //private val iBeaconsView: IBeaconsView by lazy { ViewModelProvider(activity)[IBeaconsView::class.java] }




    @SuppressLint("CheckResult")
    fun connectToDevice (deviceMacAddres: String){
        //notificationViewNodel.showNotificationData("connecting...")
       selectedRxBleDeviceClient = rxBleClient.getBleDevice(deviceMacAddres)
        connectionDisposable?.dispose()
          connectionDisposable = selectedRxBleDeviceClient?.establishConnection(false)
              ?.subscribe({ result ->
                  currentBleConnection = result
                  notificationViewNodel.showNotificationData(activity.getString(R.string.Message_After_Connecting))
                  result.discoverBleServices()
                      .subscribe({service ->
                          if (service.uuid == UUID.fromString(servicesUUID)){
                              personalBleService = service
                              result.enableNotificationCharacteristic()
                                  ?.subscribe({ resultData ->
                                      notificationViewNodel.showNotificationData(resultData.decodeToString())
                                  }, {

                                  })
                          }
                      },{

                      })
              },{

              })
    }

    private fun RxBleConnection.discoverBleServices (): Observable<BluetoothGattService> {
        return this.discoverServices()
                .toObservable()
                .flatMap{services ->
                    val myBleServices = services.bluetoothGattServices.find { it.uuid == UUID.fromString(servicesUUID) }
                    if (myBleServices != null) {
                        discoveryCharacteristics(myBleServices)
                            .flatMap { characteristics ->
                                when (characteristics.uuid) {
                                    UUID.fromString(notificationUUID) -> {
                                        notificationCaracteristic = characteristics
                                        Log.d("FINDING_CARACTERISTIC", "notification caracteristic are uuid: $notificationCaracteristic.uuid, property: $notificationCaracteristic.properties, permission: $notificationCaracteristic.permissions")
                                        notificationCaracteristic?.let {descriptorBLE2902 = it.getDescriptor(UUID.fromString(descriptorBLE2902UUID))}
                                    }
                                    UUID.fromString(writeCharacteristicUUID) -> {
                                        writeCaracteristic = characteristics
                                        Log.d("FINDING_CHARACTERISTIC", "write caracteristic are uuid: $writeCaracteristic.uuid, permission: $writeCaracteristic.permissions, properties: $writeCaracteristic.properties")
                                    }
                                }
                                Observable.just(myBleServices)
                            }
                    } else {
                        Observable.error(Throwable("serviciu cu uuid specificat nu afost gasit"))
                    }
                }
    }

    private fun discoveryCharacteristics (service: BluetoothGattService): Observable<BluetoothGattCharacteristic> {
        return Observable.fromIterable(service.characteristics)
    }

    @SuppressLint("CheckResult")
    fun writeBleCharacteristic (connection: RxBleConnection, sendingData: ByteArray) {
        try {
            Log.e("WRITE-CHARACTERISTIC", "function de scrierea characteristic sa apelat ")
            if (writeCaracteristic != null && writeCaracteristic!!.uuid == UUID.fromString(writeCharacteristicUUID)){
                val pew = writeCaracteristic!!.permissions// and
                val prw = writeCaracteristic!!.properties// and PROPERTY_WRITE
                val ptw = writeCaracteristic!!.writeType
                Log.e("CARACTERISTIC_PERMISSION", "caracteristica are permission $pew , properties :$prw, type: $ptw ")
                Log.e("VALUE", "property: $PROPERTY_WRITE, permission: $PERMISSION_WRITE")
                if ( prw == PROPERTY_WRITE){
                    Log.e("WRITE-CHARACTERISTIC", "caracteristica are aces sa scrie ")
                    when (ptw){
                        WRITE_TYPE_DEFAULT -> {
                            writeCaracteristic!!.writeType = WRITE_TYPE_DEFAULT
                            Log.e("WRITE_MDE", "set write mode default")
                        }
                        WRITE_TYPE_NO_RESPONSE ->{
                            writeCaracteristic!!.writeType = WRITE_TYPE_NO_RESPONSE
                            Log.e("WRITE_MODE", "no response write mode has been set")
                        }
                    }
                    connection.writeCharacteristic(writeCaracteristic!!, sendingData)
                        .subscribe({sucesedValue ->
                            if (sucesedValue.isNotEmpty()){
                                Log.e("WRITE-CHARACTERISTIC", "the writing feature confirmed with: ${sucesedValue.decodeToString()}")
                            }
                        },{throwable ->
//if (throwable is RxB){

//}
                        })
                } else{
                    Log.e("ERROR", "Caracteristica nu poate fii scrisa")
                }
            } else{
                Log.e("ERROR", "nu sa găsit caracteristica cu uuid specificat")
            }
        } catch (e: Exception){
            Log.e("EXCEPTION", "$e")
        }
    }

   @SuppressLint("CheckResult")
   private fun RxBleConnection.enableNotificationCharacteristic (): Observable<ByteArray>? {
        Log.e("NOTIFICATION-CHARACTERISTIC", "the notify function was called")
                 return if (notificationCaracteristic != null && notificationCaracteristic?.uuid == UUID.fromString(notificationUUID)){
                     Log.e("NOTIFICATION-CHARACTERISTIC", "caracteristica nu este null")
                    val prn = notificationCaracteristic!!.properties and PROPERTY_NOTIFY
                     val pen = notificationCaracteristic!!.permissions and PERMISSION_READ
                     Log.d("INFORMATION_CHARACTERISTIC", "propriety: $prn permission: $pen")
               if (prn == PROPERTY_NOTIFY){
                   Log.d("MESSAGE", "are permissions for notification")
                                               this.setupNotification(notificationCaracteristic!!)
                             .flatMap{notificationObservable ->
                                 if (!this.isEnabledDescriptor2902()){
                                     Log.d("DESCRIPTOR_STATUS", "descriptor status is disabled")
                                     this.writeDescriptor(descriptorBLE2902!!, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                                         .subscribe({
                                             Log.d("WRITE_DESCRIPTOR_STATUS", "descriptor was written successfully")
                                         },{
                                             Log.d("WRITE_DESCRIPTOR_STATUS", "an error occurred when writing the descriptor")
                                         })
                                 }
                                 notificationObservable}
               } else{
                   Log.d("ERROR", "cannot permissions for notification")
                   Observable.error(Throwable("caracteristica de notification nu se citeste sau de activat notification"))
               }
           } else{
                Observable.error(Throwable("caracteristica cu UUID specificat nu afost găsit sau este null"))
            }
    }

    private fun RxBleConnection.isEnabledDescriptor2902(): Boolean{
       descriptorBLE2902.let {
              val descriptor2902 = this.readDescriptor(descriptorBLE2902!!)
          return descriptor2902?.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ?: false
        }
    }

    fun disConnect (){
        try {
            connectionDisposable?.dispose()
            currentBleConnection?.let {
                currentBleConnection = null
                             //compositeDisposable.dispose()
            }
        } catch (error: Exception){
            Log.e("DISCONNECT_ERROR", "eroarea este: $error.message")
        }
    }


}

