package database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "DeviceInfo",
    foreignKeys = [
        ForeignKey(
            entity = Events::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DeviceInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var eventId: Int = -1,
    var apiLevel: Int,
    var startBatteryLevel: Int,
    var endBatteryLevel: Int,
    var model: String,
    var manufacturer: String
)