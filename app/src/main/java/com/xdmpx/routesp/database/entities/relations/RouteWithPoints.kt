package com.xdmpx.routesp.database.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.RouteEntity

data class RouteWithPoints(
    @Embedded val route: RouteEntity, @Relation(
        parentColumn = "id", entityColumn = "routeID"
    ) val points: List<PointEntity>
)