package de.szalkowski.activitylauncher.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.szalkowski.activitylauncher.data.external.ReviewRequesterImplStub
import de.szalkowski.activitylauncher.domain.external.ReviewRequester
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OssModule {
    @Singleton
    @Binds
    abstract fun bindReviewRequester(
        reviewRequesterImplStub: ReviewRequesterImplStub,
    ): ReviewRequester
}
