package com.xdmpx.routesp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeID"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeID: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
)