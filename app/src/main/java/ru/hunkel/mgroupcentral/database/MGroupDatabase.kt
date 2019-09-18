package ru.hunkel.mgroupcentral.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking
import ru.hunkel.mgroupcentral.database.dao.ModulesDao
import ru.hunkel.mgroupcentral.database.dao.entities.Module

@Database(
    entities = [Module::class],
    version = 1
)
abstract class MGroupDatabase : RoomDatabase() {
    abstract fun trackingModel(): ModulesDao

    companion object {
        private var INSTANCE: MGroupDatabase? = null

        fun getInstance(context: Context): MGroupDatabase {
            if (INSTANCE == null) {
                runBlocking {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        MGroupDatabase::class.java,
                        "mgroup-database"
                    )
                        //TODO used only for primary testing. In future rewrite all database queries with coroutines
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}