package utils

import android.bluetooth.BluetoothAdapter

fun isBluetoothEnabled(): Boolean {
    return try {
        BluetoothAdapter.getDefaultAdapter().isEnabled
    } catch (ex: Exception) {
        false
    }
}