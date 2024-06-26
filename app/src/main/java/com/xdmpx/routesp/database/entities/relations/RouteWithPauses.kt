package com.xdmpx.routesp.database.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.database.entities.RouteEntity

data class RouteWithPauses(
    @Embedded val route: RouteEntity, @Relation(
        parentColumn = "id", entityColumn = "routeID"
    ) val pauses: List<PauseEntity>
)
