package ru.hunkel.mgroupcentral.database.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "module")
data class Module(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    val appPackage: String,

    val appSettings: String = ""
)