package com.gtdvm.echopoint

object Utils {
    fun toHex (arrayBytes: ByteArray): String {
        val result = StringBuffer()
        arrayBytes.forEach {
            result.append(String.format("%02X", it))
        }
        return result.toString()
    }

   private fun ByteArray.getSubByte (hom: Int, end:    Int): ByteArray{
        //val result = StringBuffer()
        val result = mutableListOf<Byte>()
        var index = 0
        forEach {
            if (index in hom..end) {
//result.append(String.format("%02X", it))
                result.add(it)
            }
            index++
        }
        return result.toByteArray() //toString()
    }

    fun ByteArray.getMajor (): ByteArray{
        return this.getSubByte(25, 26)
    }

    fun ByteArray.getMinor (): ByteArray{
        return this.getSubByte(27, 28)
    }

    fun ByteArray.getUUID (): ByteArray{
        return this.getSubByte(9, 24)
    }

    fun uuidFormat (arrayBytes: ByteArray): String{
        return String.format("%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X",
            arrayBytes[15], arrayBytes[14], arrayBytes[13], arrayBytes[12], arrayBytes[11], arrayBytes[10], arrayBytes[9], arrayBytes[8], arrayBytes[7], arrayBytes[6], arrayBytes[5], arrayBytes[4], arrayBytes[3], arrayBytes[2], arrayBytes[1], arrayBytes[0])
    }


}
