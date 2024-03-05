package com.xdmpx.routesp.database.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.xdmpx.routesp.database.entities.KilometerPointEntity
import com.xdmpx.routesp.database.entities.RouteEntity

data class RouteWithKilometerPoints(
    @Embedded val route: RouteEntity, @Relation(
        parentColumn = "id", entityColumn = "routeID"
    ) val points: List<KilometerPointEntity>
)