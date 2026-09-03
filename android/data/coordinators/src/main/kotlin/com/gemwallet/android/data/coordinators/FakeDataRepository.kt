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

enum class FakeMode {
    REAL,
    CUSTOM,
    MERGED,
}

@Singleton
class FakeDataRepository @Inject constructor(
    private val fakeBalancesDao: FakeBalancesDao,
    private val fakeTransactionsDao: FakeTransactionsDao,
) {
    private val _fakeMode = MutableStateFlow(FakeMode.REAL)
    val fakeMode: StateFlow<FakeMode> = _fakeMode

    fun setFakeMode(mode: FakeMode) {
        _fakeMode.value = mode
    }

    fun isFakeDataVisible(): Boolean {
        return _fakeMode.value != FakeMode.REAL
    }

    fun isRealDataVisible(): Boolean {
        return _fakeMode.value != FakeMode.CUSTOM
    }

    suspend fun getBalanceAdjustment(walletId: String, assetId: String): BigInteger? {
        val fake = fakeBalancesDao.get(walletId, assetId) ?: return null
        return fake.adjustment.toBigIntegerOrNull()
    }

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

    suspend fun clearBalanceAdjustment(walletId: String, assetId: String) {
        fakeBalancesDao.delete(walletId, assetId)
    }

    suspend fun getFakeTransactions(walletId: String, assetId: String? = null): List<DbFakeTransaction> {
        return if (assetId != null) {
            fakeTransactionsDao.getByAsset(walletId, assetId)
        } else {
            fakeTransactionsDao.getByWallet(walletId)
        }
    }

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
        val current = getBalanceAdjustment(walletId, assetId) ?: BigInteger.ZERO
        setBalanceAdjustment(walletId, assetId, current - amount)
        return id
    }

    suspend fun deleteFakeTransaction(id: Long) {
        fakeTransactionsDao.delete(id)
    }

    suspend fun clearAll() {
        fakeBalancesDao.clearAll()
        fakeTransactionsDao.clearAll()
    }
}
