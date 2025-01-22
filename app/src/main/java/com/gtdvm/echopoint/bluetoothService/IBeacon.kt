package com.gtdvm.echopoint.bluetoothService

class IBeacon (mac: String?) {
    val macAddress =mac
    var manufacturer: String? = null
var uuid: String? = null
    var major: Int? = null
    var minor: Int? = null
var rssi: Int? =null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IBeacon) return false
        if (macAddress != other.macAddress) return false

        return true //super.equals(other)
    }

    override fun hashCode(): Int {
        return macAddress?.hashCode() ?: 0
        //super.hashCode()
    }

}