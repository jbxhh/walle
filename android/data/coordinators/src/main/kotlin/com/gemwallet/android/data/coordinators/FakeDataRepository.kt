package com.gemwallet.android.data.coordinators

import com.gemwallet.android.data.service.store.database.FakeBalancesDao
import com.gemwallet.android.data.service.store.database.FakeTransactionsDao
import com.gemwallet.android.data.service.store.database.entities.DbFakeBalance
import com.gemwallet.android.data.service.store.database.entities.DbFakeTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDataRepository @Inject constructor(
    private val fakeBalancesDao: FakeBalancesDao,
    private val fakeTransactionsDao: FakeTransactionsDao,
) {
    // 模式开关：是否启用假数据模式
    private val _fakeModeEnabled = MutableStateFlow(false)
    val fakeModeEnabled: StateFlow<Boolean> = _fakeModeEnabled

    fun setFakeModeEnabled(enabled: Boolean) {
        _fakeModeEnabled.value = enabled
    }

    // 获取自定义余额调整值（返回 null 表示没有自定义）
    suspend fun getBalanceAdjustment(walletId: String, assetId: String): BigInteger? {
        val fake = fakeBalancesDao.get(walletId, assetId) ?: return null
        return fake.adjustment.toBigIntegerOrNull()
    }

    // 设置自定义余额调整值
    suspend fun setBalanceAdjustment(walletId: String, assetId: String, adjustment: BigInteger, isOverride: Boolean = false) {
        fakeBalancesDao.upsert(
            DbFakeBalance(
                walletId = walletId,
                assetId = assetId,
                adjustment = adjustment.toString(),
                isOverride = isOverride,
            )
        )
    }

    // 清除某个资产的自定义余额
    suspend fun clearBalanceAdjustment(walletId: String, assetId: String) {
        fakeBalancesDao.delete(walletId, assetId)
    }

    // 获取假交易记录
    suspend fun getFakeTransactions(walletId: String, assetId: String? = null): List<DbFakeTransaction> {
        return if (assetId != null) {
            fakeTransactionsDao.getByAsset(walletId, assetId)
        } else {
            fakeTransactionsDao.getByWallet(walletId)
        }
    }

    // 添加一条假转账记录
    suspend fun addFakeTransfer(
        walletId: String,
        assetId: String,
        fromAddress: String,
        toAddress: String,
        amount: BigInteger,
        fee: BigInteger = BigInteger.ZERO,
        memo: String = "",
    ): Long {
        val tx = DbFakeTransaction(
            walletId = walletId,
            assetId = assetId,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amount = amount.toString(),
            fee = fee.toString(),
            memo = memo,
            timestamp = System.currentTimeMillis() / 1000,
            status = "confirmed",
            type = "transfer",
        )
        val id = fakeTransactionsDao.insert(tx)
        // 自动扣减余额
        val current = getBalanceAdjustment(walletId, assetId) ?: BigInteger.ZERO
        setBalanceAdjustment(walletId, assetId, current - amount)
        return id
    }

    // 删除假记录
    suspend fun deleteFakeTransaction(id: Long) {
        fakeTransactionsDao.delete(id)
    }

    // 清空所有假数据
    suspend fun clearAll() {
        fakeBalancesDao.clearAll()
        fakeTransactionsDao.clearAll()
    }
}
