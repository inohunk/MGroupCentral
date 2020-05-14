package utils

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanFilter.Builder
import models.Beacon
import java.nio.ByteBuffer
import java.util.*

/**
 * Use this for convert an UUID to ByteArray
 */
fun uuidToBytes(uuid: UUID): ByteArray {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(uuid.mostSignificantBits)
    bb.putLong(uuid.leastSignificantBits)
    return bb.array()
}

/**
 * Used for converting an int in range 0..65535 to ByteArray with 2 items. Use this for convert number to major, minor
 * @param value number that will be converted to byte array
 */
fun integerToTwoBytes(value: Int): ByteArray {
    val bytes = ByteArray(2)
    bytes[0] = (value / 256).toByte()
    bytes[1] = (value % 256).toByte()
    return bytes
}

fun getScanFilter(beacon: Beacon): ScanFilter {
    val builder = Builder()
    // the manufacturer data byte is the filter!
    val manufacturerData = byteArrayOf(
        0, 0,
        // 128 bit UUID
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        // major
        0, 0,
        // minor
        0, 0,
        // txPower
        0
    )

    val manufacturerDataMask = byteArrayOf(
        0, 0,
        // 128 bit UUID
        1, 1, 1, 1,
        1, 1, 1, 1,
        1, 1, 1, 1,
        1, 1, 1, 1,
        // major
        1, 1,
        // minor
        0, 0,
        // txPower
        0
    )

    //Insert uuid to manufacturer data
    System.arraycopy(
        uuidToBytes(beacon.uuid),
        0,
        manufacturerData,
        2,
        16
    )

    //Insert major to manufacturer data
    System.arraycopy(
        integerToTwoBytes(beacon.major),
        0,
        manufacturerData,
        18,
        2
    )

    //Insert minor to manufacturer data
    System.arraycopy(
        integerToTwoBytes(beacon.minor),
        0,
        manufacturerData,
        20,
        2
    )

    //Add manufacturer data to ScanFilter
    builder.setManufacturerData(
        76,
        manufacturerData,
        manufacturerDataMask
    )
    return builder.build()
}
