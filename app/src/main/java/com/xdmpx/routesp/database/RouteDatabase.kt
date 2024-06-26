package com.xdmpx.routesp.database

import android.content.Context
import androidx.room.*
import com.xdmpx.routesp.database.entities.Converters
import com.xdmpx.routesp.database.entities.KilometerPointEntity
import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.RouteEntity

@Database(
    version = 2,
    entities = [RouteEntity::class, PointEntity::class, KilometerPointEntity::class, PauseEntity::class],
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class RouteDatabase : RoomDatabase() {

    abstract val routeDatabaseDao: RouteDao

    companion object {

        @Volatile
        private var INSTANCE: RouteDatabase? = null

        fun getInstance(context: Context): RouteDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, RouteDatabase::class.java, "route_db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}