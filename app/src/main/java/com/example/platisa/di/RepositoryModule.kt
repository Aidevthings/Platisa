package com.example.platisa.di

import com.example.platisa.core.data.repository.EpsDataRepositoryImpl
import com.example.platisa.core.data.repository.ReceiptRepositoryImpl
import com.example.platisa.core.domain.repository.EpsDataRepository
import com.example.platisa.core.domain.repository.ReceiptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(
        receiptRepositoryImpl: ReceiptRepositoryImpl
    ): ReceiptRepository

    @Binds
    @Singleton
    abstract fun bindEpsDataRepository(
        epsDataRepositoryImpl: EpsDataRepositoryImpl
    ): EpsDataRepository
}
