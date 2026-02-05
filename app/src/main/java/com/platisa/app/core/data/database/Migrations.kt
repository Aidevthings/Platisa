package com.platisa.app.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Dodaj Payment ID polja
        database.execSQL("ALTER TABLE receipts ADD COLUMN naplatniNumber TEXT")
        database.execSQL("ALTER TABLE receipts ADD COLUMN paymentId TEXT")
        database.execSQL("ALTER TABLE receipts ADD COLUMN isStorno INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE receipts ADD COLUMN isVisible INTEGER NOT NULL DEFAULT 1")
        
        // Dodaj index
        database.execSQL("CREATE INDEX IF NOT EXISTS index_receipts_paymentId ON receipts(paymentId)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Dodaj dueDate (rok plaćanja)
        database.execSQL("ALTER TABLE receipts ADD COLUMN dueDate INTEGER")
    }
}

// No-op migrations for version gaps (no schema changes)
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema changes in this version
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema changes in this version
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add Recipient Name and Address
        database.execSQL("ALTER TABLE receipts ADD COLUMN recipientName TEXT")
        database.execSQL("ALTER TABLE receipts ADD COLUMN recipientAddress TEXT")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add currentMonthAmount and previousDebtAmount for bill separation
        database.execSQL("ALTER TABLE receipts ADD COLUMN currentMonthAmount TEXT")
        database.execSQL("ALTER TABLE receipts ADD COLUMN previousDebtAmount TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No-op migration - schema already up to date, version bump to trigger destructive migration
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add sourceEmail column for proper Firestore sync
        database.execSQL("ALTER TABLE receipts ADD COLUMN sourceEmail TEXT DEFAULT NULL")
        
        // Backfill: Extract email from metadata where present (SOURCE_EMAIL:user@gmail.com)
        database.execSQL("""
            UPDATE receipts 
            SET sourceEmail = SUBSTR(metadata, INSTR(metadata, 'SOURCE_EMAIL:') + 13)
            WHERE metadata LIKE '%SOURCE_EMAIL:%'
        """)
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add Payer Name and Address (for property identification)
        database.execSQL("ALTER TABLE receipts ADD COLUMN payerName TEXT")
        database.execSQL("ALTER TABLE receipts ADD COLUMN payerAddress TEXT")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema changes from 15 to 16 were explicitly tracked, 
        // but version was bumped. Keeping as no-op.
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add anomaly detection fields
        database.execSQL("ALTER TABLE receipts ADD COLUMN isAnomalyConfirmed INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE receipts ADD COLUMN anomalyType TEXT")
        database.execSQL("ALTER TABLE receipts ADD COLUMN anomalyMessage TEXT")
    }
}

