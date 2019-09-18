package ru.hunkel.mgroupcentral.managers

import android.content.Context
import android.util.Log
import ru.hunkel.mgroupcentral.database.MGroupDatabase
import ru.hunkel.mgroupcentral.database.dao.entities.Module

const val TAG = "MGroupDatabaseManager"

class MGroupDatabaseManager(context: Context) {

    private var mDatabase = MGroupDatabase.getInstance(context)

    fun actionAddModule(module: Module) {
        val insertedId = mDatabase.trackingModel().addModule(module)
        Log.i(
            TAG,
            ".\nADD MODULE:\n" +
                    "\t\tnew module id: $insertedId"
        )
    }

    fun actionGetAllModules(): List<Module> {
        return mDatabase.trackingModel().getAllModules()
    }
}