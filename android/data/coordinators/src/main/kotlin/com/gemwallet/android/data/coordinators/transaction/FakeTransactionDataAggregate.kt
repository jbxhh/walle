package com.gemwallet.android.data.coordinators.transaction

import com.gemwallet.android.data.service.store.database.entities.DbFakeTransaction
import com.gemwallet.android.domains.transaction.aggregates.TransactionDataAggregate
import com.gemwallet.android.model.ValueFormatter
import com.wallet.core.primitives.Asset
import com.wallet.core.primitives.TransactionDirection
import com.wallet.core.primitives.TransactionId
import com.wallet.core.primitives.TransactionState
import com.wallet.core.primitives.TransactionType
import uniffi.gemstone.GemAmountSign
import uniffi.gemstone.GemTransactionSubtitle
import uniffi.gemstone.GemTransactionTitle
import java.math.BigInteger

class FakeTransactionDataAggregate(
    private val fake: DbFakeTransaction,
    private val _asset: Asset,
) : TransactionDataAggregate {

    private val amount = fake.amount.toBigIntegerOrNull() ?: BigInteger.ZERO
    private val formatter = ValueFormatter(style = ValueFormatter.Style.Short)

    override val id: TransactionId = TransactionId(fake.id.toString())
    override val asset: Asset = _asset
    override val address: String = fake.toAddress
    override val addressName: String? = null
    override val value: String = formatter.string(amount, _asset)
    override val equivalentValue: String? = null
    override val title: GemTransactionTitle = GemTransactionTitle.Transfer
    override val subtitle: GemTransactionSubtitle = GemTransactionSubtitle.ToAddress(fake.toAddress)
    override val valueSign: GemAmountSign = GemAmountSign.OUTGOING
    override val type: TransactionType = TransactionType.Transfer
    override val direction: TransactionDirection = TransactionDirection.Outgoing
    override val pnl: Double? = null
    override val state: TransactionState = TransactionState.Confirmed
    override val nftImageUrl: String? = null
    override val createdAt: Long = fake.timestamp
}
