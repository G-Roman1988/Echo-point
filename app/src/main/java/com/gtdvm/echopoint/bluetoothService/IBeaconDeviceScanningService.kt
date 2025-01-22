package com.gtdvm.echopoint.bluetoothService

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.gtdvm.echopoint.ListDevices
import com.gtdvm.echopoint.R
import org.altbeacon.beacon.*


class IBeaconDeviceScanningService: Application() {
    private val bleUUID = "A134D0B2-1DA2-1BA7-C94C-E8E00C9F7A2D" //"2D7A9F0C-E0E8-4CC9-A71B-A21DB2D034A1"
    val myIBeaconsRegion: Region = Region("all-beacons", Identifier.parse(bleUUID), null, null)

    override fun onCreate() {
        super.onCreate()
        val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(this)
BeaconManager.setDebug(true)
        val parser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        parser.setHardwareAssistManufacturerCodes(arrayOf(0x004c).toIntArray())
        beaconManager.getBeaconParsers().add( parser)
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

    private fun                 setupForegroundService(){
val builder = Notification.Builder(this, "GuideBeep_ForeGroundService_ID")
builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        builder.setContentTitle("Scanarea dispozitive BLE")
        val intent = Intent(this, ListDevices::class.java)
val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)
        val channel = NotificationChannel("GuideBeep_NotificationChannel_ID", "GuideBeep service", NotificationManager.IMPORTANCE_DEFAULT)
channel.description = "Notificarea utilizatorul rularea serviciu din prim plan"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
builder.setChannelId(channel.id)
        Log.d("TAG", "Calling enableForegroundServiceScanning")
        BeaconManager.getInstanceForApplication(this).enableForegroundServiceScanning(builder.build(), 456)
        Log.d("TAG", "Back from  enableForegroundServiceScanning")
    }

    //observe live data monitoring the status of IBeacon devices
    private val centralMonitoringObserver = Observer<Int> {state ->
        if (state == MonitorNotifier.OUTSIDE){
            Log.d("IBeaconDevice", "It is not detected")
        } else{
            Log.d("IBeaconDevice", "a new device is detected")
            sendNotification()
        }
    }

    private val centralRangingObserver = Observer<Collection<Beacon>> { beacons ->
        beacons.forEach {detectinningBeacon ->
            Log.d("IBeaconDevice", "${detectinningBeacon.bluetoothName}  ${detectinningBeacon.bluetoothAddress}")
        }

    }

    fun stopScaningForeGroundServices (){
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.stopRangingBeacons(myIBeaconsRegion)
        beaconManager.stopMonitoring(myIBeaconsRegion)
        beaconManager.disableForegroundServiceScanning()
    }

    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, "GuideBeep_notification_ID")
            .setContentTitle("Starea regiunei")
            .setContentText("Sa detectat un dispozitiv sau mai multe BLE.")
            .setSmallIcon(R.drawable.ic_launcher_background)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntent(Intent(this, ListDevices::class.java))
        val resultPendingIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(resultPendingIntent)
        val channel =  NotificationChannel("GuideBeep_NotificationChannel_ID", "GuideBeep_alertă", NotificationManager.IMPORTANCE_DEFAULT)
        channel.setDescription("Notificarea utilizatorul la detectarea unui sau mai multe dispozitive BLE")
        val notificationManager =  getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId(channel.getId())
        notificationManager.notify(1, builder.build())
    }




}
