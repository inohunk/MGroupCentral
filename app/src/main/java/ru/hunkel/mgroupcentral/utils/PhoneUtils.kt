package utils

import android.content.Context
import android.content.Intent

import android.content.IntentFilter
import android.os.BatteryManager


fun getBatteryLevel(context: Context): Int {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus: Intent = context.registerReceiver(null, filter)!!
    return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
}