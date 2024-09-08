package com.gtdvm.echopoint

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class IBeaconsView : ViewModel() {
private val _beacons = MutableLiveData<List<IBeacon>>()
    val beacons: LiveData<List<IBeacon>> get() = _beacons

fun updateListIBeacons (listIBeacon: List<IBeacon>){
_beacons.value = listIBeacon
}

}

