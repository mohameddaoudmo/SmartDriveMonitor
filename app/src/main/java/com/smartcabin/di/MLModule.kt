package com.smartcabin.di

import android.content.Context
import com.smartcabin.ml.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides @Singleton
    fun provideSmartDriveProcessor(@ApplicationContext ctx: Context): SmartDriveProcessor =
        SmartDriveProcessor(ctx)

    @Provides @Singleton
    fun provideMLEngine(
        @ApplicationContext ctx: Context,
        processor: SmartDriveProcessor
    ): MLInferenceEngine = MLInferenceEngine(ctx, processor)

    @Provides @Singleton
    fun provideBehaviorClassifier(
        engine: MLInferenceEngine
    ): BehaviorClassifier = BehaviorClassifier(engine)
}
