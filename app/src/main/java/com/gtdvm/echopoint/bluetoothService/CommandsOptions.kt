package com.gtdvm.echopoint.bluetoothService

object CommandsOptions {
    val START_COLL_VALUE: ByteArray = byteArrayOf(0X31)
    val STOP_COLL_VALUE: ByteArray = byteArrayOf(0X30)
    const val STOP_COLL = "0"
    val DETAILS_INFORMATION_VALUE: ByteArray = byteArrayOf(0X32)

}