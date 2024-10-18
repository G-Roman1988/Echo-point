package com.gtdvm.echopoint.bluetoothService

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.gtdvm.echopoint.IBeacon
import com.gtdvm.echopoint.IBeaconsView
import com.gtdvm.echopoint.ListDevices
import com.gtdvm.echopoint.R
import org.altbeacon.beacon.*
//import org.altbeacon.bluetooth.BluetoothMedic



class IBeaconDeviceScanningService: Application() {
    private val devicesFound = mutableListOf<IBeacon>()
    //private var scanDisposable: Disposable? = null
    private val bleUUID = "A134D0B2-1DA2-1BA7-C94C-E8E00C9F7A2D" //"2D7A9F0C-E0E8-4CC9-A71B-A21DB2D034A1"
    //private val myIBeaconsRegion: Region = Region("IBeacons", Identifier.parse(bleUUID), null, null)
     val myIBeaconsRegion = Region("all-beacons", null, null, null)
    //private val iBeaconsView: IBeaconsView by lazy { ViewModelProvider(this)[IBeaconsView::class.java] }

    override fun onCreate() {
        super.onCreate()
        val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(this)
BeaconManager.setDebug(true)
        //val parser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        //parser.setHardwareAssistManufacturerCodes(arrayOf(0x004c).toIntArray())
        val parser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        parser.setHardwareAssistManufacturerCodes(arrayOf(0x004c).toIntArray())
        beaconManager.getBeaconParsers().add( parser)
        //beaconManager.beaconParsers.add(parser)
        //BluetoothMedic.getInstance().enablePeriodicTests(this, BluetoothMedic.SCAN_TEST + BluetoothMedic.TRANSMIT_TEST)
//setupBeaconScanning()
        setupForegroundService()
    }

    fun setupBeaconScanning(){
val beaconManager = BeaconManager.getInstanceForApplication(this)
        try {
            setupForegroundService()
        } catch (e: SecurityException){
            Log.d("TAG", "Not setting up foreground service scanning until location permission granted by user")
            return
        }
        beaconManager.startMonitoring(myIBeaconsRegion)
        beaconManager.startRangingBeacons(myIBeaconsRegion)
        val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(myIBeaconsRegion)
        regionViewModel.regionState.observeForever(centralMonitoringObserver)
        regionViewModel.rangedBeacons.observeForever(centralRangingObserver)
    }

    fun setupForegroundService(){
val builder = Notification.Builder(this, "IBeaconDeviceScanningService")
builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        builder.setContentTitle("Scanning for Beacons")
        val intent = Intent(this, ListDevices::class.java)
val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)
        val channel = NotificationChannel("beacon-ref-notification-id", "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT)
channel.description = "My Notification Channel Description"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
builder.setChannelId(channel.id)
        Log.d("TAG", "Calling enableForegroundServiceScanning")
        BeaconManager.getInstanceForApplication(this).enableForegroundServiceScanning(builder.build(), 456)
        Log.d("TAG", "Back from  enableForegroundServiceScanning")
    }

    //observe live data monitoring the status of IBeacon devices
    val centralMonitoringObserver = Observer<Int> {state ->
        if (state == MonitorNotifier.OUTSIDE){
            Log.d("IBeaconDevice", "It is not detected")
        } else{
            Log.d("IBeaconDevice", "a new device is detected")
            sendNotification()
        }
    }

    val centralRangingObserver = Observer<Collection<Beacon>> {beacons ->
        beacons.forEach {detectinningBeacon ->
            Log.d("IBeaconDevice", "${detectinningBeacon.bluetoothName}  ${detectinningBeacon.bluetoothAddress}")
        }

    }

    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, "beacon-ref-notification-id")
            .setContentTitle("IBeaconDeviceScanningService")
            .setContentText("A beacon is nearby.")
            .setSmallIcon(R.drawable.ic_launcher_background)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntent(Intent(this, ListDevices::class.java))
        val resultPendingIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(resultPendingIntent)
        val channel =  NotificationChannel("beacon-ref-notification-id",
            "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT)
        channel.setDescription("My Notification Channel Description")
        val notificationManager =  getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId(channel.getId())
        notificationManager.notify(1, builder.build())
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


}
