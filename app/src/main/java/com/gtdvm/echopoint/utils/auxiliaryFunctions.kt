package com.gtdvm.echopoint.utils

import android.util.Log
import com.gtdvm.echopoint.bluetoothService.IBeacon
import com.gtdvm.echopoint.data.DataServices

object AuxiliaryFunctions {
    private val dataServices = DataServices()

    fun getDeviceAnnouncementText(device: IBeacon): String {
        val category = dataServices.getNameByMajor(device.major.toString())
        val number = dataServices.getNumberByMinor(device.major.toString(),
            device.minor.toString())
        return "$category $number"
    }

}

