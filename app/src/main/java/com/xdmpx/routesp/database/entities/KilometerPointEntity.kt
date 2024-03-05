package com.xdmpx.routesp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date


@Entity(
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeID"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class KilometerPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val routeID: Int,
    val date: Date
)