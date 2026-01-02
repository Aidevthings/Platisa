package com.platisa.app.core.data.database.entity

import androidx.room.Entity

@Entity(primaryKeys = ["receiptId", "tagId"])
data class ReceiptTagCrossRef(
    val receiptId: Long,
    val tagId: Long
)

