package com.platisa.app.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "eps_data",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["receiptId"])]
)
data class EpsDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val edNumber: String?, // ED Broj
    val billingPeriod: String?, // Obracunski period
    val consumptionVt: Double?, // Visa tarifa
    val consumptionNt: Double?, // Niza tarifa
    val totalConsumption: Double? // Ukupno kWh
)

