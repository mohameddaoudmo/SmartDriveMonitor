package com.example.smartdrivemonitor.di

import android.content.Context
import com.example.smartdrivemonitor.ml.*
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
    fun provideNormalizer(@ApplicationContext ctx: Context): SensorNormalizer =
        SensorNormalizer(ctx)

    @Provides @Singleton
    fun provideFeatureExtractor(): FeatureExtractor = FeatureExtractor()

    @Provides @Singleton
    fun provideMLEngine(
        @ApplicationContext ctx: Context,
        normalizer: SensorNormalizer
    ): MLInferenceEngine = MLInferenceEngine(ctx, normalizer)

    @Provides @Singleton
    fun provideBehaviorClassifier(
        engine: MLInferenceEngine,
        normalizer: SensorNormalizer,
        extractor: FeatureExtractor
    ): BehaviorClassifier = BehaviorClassifier(engine, normalizer, extractor)
}
