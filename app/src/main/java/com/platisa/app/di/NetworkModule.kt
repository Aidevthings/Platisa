package com.platisa.app.di

import android.content.Context
import com.platisa.app.core.data.network.GmailService
import com.platisa.app.core.data.repository.GmailRepositoryImpl
import com.platisa.app.core.domain.repository.GmailRepository
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

