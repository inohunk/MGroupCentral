package ru.hunkel.mgroupcentral.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import database.entities.*
import kotlinx.coroutines.runBlocking
import ru.hunkel.mgroupcentral.database.dao.TrackingDao

@Database(
    entities = [ControlPoints::class, Events::class, Punches::class, UserInfo::class, DeviceInfo::class],
    version = 7
)

abstract class MGroupDatabase : RoomDatabase() {
    abstract fun trackingModel(): TrackingDao

    companion object {
        private var INSTANCE: MGroupDatabase? = null
        //TODO ADD MIGRATIONS

        fun getInstance(context: Context): MGroupDatabase {
            if (INSTANCE == null) {
                runBlocking {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        MGroupDatabase::class.java,
                        "tracking-database"
                    )
                        //TODO used only for primary testing. In future rewrite all database queries with coroutines
                        .allowMainThreadQueries()
//                      .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}