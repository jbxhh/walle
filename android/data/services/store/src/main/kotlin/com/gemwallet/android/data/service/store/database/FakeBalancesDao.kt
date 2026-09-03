package com.gemwallet.android.data.service.store.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gemwallet.android.data.service.store.database.entities.DbFakeBalance

@Dao
interface FakeBalancesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balance: DbFakeBalance)

    @Query("SELECT * FROM fake_balances WHERE walletId = :walletId AND assetId = :assetId")
    suspend fun get(walletId: String, assetId: String): DbFakeBalance?

    @Query("SELECT * FROM fake_balances WHERE walletId = :walletId")
    suspend fun getByWallet(walletId: String): List<DbFakeBalance>

    @Query("DELETE FROM fake_balances WHERE walletId = :walletId AND assetId = :assetId")
    suspend fun delete(walletId: String, assetId: String)

    @Query("DELETE FROM fake_balances")
    suspend fun clearAll()
}
