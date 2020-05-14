package database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "ControlPoints"
//    foreignKeys = [
//        ForeignKey(
//            entity = Events::class,
//            parentColumns = ["id"],
//            childColumns = ["eventId"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ]
)
data class ControlPoints(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    @ColumnInfo(name = "eventId")
    var eventId: Int,

    var controlPointNumber: Int,
    val rssi: Int,
    var time: Long
)