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

