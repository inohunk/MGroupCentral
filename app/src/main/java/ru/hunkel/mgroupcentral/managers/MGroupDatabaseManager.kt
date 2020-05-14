package ru.hunkel.mgroupcentral.managers

import android.content.Context
import android.util.Log
import ru.hunkel.mgroupcentral.database.MGroupCentralDatabase
import ru.hunkel.mgroupcentral.database.entities.Module

const val TAG = "MGroupDatabaseManager"

class MGroupDatabaseManager(context: Context) {

    private var mDatabase = MGroupCentralDatabase.getInstance(context)

    fun actionAddModule(module: Module) {
        val insertedId = mDatabase.trackingModel().addModule(module)
        Log.i(
            TAG,
            ".\nADD MODULE:\n" +
                    "\t\tnew module id: $insertedId"
        )
    }

    fun actionGetAllModules(): List<Module> {
        val modules = mDatabase.trackingModel().getAllModules()
        Log.i(
            TAG, ".\n" +
                    "GET ALL MODULES:\n" +
                    "\t\tsize: ${modules.size}"
        )
        return modules
    }

    fun actionGetModuleByPackage(appPackage: String): Module? {
        val module = mDatabase.trackingModel().getModuleByPackage(appPackage)
        if (module != null) {
            Log.i(
                TAG, ".\n" +
                        "GET MODULE PACKAGE:\n" +
                        "\t\tid: ${module.id}" +
                        "\t\tpackage: ${module.appPackage}" +
                        "\t\tsettings: ${module.appSettings}"
            )
        }
        return module
    }
}