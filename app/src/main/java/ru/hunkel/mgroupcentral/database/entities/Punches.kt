package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "punches"
//    foreignKeys = [
//        ForeignKey(
//            entity = Events::class,
//            parentColumns = ["id"],
//            childColumns = ["event_id"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ]
)

data class Punches(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    var eventId: Int,

    var time: Long = 0L,

    var controlPoint: Int = 0
)