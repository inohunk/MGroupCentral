package ru.hunkel.mgroupcentral.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import ru.hunkel.mgroupcentral.IMGroupApplication

const val TAG = "MGroupService"

class MGroupService : Service() {

    inner class MGroupServiceImpl : IMGroupApplication.Stub() {
        override fun register(appPackage: String?) {
            Log.i(
                TAG, ".\nNew app registration\n" +
                        "\t\tpackage: $appPackage"
            )
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return MGroupServiceImpl()
    }
}
