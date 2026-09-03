package com.gemwallet.android.ui.components.list_item

import androidx.annotation.StringRes
import com.gemwallet.android.ui.R
import com.wallet.core.primitives.WalletType

@get:StringRes
val WalletType.descriptionRes: Int get() = when (this) {
    // 将 View 也归入 secret_phrase，这样列表里所有钱包都显示“助记词”，彻底伪装
    WalletType.Multicoin, WalletType.Single, WalletType.View -> R.string.common_secret_phrase
    WalletType.PrivateKey -> R.string.common_private_key
}

fun WalletType.supportIcon(): String? = when (this) {
    // 彻底移除“观察”小图标
    else -> null
}
