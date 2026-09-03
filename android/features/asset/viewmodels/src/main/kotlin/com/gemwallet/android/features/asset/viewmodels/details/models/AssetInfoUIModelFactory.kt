package com.gemwallet.android.features.asset.viewmodels.details.models

import com.gemwallet.android.data.coordinators.FakeDataRepository
import com.gemwallet.android.domains.asset.chain
import com.gemwallet.android.domains.asset.getIconUrl
import com.gemwallet.android.domains.percentage.PercentageFormatterStyle
import com.gemwallet.android.domains.percentage.formatAsPercentage
import com.gemwallet.android.domains.price.toValueDirection
import com.gemwallet.android.ext.asset
import com.gemwallet.android.ext.toIdentifier
import com.gemwallet.android.ext.toPrimitives
import com.gemwallet.android.model.AssetInfo
import com.gemwallet.android.model.ChainAssetInfo
import com.gemwallet.android.model.CurrencyFormatter
import com.gemwallet.android.model.ValueFormatter
import com.gemwallet.android.model.getTotalAmount
import com.gemwallet.android.model.toGem
import com.wallet.core.primitives.Asset
import com.wallet.core.primitives.AssetType
import com.wallet.core.primitives.Currency
import com.wallet.core.primitives.VerificationStatus
import com.wallet.core.primitives.WalletType
import uniffi.gemstone.GemAssetNetworkDestination
import uniffi.gemstone.GemBalanceRow
import java.math.BigInteger
import javax.inject.Inject

class AssetInfoUIModelFactory @Inject constructor(
    private val fakeDataRepository: FakeDataRepository,
) {
    suspend fun create(
        chainAssetInfo: ChainAssetInfo,
        explorerName: String,
        walletType: WalletType,
        walletId: String,
        explorerAddressUrl: String?,
        explorerTokenUrl: String?,
    ): AssetInfoUIModel {
        val assetInfo = chainAssetInfo.assetInfo
        val feeAssetInfo = chainAssetInfo.feeAssetInfo
        val asset = assetInfo.asset
        val balances = assetInfo.balance
        val price = assetInfo.price?.price?.price ?: 0.0
        val currency = assetInfo.price?.currency ?: Currency.USD
        val currencyFormatter = CurrencyFormatter(currency = currency)
        val valueFormatter = ValueFormatter(style = ValueFormatter.Style.Auto)

        // 叠加自定义余额
        val baseBalance = balances.balance.getTotalAmount()
        val adjustment = if (fakeDataRepository.isFakeDataVisible()) {
            fakeDataRepository.getBalanceAdjustment(walletId, asset.id.toIdentifier()) ?: BigInteger.ZERO
        } else {
            BigInteger.ZERO
        }
        val totalBalance = baseBalance + adjustment
        val fiatTotal = if (balances.fiatTotalAmount == 0.0 && adjustment == BigInteger.ZERO) {
            ""
        } else {
            currencyFormatter.string(balances.fiatTotalAmount)
        }

        return AssetInfoUIModel(
            assetInfo = assetInfo,
            name = assetName(asset),
            iconUrl = asset.id.getIconUrl(),
            priceValue = if (price == 0.0) "" else currencyFormatter.string(price),
            priceDayChanges = assetInfo.price?.price?.priceChangePercentage24h.formatAsPercentage(),
            priceChangedType = assetInfo.price?.price?.priceChangePercentage24h.toValueDirection(),
            tokenType = asset.type,
            isBuyEnabled = assetInfo.metadata.isBuyEnabled,
            isSwapEnabled = assetInfo.metadata.isSwapEnabled,
            swapPayAssetId = null,
            swapReceiveAssetId = null,
            explorerName = explorerName,
            explorerAddressUrl = explorerAddressUrl,
            explorerTokenUrl = explorerTokenUrl,
            verificationStatus = null,
            networkDestination = null,
            shareUrl = "",
            accountInfoUIModel = AssetInfoUIModel.AccountInfoUIModel(
                walletType = walletType,
                totalBalance = valueFormatter.string(totalBalance, balances.asset),
                totalFiat = fiatTotal,
                owner = assetInfo.owner?.address ?: "",
                balances = balanceRows(assetInfo, valueFormatter),
                balanceMetadata = feeAssetInfo.balance.metadata,
            ),
        )
    }

    private fun assetName(asset: Asset): String =
        if (asset.type == AssetType.NATIVE) asset.id.chain.asset().name else asset.name

    private fun balanceRows(assetInfo: AssetInfo, formatter: ValueFormatter): List<AssetInfoUIModel.BalanceUIModel> {
        val asset = assetInfo.asset
        val text = { value: BigInteger -> formatter.string(value, asset) }
        return assetInfo.balance.toGem().detailRows(asset.chain.string, assetInfo.metadata.isStakeEnabled).mapNotNull { row ->
            when (row) {
                is GemBalanceRow.Available -> AssetInfoUIModel.BalanceUIModel(AssetInfoUIModel.BalanceViewType.Available, text(row.value))
                is GemBalanceRow.Staked -> AssetInfoUIModel.BalanceUIModel(
                    AssetInfoUIModel.BalanceViewType.Stake,
                    if (row.value == BigInteger.ZERO) {
                        "APR ${(assetInfo.metadata.stakingApr ?: 0.0).formatAsPercentage(style = PercentageFormatterStyle.PercentSignLess)}"
                    } else {
                        text(row.value)
                    },
                )
                is GemBalanceRow.PendingUnconfirmed -> AssetInfoUIModel.BalanceUIModel(AssetInfoUIModel.BalanceViewType.PendingUnconfirmed, text(row.value))
                is GemBalanceRow.Reserved -> AssetInfoUIModel.BalanceUIModel(AssetInfoUIModel.BalanceViewType.Reserved, text(row.value), row.url)
                is GemBalanceRow.Earn -> null
            }
        }
    }
}
