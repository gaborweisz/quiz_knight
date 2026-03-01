package com.trainig.quiz_knight.di

import com.google.gson.Gson
import com.trainig.quiz_knight.data.map.MapGraphProviderImpl
import com.trainig.quiz_knight.data.repository.GameStateRepositoryImpl
import com.trainig.quiz_knight.data.repository.QuestionRepositoryImpl
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import com.trainig.quiz_knight.domain.repository.QuestionRepository
import com.trainig.quiz_knight.domain.usecase.MapGraphProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindQuestionRepository(impl: QuestionRepositoryImpl): QuestionRepository

    @Binds @Singleton
    abstract fun bindGameStateRepository(impl: GameStateRepositoryImpl): GameStateRepository

    @Binds @Singleton
    abstract fun bindMapGraphProvider(impl: MapGraphProviderImpl): MapGraphProvider

    companion object {
        @Provides @Singleton
        fun provideGson(): Gson = Gson()
    }
}

