package org.web3j.evm.debugger.utils

object StringUtils {

    fun arrayToReadableString(array: ByteArray): String {
        return if(array.isEmpty()) " " else String(arrayWithoutEmptyBytes(array))
    }

    private fun arrayWithoutEmptyBytes(array: ByteArray) : ByteArray{
        return array.filterNot {it == 0.toByte()}.toByteArray()
    }

    fun hexToString(bytes: ByteArray) : String{
        return String.format("%x", bytes)
    }

}