package com.example.platisa.di

import android.content.Context
import com.example.platisa.core.data.network.GmailService
import com.example.platisa.core.data.repository.GmailRepositoryImpl
import com.example.platisa.core.domain.repository.GmailRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGmailService(@ApplicationContext context: Context): GmailService {
        return GmailService(context)
    }

    @Provides
    @Singleton
    fun provideGmailRepository(repository: GmailRepositoryImpl): GmailRepository {
        return repository
    }
}
