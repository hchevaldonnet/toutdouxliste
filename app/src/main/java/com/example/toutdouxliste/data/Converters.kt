package com.example.toutdouxliste.data

import androidx.room.TypeConverter
import java.time.LocalDateTime

class Converters {

    @TypeConverter
    fun fromEtat(etat: Etat): String {
        return etat.name
    }

    @TypeConverter
    fun toEtat(value: String): Etat {
        return Etat.valueOf(value)
    }

    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime): String {
        return date.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value)
    }
}