package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
abstract class ServicesModule {
    @ActivityScoped
    @Binds
    abstract fun bindActivityLauncherService(
        activityLauncherServiceImpl: ActivityLauncherServiceImpl
    ): ActivityLauncherService

    @ActivityScoped
    @Binds
    abstract fun bindActivityListService(
        activityListServiceImpl: ActivityListServiceImpl
    ): ActivityListService

    @ActivityScoped
    @Binds
    abstract fun bindPackageListService(
        packageListServiceImpl: PackageListServiceImpl
    ): PackageListService

    @ActivityScoped
    @Binds
    abstract fun bindIconCreatorService(
        iconCreatorServiceImpl: IconCreatorServiceImpl
    ): IconCreatorService

    @ActivityScoped
    @Binds
    abstract fun bindIconLoaderService(
        iconLoaderServiceImpl: IconLoaderServiceImpl
    ): IconLoaderService

    @ActivityScoped
    @Binds
    abstract fun bindShareActivityService(
        shareActivityServiceImpl: ShareActivityServiceImpl
    ): ShareActivityService
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationServicesModule {
    @Singleton
    @Binds
    abstract fun bindIntentSigningService(
        intentSigningServiceImpl: IntentSigningServiceImpl
    ): IntentSigningService

    @Singleton
    @Binds
    abstract fun bindViewIntentParserService(
        viewIntentParserServiceImpl: ViewIntentParserServiceImpl
    ): ViewIntentParserService

    @Singleton
    @Binds
    abstract fun bindRootDetectionService(
        rootDetectionServiceImpl: RootDetectionServiceImpl
    ): RootDetectionService

    @Singleton
    @Binds
    abstract fun bindSettingsService(
        settingsServiceImpl: SettingsServiceImpl
    ): SettingsService

    @Singleton
    @Binds
    abstract fun bindInAppReviewService(
        inAppReviewServiceImpl: InAppReviewServiceImpl
    ): InAppReviewService

    @Singleton
    @Binds
    abstract fun bindFavoritesService(
        favoritesServiceImpl: FavoritesServiceImpl
    ): FavoritesService

    @Singleton
    @Binds
    abstract fun bindRecentActivitiesService(
        recentActivitiesServiceImpl: RecentActivitiesServiceImpl
    ): RecentActivitiesService
}
