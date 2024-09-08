package com.gtdvm.echopoint

import  android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.IBinder
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
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
import com.polidea.rxandroidble3.RxBleDeviceServices
import com.polidea.rxandroidble3.scan.ScanSettings
import com.polidea.rxandroidble3.scan.ScanFilter
import com.polidea.rxandroidble3.scan.ScanResult
import com.polidea.rxandroidble3.RxBleScanResult
import com.polidea.rxandroidble3.exceptions.BleException
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException
import com.polidea.rxandroidble3.NotificationSetupMode
import com.polidea.rxandroidble3.RxBleConnection.RxBleConnectionState
import android.location.LocationManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.ServiceConnection
import android.icu.number.IntegerWidth
import android.os.Build
import android.os.Looper
import android.os.ParcelUuid
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.defaultDecayAnimationSpec
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringArrayResource
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleSource
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.operators.observable.ObservableIgnoreElementsCompletable
import io.reactivex.rxjava3.internal.operators.observable.ObservableJust
import io.reactivex.rxjava3.internal.operators.observable.ObservableScan
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.newFixedThreadPoolContext
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.util.UUID
//import com.gtdvm.echopoint.Utils.getMajor
//import com.gtdvm.echopoint.Utils.getMinor
//import com.gtdvm.echopoint.Utils.getUUID
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.*


//import android.window.OnBackInvokedCallback
//import android.window.OnBackInvokedDispatcher
//import androidx.activity.OnBackPressedCallback

//import androidx.core.app.ComponentActivity
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.ActivityResult
//import androidx.activity.result.ActivityResultCallback
//import android.Manifest

class BluetoothServices(private val activity: AppCompatActivity): RangeNotifier, MonitorNotifier {
    private var managerDevices: ManagerDevices = ManagerDevices(activity)
    private var beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(activity)
    private var rxBleClient: RxBleClient = RxBleClient.create(activity)
    var currentBleConnection: RxBleConnection? = null
    private var selectedRxBleDeviceClient: RxBleDevice? = null
    private var scanDisposable: Disposable? = null
    private var connectionDisposable: Disposable? = null
    //private val compositeDisposable = CompositeDisposable()
    private var personalBleService: BluetoothGattService? = null
    private var notificationCaracteristic: BluetoothGattCharacteristic? = null
    private var writeCaracteristic: BluetoothGattCharacteristic? = null
    private var descriptorBLE2902: BluetoothGattDescriptor? = null
   private val devicesFound = mutableListOf<IBeacon>()

    private val bleUUID = "A134D0B2-1DA2-1BA7-C94C-E8E00C9F7A2D" //"2D7A9F0C-E0E8-4CC9-A71B-A21DB2D034A1"
    private val servicesUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    private val writeCharacteristicUUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
    private val notificationUUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
    private val descriptorBLE2902UUID = "00002902-0000-1000-8000-00805f9b34fb"


    private val notificationViewNodel: NotificationViewModel by lazy { ViewModelProvider(activity)[NotificationViewModel::class.java] }
    private val iBeaconsView: IBeaconsView by lazy { ViewModelProvider(activity)[IBeaconsView::class.java] }
    private val myIBeaconsRegion: Region = Region("IBeacons", Identifier.parse(bleUUID), null, null)

    init {
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
createNotificationChannel()
    BeaconManager.setDebug(true)
            beaconManager.enableForegroundServiceScanning(createNotification(), NOTIFICATION_ID)
        beaconManager.foregroundScanPeriod = 1100L
        beaconManager.foregroundBetweenScanPeriod = 0L
        beaconManager.setEnableScheduledScanJobs(false)
        beaconManager.addMonitorNotifier(this)
        beaconManager.addRangeNotifier(this)
    }



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

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
beacons?.forEach{beacon ->
if (isPointer(beacon.id1)){
val iBeacon = IBeacon(beacon.bluetoothAddress)
    iBeacon.uuid = beacon.id1.toString()
    iBeacon.major = beacon.id2.toInt()
    iBeacon.minor = beacon.id3.toInt()
    iBeacon.rssi = beacon.rssi
    iBeacon.manufacturer = beacon.manufacturer.toString()
    devicesFound.add(iBeacon)
    iBeaconsView.updateListIBeacons(devicesFound)
}
}
    }


    override fun didEnterRegion(region: Region?) {
Log.d("TAG", "region is ${region?.bluetoothAddress}")
    }

    override fun didExitRegion(region: Region?) {
        if (devicesFound.any { it.macAddress == region?.bluetoothAddress }){
this.removeDeviceOfList(region?.bluetoothAddress)
iBeaconsView.updateListIBeacons(devicesFound)
        }
    }

    override fun didDetermineStateForRegion(state: Int, region: Region?) {
when (state) {
MonitorNotifier.INSIDE -> Log.d("BeaconMonitoring", "Inside region: ${region?.uniqueId}")
    MonitorNotifier.OUTSIDE -> Log.d("BeaconMonitoring", "Outside region: ${region?.uniqueId}")
}
    }


    fun startBluetoothScan () {
                     if (managerDevices.areBluetoothPermissionsGranted() && managerDevices.isBluetoothEnabled() && managerDevices.isLocationActive() && managerDevices.notificationsAreAccepted()) {
val checkbeacon = beaconManager.checkAvailability()
                      Log.d("CHECK_BEACON", "is: $checkbeacon")

                    try {
                        beaconManager.startMonitoring(myIBeaconsRegion)
                        beaconManager.startRangingBeacons(myIBeaconsRegion)
                        beaconManager.requestStateForRegion(myIBeaconsRegion)
                    } catch (e: RemoteException){
e.printStackTrace()
                        //emiter.onError(e)
                    }
//return devicesFound
            }
        }

    fun stopScan () {
        scanDisposable?.dispose()
        //val region = Region("myRangingUniqueId", null, null, null)
        try {
            beaconManager.stopMonitoring(myIBeaconsRegion)
            beaconManager.stopRangingBeacons(myIBeaconsRegion)
            beaconManager.disableForegroundServiceScanning()
        } catch (e: RemoteException){
e.printStackTrace()
        }
    }

    private fun isPointer(deviceUUID: Identifier): Boolean {
        return deviceUUID.toString().equals(bleUUID, ignoreCase = true)
    }

    private fun removeDeviceOfList(deviceMacAddres: String?){
        devicesFound.forEachIndexed { index, iBeacon ->
            if (iBeacon.macAddress == deviceMacAddres){
                devicesFound.removeAt(index)
                return@forEachIndexed
            }
        }
    }

    private fun createNotificationChannel(){
Log.d("NOTIFICATION_CHANEL", "the create notification channel function was called")
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
        val channel = NotificationChannel(NOTIFICATIONCHANNEL_ID, "Beacon Scan Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            //.apply { description = "Channel for Beacon Scan Service" }
        val notificationManager: NotificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
//}
    }

    private fun createNotification(): Notification {
        Log.d("CREATE_NOTIFICATION", "the notification has been created")
        //val notificationIntent = Intent(activity, MainActivity::class.java)
        //val pendingIntent = PendingIntent.getActivity(activity, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(activity, NOTIFICATIONCHANNEL_ID)
            .setContentTitle("Scanning for Beacons")
            .setContentText("The service is scanning for beacons in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }


    companion object{
        private const val NOTIFICATIONCHANNEL_ID = "Echo_point_scan_service"
        private const val NOTIFICATION_ID = 456
    }




}

