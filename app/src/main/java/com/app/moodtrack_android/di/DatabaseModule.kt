package com.app.moodtrack_android.di

import android.content.Context
import androidx.room.Room
import com.app.moodtrack_android.database.AppDatabase
import com.app.moodtrack_android.database.dao.FileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideFileDao(appDatabase: AppDatabase) : FileDao {
        return appDatabase.fileDao()
    }

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }
}