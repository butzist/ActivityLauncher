package de.szalkowski.activitylauncher.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.szalkowski.activitylauncher.database.AppDatabase
import de.szalkowski.activitylauncher.database.PackageDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "activity_launcher.db",
        ).build()
    }

    @Provides
    fun providePackageDao(database: AppDatabase): PackageDao {
        return database.packageDao()
    }
}
