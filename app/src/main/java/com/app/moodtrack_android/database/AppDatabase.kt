package com.app.moodtrack_android.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.moodtrack_android.database.dao.FileDao
import com.app.moodtrack_android.model.StoredFile

@Database(entities = [StoredFile::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
}