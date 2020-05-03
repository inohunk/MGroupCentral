package ru.hunkel.mgroupcentral.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import ru.hunkel.mgroupcentral.IMGroupApplication
import ru.hunkel.mgroupcentral.database.entities.Module
import ru.hunkel.mgroupcentral.managers.MGroupDatabaseManager

const val TAG = "MGroupService"

class MGroupService : Service() {

    private lateinit var mDatabaseManager: MGroupDatabaseManager

    inner class MGroupServiceImpl : IMGroupApplication.Stub() {
        override fun register(appPackage: String?) {
            val newModule = Module(
                appPackage = appPackage!!
            )
            val module = mDatabaseManager.actionGetModuleByPackage(appPackage)
            if (module == null) {
                mDatabaseManager.actionAddModule(newModule)
                Log.i(
                    TAG, ".\nNew app registration\n" +
                            "\t\tpackage: $appPackage"
                )
            }
        }

        override fun registerWithSettings(appPackage: String?, appSettings: String?) {
            val newModule = Module(
                appPackage = appPackage!!,
                appSettings = appSettings!!
            )
            val module = mDatabaseManager.actionGetModuleByPackage(appPackage)
            if (module == null) {
                mDatabaseManager.actionAddModule(newModule)
                Log.i(
                    TAG, ".\nNew app registration with settings\n" +
                            "\t\tpackage: $appPackage" +
                            "\t\tsettings: $appSettings"
                )
            }
        }

        override fun registerWithAppService(
            appPackage: String?,
            appSettings: String?,
            appService: String?
        ) {
            val newModule = Module(
                appPackage = appPackage!!,
                appSettings = appSettings!!,
                appService = appService!!
            )
            val module = mDatabaseManager.actionGetModuleByPackage(appPackage)
            if (module == null) {
                mDatabaseManager.actionAddModule(newModule)
                Log.i(
                    TAG, ".\nNew app registration with settings\n" +
                            "\t\tpackage: $appPackage" +
                            "\t\tsettings: $appSettings"
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mDatabaseManager = MGroupDatabaseManager(this)
    }

    override fun onBind(intent: Intent): IBinder {
        return MGroupServiceImpl()
    }
}
