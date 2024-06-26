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
data class PauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeID: Int,
    val pauseStart: Date,
    val pauseEnd: Date
)