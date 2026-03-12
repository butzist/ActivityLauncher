package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreServicesModule {
    @Singleton
    @Binds
    abstract fun bindActivityLauncherService(
        activityLauncherServiceImpl: ActivityLauncherServiceImpl,
    ): ActivityLauncherService

    @Singleton
    @Binds
    abstract fun bindActivityListService(
        activityListServiceImpl: ActivityListServiceImpl,
    ): ActivityListService

    @Singleton
    @Binds
    abstract fun bindPackageListService(
        packageListServiceImpl: PackageListServiceImpl,
    ): PackageListService

    @Singleton
    @Binds
    abstract fun bindIconCreatorService(
        iconCreatorServiceImpl: IconCreatorServiceImpl,
    ): IconCreatorService

    @Singleton
    @Binds
    abstract fun bindIconLoaderService(
        iconLoaderServiceImpl: IconLoaderServiceImpl,
    ): IconLoaderService

    @Singleton
    @Binds
    abstract fun bindShareActivityService(
        shareActivityServiceImpl: ShareActivityServiceImpl,
    ): ShareActivityService

    @Singleton
    @Binds
    abstract fun bindIntentSigningService(
        intentSigningServiceImpl: IntentSigningServiceImpl,
    ): IntentSigningService

    @Singleton
    @Binds
    abstract fun bindViewIntentParserService(
        viewIntentParserServiceImpl: ViewIntentParserServiceImpl,
    ): ViewIntentParserService

    @Singleton
    @Binds
    abstract fun bindRootDetectionService(
        rootDetectionServiceImpl: RootDetectionServiceImpl,
    ): RootDetectionService

    @Singleton
    @Binds
    abstract fun bindSettingsService(
        settingsServiceImpl: SettingsServiceImpl,
    ): SettingsService

    @Singleton
    @Binds
    abstract fun bindFavoritesService(
        favoritesServiceImpl: FavoritesServiceImpl,
    ): FavoritesService

    @Singleton
    @Binds
    abstract fun bindRecentActivitiesService(
        recentActivitiesServiceImpl: RecentActivitiesServiceImpl,
    ): RecentActivitiesService
}
