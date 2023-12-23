package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class ServicesModule {
    @Binds
    abstract fun bindActivityLauncherService(
        activityLauncherServiceImpl: ActivityLauncherServiceImpl
    ): ActivityLauncherService

    @Binds
    abstract fun bindActivityListService(
        activityListServiceImpl: ActivityListServiceImpl
    ): ActivityListService

    @Binds
    abstract fun bindPackageListService(
        packageListServiceImpl: PackageListServiceImpl
    ): PackageListService

    @Binds
    abstract fun bindIconCreatorService(
        iconCreatorServiceImpl: IconCreatorServiceImpl
    ): IconCreatorService
}