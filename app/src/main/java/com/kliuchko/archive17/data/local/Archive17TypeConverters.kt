package com.kliuchko.archive17.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kliuchko.archive17.domain.model.ReadingStatus

class Archive17TypeConverters {
    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return gson.fromJson(value, stringListType)
    }

    @TypeConverter
    fun fromReadingStatus(value: ReadingStatus): String = value.name

    @TypeConverter
    fun toReadingStatus(value: String): ReadingStatus = ReadingStatus.valueOf(value)
}
