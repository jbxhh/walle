package com.gemwallet.android.ui.components.list_item

import androidx.annotation.StringRes
import com.gemwallet.android.ui.R
import com.wallet.core.primitives.WalletType

@get:StringRes
val WalletType.descriptionRes: Int get() = when (this) {
    WalletType.Multicoin, WalletType.Single -> R.string.common_secret_phrase
    WalletType.PrivateKey -> R.string.common_private_key
    // 保持为“地址”。若想彻底伪装成真实钱包的“助记词”，可将此处改为 R.string.common_secret_phrase
    WalletType.View -> R.string.common_address 
}

fun WalletType.supportIcon(): String? = when (this) {
    // 删除了 View 的 watch_badge 分支，使其统一返回 null，彻底移除“观察”小图标
    else -> null
}
