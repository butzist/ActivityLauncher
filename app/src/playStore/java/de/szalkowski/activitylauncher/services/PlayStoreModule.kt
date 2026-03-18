package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayStoreModule {
    @Singleton
    @Binds
    abstract fun bindInAppReviewService(
        inAppReviewServiceImpl: InAppReviewServiceImpl,
    ): InAppReviewService
}
