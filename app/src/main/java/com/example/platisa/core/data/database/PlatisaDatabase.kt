package com.example.platisa.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.platisa.core.data.database.dao.EpsDao
import com.example.platisa.core.data.database.dao.ReceiptDao
import com.example.platisa.core.data.database.dao.SectionDao
import com.example.platisa.core.data.database.dao.TagDao
import com.example.platisa.core.data.database.entity.EpsDataEntity
import com.example.platisa.core.data.database.entity.ReceiptEntity
import com.example.platisa.core.data.database.entity.ReceiptTagCrossRef
import com.example.platisa.core.data.database.entity.SectionEntity
import com.example.platisa.core.data.database.entity.TagEntity

@Database(
    entities = [
        ReceiptEntity::class,
        SectionEntity::class,
        TagEntity::class,
        ReceiptTagCrossRef::class,
        EpsDataEntity::class,
        com.example.platisa.core.data.database.entity.ReceiptItemEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlatisaDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun sectionDao(): SectionDao
    abstract fun tagDao(): TagDao
    abstract fun epsDao(): EpsDao
}
