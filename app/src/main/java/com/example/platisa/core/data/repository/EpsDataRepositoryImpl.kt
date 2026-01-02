package com.example.platisa.core.data.repository

import com.example.platisa.core.data.database.dao.EpsDao
import com.example.platisa.core.data.database.entity.EpsDataEntity
import com.example.platisa.core.data.mapper.toDomain
import com.example.platisa.core.domain.model.EpsData
import com.example.platisa.core.domain.repository.EpsDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EpsDataRepositoryImpl @Inject constructor(
    private val epsDao: EpsDao
) : EpsDataRepository {

    override fun getEpsDataByReceiptId(receiptId: Long): Flow<EpsData?> {
        return epsDao.getEpsDataForReceiptFlow(receiptId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun insertEpsData(epsData: EpsData, receiptId: Long) {
        val entity = EpsDataEntity(
            receiptId = receiptId,
            edNumber = epsData.edNumber,
            billingPeriod = epsData.billingPeriod,
            consumptionVt = epsData.consumptionVt?.toDouble(),
            consumptionNt = epsData.consumptionNt?.toDouble(),
            totalConsumption = epsData.totalConsumption?.toDouble()
        )
        epsDao.insertEpsData(entity)
    }

    override suspend fun updateEpsData(epsData: EpsData) {
        // Implement if needed in the future
    }

    override suspend fun deleteEpsData(receiptId: Long) {
        epsDao.deleteEpsDataForReceipt(receiptId)
    }
}
