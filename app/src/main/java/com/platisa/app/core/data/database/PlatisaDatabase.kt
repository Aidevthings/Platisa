package com.platisa.app.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.platisa.app.core.data.database.dao.EpsDao
import com.platisa.app.core.data.database.dao.ReceiptDao
import com.platisa.app.core.data.database.dao.SectionDao
import com.platisa.app.core.data.database.dao.TagDao
import com.platisa.app.core.data.database.entity.EpsDataEntity
import com.platisa.app.core.data.database.entity.ReceiptEntity
import com.platisa.app.core.data.database.entity.ReceiptTagCrossRef
import com.platisa.app.core.data.database.entity.SectionEntity
import com.platisa.app.core.data.database.entity.TagEntity

@Database(
    entities = [
        ReceiptEntity::class,
        SectionEntity::class,
        TagEntity::class,
        ReceiptTagCrossRef::class,
        EpsDataEntity::class,
        com.platisa.app.core.data.database.entity.ReceiptItemEntity::class
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

