package com.xdmpx.routesp.database.entities

import androidx.room.TypeConverter
import java.util.Calendar
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {

        return value?.let {
            val date = Calendar.getInstance()
            date.timeInMillis = it
            date.time
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
