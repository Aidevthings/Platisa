package com.platisa.app.di

import android.content.Context
import androidx.room.Room
import com.platisa.app.core.data.database.MIGRATION_6_7
import com.platisa.app.core.data.database.MIGRATION_7_8
import com.platisa.app.core.data.database.MIGRATION_8_9
import com.platisa.app.core.data.database.MIGRATION_9_10
import com.platisa.app.core.data.database.MIGRATION_10_11
import com.platisa.app.core.data.database.PlatisaDatabase
import com.platisa.app.core.data.database.dao.EpsDao
import com.platisa.app.core.data.database.dao.ReceiptDao
import com.platisa.app.core.data.database.dao.SectionDao
import com.platisa.app.core.data.database.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePlatisaDatabase(@ApplicationContext context: Context): PlatisaDatabase {
        return Room.databaseBuilder(
            context,
            PlatisaDatabase::class.java,
            "platisa_db"
        )
        .addMigrations(
            MIGRATION_6_7, 
            MIGRATION_7_8, 
            MIGRATION_8_9,  // No-op migration
            MIGRATION_9_10, // No-op migration
            MIGRATION_10_11
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideReceiptDao(db: PlatisaDatabase): ReceiptDao = db.receiptDao()

    @Provides
    @Singleton
    fun provideSectionDao(db: PlatisaDatabase): SectionDao = db.sectionDao()

    @Provides
    @Singleton
    fun provideTagDao(db: PlatisaDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideEpsDao(db: PlatisaDatabase): EpsDao = db.epsDao()
}

