package com.gtdvm.echopoint.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.gtdvm.echopoint.SelectedDevice
import com.gtdvm.echopoint.bluetoothService.IBeacon
import com.gtdvm.echopoint.bluetoothService.IBeaconDeviceScanningService
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.MonitorNotifier


// subscribe directly to BeaconManager (observeForever), independent of the activity's lifecycle. AndroidViewModel receives Application as a parameter, not Activity Context,
class BeaconViewModel(app: IBeaconDeviceScanningService) : ViewModel() {
    // State of the region
    enum class RegionStatus { INSIDE, OUTSIDE }
enum class DeviceStatus {                       FOUND, LOST }
    data class DeviceStatusEvent(val device: IBeacon, val status: DeviceStatus)
    // State of the region: INSIDE / OUTSIDE
    private val _regionStatus = MutableLiveData<RegionStatus>()
    val regionStatus: LiveData<RegionStatus> = _regionStatus
    // The current list of visible devices in the scanning range
    private val _deviceList = MutableLiveData<List<IBeacon>>(emptyList())
   val deviceList: LiveData<List<IBeacon>> = _deviceList
    // Observer : a device with the status of (FOUND / LOST)
    private val _deviceStatus = MutableLiveData<List<DeviceStatusEvent>>(emptyList())
    val deviceStatus: LiveData<List<DeviceStatusEvent>> = _deviceStatus
    // Local list of currently displayed devices — the source against which the temporary list received from AltBeacon is compared
    private val currentDevices = mutableMapOf<String, IBeacon>()
// The timestamp of the last detection per device (MAC -> ms)
private val timestamp = mutableMapOf<String, Long>()
private val deviceHasChangedState = mutableListOf<DeviceStatusEvent>()

    // Observer 1: state of the region
    val monitoringObserver = Observer<Int> { state ->
        Log.d(TAG, "sa declansat statusul")
        if (state == MonitorNotifier.OUTSIDE) {
            Log.d(TAG, "Region: OUTSIDE")
            _regionStatus.postValue(RegionStatus.OUTSIDE)
        } else {
            Log.d(TAG, "Region: INSIDE")
            _regionStatus.postValue(RegionStatus.INSIDE)
        }
    }

    // Observer: list of beacons received from AltBeacon every second
    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "sa returnat lista cu dispozitive")
        deviceHasChangedState.clear()
// Check each received beacon: is it new or existing?
        beacons.filter { SelectedDevice.isSelectedDevice(it.id2.toInt(), it.id3.toInt()) }
            .sortedBy { it.distance }
            .forEach { beacon -> beacon.checkDevice() }
// Check the local list: are there devices older than 10s?
        deleteOldDevices()
        // Notify subscribers that the list has been updated
        _deviceList.postValue(currentDevices.values.toList())
        if (deviceHasChangedState.isNotEmpty()) {
            _deviceStatus.postValue(deviceHasChangedState)
        }
    }

    // Check if the received beacon is already in the local list. If yes: just update the time. otherwise: add it as a new device.
    private fun Beacon.checkDevice() {
        val mac = this.bluetoothAddress
        if (currentDevices.containsKey(mac)) {
            updateTimeStamp(mac)
        } else {
            addDevice(this)
        }
    }

// Updates the timestamp of an existing device in the local list
private fun updateTimeStamp(mac: String) {
    timestamp[mac] = System.currentTimeMillis()
    Log.d(TAG, "Timestamp updated: $mac")
}

// Create the IBeacon object, add it to the local list, record the timestamp and send FOUND to the observer
private fun addDevice(beacon: Beacon) {
    val mac = beacon.bluetoothAddress
    val iBeacon = IBeacon(mac).apply {
        uuid = beacon.id1.toString()
        major = beacon.id2.toInt()
        minor = beacon.id3.toInt()
        rssi = beacon.rssi
    }
    currentDevices[mac] = iBeacon
    timestamp[mac] = System.currentTimeMillis()
    Log.d(TAG, "FOUND: major=${iBeacon.major} minor=${iBeacon.minor}")
    deviceHasChangedState.add(DeviceStatusEvent(iBeacon, DeviceStatus.FOUND))
}

// Delete the device from the local list and from the timestamp, send LOST to the observer
private fun deleteDevice(mac: String) {
    currentDevices[mac]?.let { device ->
        Log.d(TAG, "LOST: major=${device.major} minor=${device.minor}")
        deviceHasChangedState.add(DeviceStatusEvent(device, DeviceStatus.LOST))
    }
    currentDevices.remove(mac)
    timestamp.remove(mac)
}

// Go through the local list and delete devices that have not been seen for more than 10 seconds
private fun deleteOldDevices() {
    val now = System.currentTimeMillis()
    timestamp
        .filter { (_, ts) -> now - ts >= MAX_AGE_MS }
        .keys.toList()
        .forEach { mac -> deleteDevice(mac) }
}

    override fun onCleared() {
        super.onCleared()
        currentDevices.clear()
        timestamp.clear()
        deviceHasChangedState.clear()
        Log.d(TAG, "BeaconViewModel destroyed")
    }



    private companion object {
        const val TAG = "BeaconViewModel"
        const val MAX_AGE_MS = 1500L
    }
}

