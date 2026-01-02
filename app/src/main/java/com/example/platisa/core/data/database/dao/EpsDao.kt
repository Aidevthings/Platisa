package com.example.platisa.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.platisa.core.data.database.entity.EpsDataEntity
import kotlinx.coroutines.flow.Flow

import java.math.BigDecimal
import java.util.Date

data class EpsAnalyticsEntity(
    val date: Date?,
    val totalAmount: BigDecimal,
    val consumptionVt: Double?,
    val consumptionNt: Double?
)

@Dao
interface EpsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpsData(epsData: EpsDataEntity)

    @Query("SELECT * FROM eps_data WHERE receiptId = :receiptId")
    suspend fun getEpsDataForReceipt(receiptId: Long): EpsDataEntity?

    @Query("SELECT * FROM eps_data WHERE receiptId = :receiptId LIMIT 1")
    fun getEpsDataForReceiptFlow(receiptId: Long): Flow<EpsDataEntity?>

    @Query("SELECT * FROM eps_data")
    fun getAllEpsData(): Flow<List<EpsDataEntity>>

    @Query("""
        SELECT r.date, r.totalAmount, e.consumptionVt, e.consumptionNt 
        FROM receipts r 
        INNER JOIN eps_data e ON r.id = e.receiptId 
        ORDER BY r.date ASC
    """)
    fun getEpsAnalyticsData(): Flow<List<EpsAnalyticsEntity>>

    @Query("DELETE FROM eps_data WHERE receiptId = :receiptId")
    suspend fun deleteEpsDataForReceipt(receiptId: Long)
    
    @Query("DELETE FROM eps_data")
    suspend fun deleteAllEpsData()
}
