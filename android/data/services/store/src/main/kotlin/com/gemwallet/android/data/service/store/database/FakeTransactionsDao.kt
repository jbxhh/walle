package com.gemwallet.android.data.service.store.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gemwallet.android.data.service.store.database.entities.DbFakeTransaction

@Dao
interface FakeTransactionsDao {
    @Insert
    suspend fun insert(tx: DbFakeTransaction): Long

    @Query("SELECT * FROM fake_transactions WHERE walletId = :walletId ORDER BY timestamp DESC")
    suspend fun getByWallet(walletId: String): List<DbFakeTransaction>

    @Query("SELECT * FROM fake_transactions WHERE walletId = :walletId AND assetId = :assetId ORDER BY timestamp DESC")
    suspend fun getByAsset(walletId: String, assetId: String): List<DbFakeTransaction>

    @Query("DELETE FROM fake_transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM fake_transactions")
    suspend fun clearAll()
}
