package com.gemwallet.android.data.service.store.database.entities

import androidx.room.Entity

@Entity(
    tableName = "fake_balances",
    primaryKeys = ["walletId", "assetId"]
)
data class DbFakeBalance(
    val walletId: String,
    val assetId: String,
    val adjustment: String, // 正数=增加余额，负数=扣减余额，单位是最小单位
    val isOverride: Boolean = false, // true=完全覆盖真实余额，false=在真实余额上加减
)
