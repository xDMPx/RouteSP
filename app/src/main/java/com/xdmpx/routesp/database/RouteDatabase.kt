package com.xdmpx.routesp.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import com.xdmpx.routesp.database.entities.Converters
import com.xdmpx.routesp.database.entities.KilometerPointEntity
import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.RouteEntity

@Database(
    version = 3,
    entities = [RouteEntity::class, PointEntity::class, KilometerPointEntity::class, PauseEntity::class],
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(
        from = 2, to = 3, spec = RouteDatabase.DeleteAccuracyMigration::class
    )]
)

@TypeConverters(Converters::class)
abstract class RouteDatabase : RoomDatabase() {


    @DeleteColumn(
        tableName = "PointEntity", columnName = "accuracy"
    )
    class DeleteAccuracyMigration : AutoMigrationSpec

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

