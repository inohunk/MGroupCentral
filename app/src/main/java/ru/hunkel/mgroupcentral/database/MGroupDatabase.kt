package ru.hunkel.mgroupcentral.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.runBlocking
import ru.hunkel.mgroupcentral.database.dao.ModulesDao
import ru.hunkel.mgroupcentral.database.entities.Module

@Database(
    entities = [Module::class],
    version = 3
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
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE module ADD COLUMN appSettings TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE module ADD COLUMN appService TEXT NOT NULL DEFAULT ''")
    }
}