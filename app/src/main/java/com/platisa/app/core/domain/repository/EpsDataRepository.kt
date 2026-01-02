package com.platisa.app.core.domain.repository

import com.platisa.app.core.domain.model.EpsData
import kotlinx.coroutines.flow.Flow

interface EpsDataRepository {
    fun getEpsDataByReceiptId(receiptId: Long): Flow<EpsData?>
    suspend fun insertEpsData(epsData: EpsData, receiptId: Long)
    suspend fun updateEpsData(epsData: EpsData)
    suspend fun deleteEpsData(receiptId: Long)
}

