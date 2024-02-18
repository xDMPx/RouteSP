package com.xdmpx.routesp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)