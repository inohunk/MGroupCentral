package ru.hunkel.mgroupcentral.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.hunkel.mgroupcentral.database.entities.Module

@Dao
interface ModulesDao {

    //Modules
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun addModule(module: Module): Long

    @Query("SELECT * FROM module")
    fun getAllModules(): List<Module>

    @Query("SELECT * FROM module WHERE appPackage=:appPackage")
    fun getModuleByPackage(appPackage: String): Module?
}