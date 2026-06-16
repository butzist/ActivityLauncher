package de.szalkowski.activitylauncher.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.szalkowski.activitylauncher.data.external.*
import de.szalkowski.activitylauncher.domain.external.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    @Singleton
    @Binds
    abstract fun bindAdManager(
        playwireAdManagerImpl: PlaywireAdManagerImpl,
    ): AdManager

    @Singleton
    @Binds
    abstract fun bindAnalyticsLogger(
        analyticsLoggerImpl: AnalyticsLoggerImpl,
    ): AnalyticsLogger

    @Singleton
    @Binds
    abstract fun bindSupportReminder(
        supportReminderImpl: SupportReminderImpl,
    ): SupportReminder
}
