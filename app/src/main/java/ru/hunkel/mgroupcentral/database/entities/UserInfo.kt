package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var eventId: Int,
    var firstName: String = "",
    var lastName: String = "",
    var number: String = ""
)