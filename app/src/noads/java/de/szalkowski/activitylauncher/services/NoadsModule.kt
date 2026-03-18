package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NoadsModule {
    @Singleton
    @Binds
    abstract fun bindAdmobService(
        admobServiceImpl: AdmobServiceImpl,
    ): AdmobService

    @Singleton
    @Binds
    abstract fun bindAnalyticsService(
        analyticsServiceImpl: AnalyticsServiceImpl,
    ): AnalyticsService

    @Singleton
    @Binds
    abstract fun bindPaidReminderService(
        paidReminderServiceImpl: PaidReminderServiceImpl,
    ): PaidReminderService
}
