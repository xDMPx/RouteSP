package com.xdmpx.routesp.database

import androidx.room.*
import com.xdmpx.routesp.database.entities.KilometerPointEntity
import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.RouteEntity
import com.xdmpx.routesp.database.entities.relations.RouteWithKilometerPoints
import com.xdmpx.routesp.database.entities.relations.RouteWithPauses
import com.xdmpx.routesp.database.entities.relations.RouteWithPoints

@Dao
interface RouteDao {

    @Insert
    suspend fun insertRoute(route: RouteEntity)

    @Insert
    suspend fun insertPoint(point: PointEntity)

    @Insert
    suspend fun insertPause(pause: PauseEntity)

    @Insert
    suspend fun insertKilometerPoint(kPoint: KilometerPointEntity)

    @Query("SELECT * FROM RouteEntity")
    suspend fun getRoutes(): List<RouteEntity>

    @Query("SELECT * FROM PauseEntity WHERE routeID = :routeID")
    suspend fun getPauses(routeID: Int): List<PauseEntity>

    @Query("SELECT id FROM RouteEntity ORDER BY id DESC LIMIT 1")
    suspend fun getLastRouteID(): Int?

    @Transaction
    @Query("SELECT * FROM RouteEntity WHERE id = :id")
    suspend fun getRouteWithPoints(id: Int): RouteWithPoints?

    @Transaction
    @Query("SELECT * FROM RouteEntity WHERE  id = :id")
    suspend fun getRouteWithKilometerPoints(id: Int): RouteWithKilometerPoints?

    @Transaction
    @Query("SELECT * FROM RouteEntity WHERE id = :id")
    suspend fun getRouteWithPauses(id: Int): RouteWithPauses?

    @Query("SELECT * FROM RouteEntity WHERE id = :id")
    suspend fun getRouteByID(id: Int): RouteEntity?

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

}