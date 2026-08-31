    package com.gtdvm.echopoint.bluetoothService

import android.app.*
//import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.gtdvm.echopoint.ListDevices
import com.gtdvm.echopoint.R
import com.gtdvm.echopoint.data.DataRepository
import org.altbeacon.beacon.*
import com.gtdvm.echopoint.viewmodel.BeaconViewModel
import com.gtdvm.echopoint.utils.AuxiliaryFunctions


class IBeaconDeviceScanningService: Application() {
    private val bleUUID = "A134D0B2-1DA2-1BA7-C94C-E8E00C9F7A2D"
    val myIBeaconsRegion: Region = Region("all-beacons", Identifier.parse(bleUUID), null, null)
    //val myIBeaconsRegion: Region = Region("all-beacons", null, null, null)
private val statusNotificationID = 2
    private val statusChannelID = "GuideBeep_status_channel"
private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    val beaconViewModel: BeaconViewModel by lazy { BeaconViewModel(this) }
    private var defaultAltBeaconRegionViewModel: RegionViewModel? = null


    override fun onCreate() {
        super.onCreate()
        val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(this)
        BeaconManager.setDebug(true)
        beaconManager.setEnableScheduledScanJobs(false)
        beaconManager.setBackgroundScanPeriod(1100L)
        beaconManager.setBackgroundBetweenScanPeriod(0L)
        val parser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        parser.setHardwareAssistManufacturerCodes(arrayOf(0x004c).toIntArray())

        beaconManager.beaconParsers.add(parser) //getBeaconParsers()
    DataRepository.dataPreparation(this)
        createStatusNotificationChannel()
    }

    fun setupBeaconScanning(){
val beaconManager = BeaconManager.getInstanceForApplication(this)
        try {
            setupForegroundService()
        } catch (e: SecurityException){
            Log.d(TAG, "Not setting up foreground service scanning until location permission granted by user. $e")
            return
        }
        beaconViewModel.clear()
        beaconManager.startMonitoring(myIBeaconsRegion)
        beaconManager.startRangingBeacons(myIBeaconsRegion)
        val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(myIBeaconsRegion)
        defaultAltBeaconRegionViewModel = regionViewModel
        if (!regionViewModel.rangedBeacons.hasObservers()) {
            regionViewModel.regionState.observeForever(beaconViewModel.monitoringObserver)
            regionViewModel.rangedBeacons.observeForever(beaconViewModel.rangingObserver)
            beaconViewModel.regionStatus.observeForever(regionStatusObserver)
            beaconViewModel.deviceStatus.observeForever(deviceStatusObserver)
            Log.d(TAG, "Scan started, observers registered")
        } else {
            Log.d(TAG, "Restarted scan, already existing observers")
        }

        //regionViewModel.regionState.observeForever(centralMonitoringObserver)
        //regionViewModel.rangedBeacons.observeForever(centralRangingObserver)
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
        //val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
builder.setChannelId(channel.id)
        Log.d("TAG", "Calling enableForegroundServiceScanning")
        BeaconManager.getInstanceForApplication(this).enableForegroundServiceScanning(builder.build(), 456)
        Log.d("TAG", "Back from  enableForegroundServiceScanning")
    }

    private val regionStatusObserver = Observer<BeaconViewModel.RegionStatus>{ status ->
            when (status) {
                BeaconViewModel.RegionStatus.INSIDE ->
                    sendNotification(title = getString(R.string.notification_entered_title),
                        message = getString(R.string.notification_entered_message))
                BeaconViewModel.RegionStatus.OUTSIDE ->
                    sendNotification(
                        title = getString(R.string.notification_exited_title),
                        message = getString(R.string.notification_exited_message))
            }
    }

    private val deviceStatusObserver = Observer<List<BeaconViewModel.DeviceStatusEvent>>{ events ->
            events.forEach { event ->
            when (event.status) {
                    BeaconViewModel.DeviceStatus.FOUND ->
                        sendNotification(title = AuxiliaryFunctions.getDeviceAnnouncementText(event.device),
                            message = getString(R.string.notification_device_found_message))
                        BeaconViewModel.DeviceStatus.LOST ->
                            sendNotification(title = AuxiliaryFunctions.getDeviceAnnouncementText(event.device),
                                message = getString(R.string.notification_device_lost_message))

                }
            }
    }

    //observe live data monitoring the status of IBeacon devices
    /* private val centralMonitoringObserver = Observer<Int> {state ->
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
   }*/

    fun stopScaningForeGroundServices (){
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.stopRangingBeacons(myIBeaconsRegion)
        beaconManager.stopMonitoring(myIBeaconsRegion)
        beaconManager.disableForegroundServiceScanning()
        defaultAltBeaconRegionViewModel?.let { regionViewModel ->
            regionViewModel.regionState.removeObserver(beaconViewModel.monitoringObserver)
            regionViewModel.rangedBeacons.removeObserver(beaconViewModel.rangingObserver)
            Log.d(TAG, "Observers removed")
        }
        defaultAltBeaconRegionViewModel = null
        cancelAllStatusNotifications()
        Log.d(TAG, "Scan stopped")
    }

    private fun sendNotification(title: String, message: String) {
         val intent = Intent(this, ListDevices::class.java)
         val pendingIntent = TaskStackBuilder.create(this).run {
             addNextIntent(intent)
             getPendingIntent(0,
                 PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
             ) }
         val notification = NotificationCompat.Builder(this, statusChannelID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_background)
             .setContentIntent(pendingIntent)
             .setOnlyAlertOnce(true)
             .build()
         notificationManager.notify(statusNotificationID, notification)
         Log.d(TAG, "Updated notification: $title $message")
    }

    private fun createStatusNotificationChannel() {
        val channel = NotificationChannel( statusChannelID,
            "GuideBeep stare scanare",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificari despre starea scanarii BLE"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun cancelAllStatusNotifications() {
        notificationManager.cancel(statusNotificationID)
        Log.d(TAG, "Notice deleted")
    }

    companion object {
        private const val TAG = "IBeaconDeviceScanningService"
    }
}

