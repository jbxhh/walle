package com.gemwallet.android.data.service.store.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fake_transactions")
data class DbFakeTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: String,
    val assetId: String,
    val fromAddress: String,
    val toAddress: String,
    val amount: String,
    val fee: String = "0",
    val memo: String = "",
    val timestamp: Long,
    val status: String = "confirmed", // confirmed / pending / failed
    val type: String = "transfer", // transfer / swap / receive
    val swapFromAssetId: String? = null,
    val swapFromAmount: String? = null,
)
