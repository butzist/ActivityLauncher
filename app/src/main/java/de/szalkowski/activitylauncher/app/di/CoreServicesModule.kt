package de.szalkowski.activitylauncher.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.szalkowski.activitylauncher.data.external.ActivitySharerImpl
import de.szalkowski.activitylauncher.data.favorites.FavoritesRepositoryImpl
import de.szalkowski.activitylauncher.data.launcher.*
import de.szalkowski.activitylauncher.data.packages.*
import de.szalkowski.activitylauncher.data.recents.RecentsRepositoryImpl
import de.szalkowski.activitylauncher.data.settings.SettingsRepositoryImpl
import de.szalkowski.activitylauncher.data.system.RootDetectorImpl
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.packages.*
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.system.RootDetector
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreServicesModule {
    @Singleton
    @Binds
    abstract fun bindActivityLauncher(
        activityLauncherImpl: ActivityLauncherImpl,
    ): ActivityLauncher

    @Singleton
    @Binds
    abstract fun bindActivityRepository(
        activityRepositoryImpl: ActivityRepositoryImpl,
    ): ActivityRepository

    @Singleton
    @Binds
    abstract fun bindPackageRepository(
        packageRepositoryImpl: PackageRepositoryImpl,
    ): PackageRepository

    @Singleton
    @Binds
    abstract fun bindShortcutCreator(
        shortcutCreatorImpl: ShortcutCreatorImpl,
    ): ShortcutCreator

    @Singleton
    @Binds
    abstract fun bindIconLoader(
        iconLoaderImpl: IconLoaderImpl,
    ): IconLoader

    @Singleton
    @Binds
    abstract fun bindActivitySharer(
        activitySharerImpl: ActivitySharerImpl,
    ): ActivitySharer

    @Singleton
    @Binds
    abstract fun bindIntentSigner(
        intentSignerImpl: IntentSignerImpl,
    ): IntentSigner

    @Singleton
    @Binds
    abstract fun bindViewIntentParser(
        viewIntentParserImpl: ViewIntentParserImpl,
    ): ViewIntentParser

    @Singleton
    @Binds
    abstract fun bindRootDetector(
        rootDetectorImpl: RootDetectorImpl,
    ): RootDetector

    @Singleton
    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl,
    ): SettingsRepository

    @Singleton
    @Binds
    abstract fun bindFavoritesRepository(
        favoritesRepositoryImpl: FavoritesRepositoryImpl,
    ): FavoritesRepository

    @Singleton
    @Binds
    abstract fun bindRecentsRepository(
        recentsRepositoryImpl: RecentsRepositoryImpl,
    ): RecentsRepository
}
