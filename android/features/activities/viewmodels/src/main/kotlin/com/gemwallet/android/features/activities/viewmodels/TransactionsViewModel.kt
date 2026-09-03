package com.gemwallet.android.features.activities.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemwallet.android.application.assets.cases.GetAssetById
import com.gemwallet.android.application.transactions.cases.GetTransactions
import com.gemwallet.android.application.transactions.cases.SyncTransactions
import com.gemwallet.android.application.transactions.cases.TransactionsRequestFilter
import com.gemwallet.android.application.session.cases.GetSession
import com.gemwallet.android.data.coordinators.FakeDataRepository
import com.gemwallet.android.data.coordinators.transaction.FakeTransactionDataAggregate
import com.gemwallet.android.ext.toIdentifier
import com.gemwallet.android.ui.models.TransactionTypeFilter
import com.wallet.core.primitives.AssetId
import com.wallet.core.primitives.Chain
import uniffi.gemstone.GemAssetConfigService
import com.wallet.core.primitives.WalletId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    getSession: GetSession,
    getTransactions: GetTransactions,
    private val syncTransactions: SyncTransactions,
    private val assetConfig: GemAssetConfigService,
    private val getAssetById: GetAssetById,
    private val fakeDataRepository: FakeDataRepository,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    val chainsFilter = MutableStateFlow<List<Chain>>(emptyList())
    val typeFilter = MutableStateFlow<List<TransactionTypeFilter>>(emptyList())
    val session = getSession()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, null)
    val walletId: StateFlow<WalletId?> = session
        .map { it?.wallet?.id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var syncedWalletId: WalletId? = null
    private val realTransactions = combine(
        chainsFilter,
        typeFilter,
    ) { chains, types ->
        buildList {
            addAll(TransactionsRequestFilter.activityDefaults(assetConfig))
            if (chains.isNotEmpty()) add(TransactionsRequestFilter.Chains(chains))
            val allowedTypes = types.flatMap { it.types }
            if (allowedTypes.isNotEmpty()) add(TransactionsRequestFilter.Types(allowedTypes))
        }
    }
    .flatMapLatest { filters -> getTransactions.getTransactions(filters) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = getTransactions.transactions().value,
    )
    val transactions = combine(
        realTransactions,
        session,
        fakeDataRepository.fakeMode,
        fakeDataRepository.dataVersion,
    ) { real, sess, _, _ ->
        if (!fakeDataRepository.isFakeDataVisible() || sess == null) {
            return@combine real
        }
        val walletId = sess.wallet.id.id
        val assetMap = real.associate { it.asset.id.toIdentifier() to it.asset }.toMutableMap()
        val fakeTxs = fakeDataRepository.getFakeTransactions(walletId)
        val fakeAggregates = fakeTxs.mapNotNull { fake ->
            val cached = assetMap[fake.assetId]
            val asset = cached ?: run {
                val id = runCatching { AssetId(fake.assetId) }.getOrNull()
                    ?: return@mapNotNull null
                val loaded = runCatching { getAssetById(id).first() }.getOrNull()
                if (loaded != null) {
                    assetMap[fake.assetId] = loaded
                }
                loaded
            }
            asset ?: return@mapNotNull null
            FakeTransactionDataAggregate(fake, asset)
        }
        (real + fakeAggregates).sortedByDescending { it.createdAt }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            session
                .filterNotNull()
                .distinctUntilChangedBy { it.wallet.id }
                .drop(1)
                .collect {
                    clearChainsFilter()
                    clearTypeFilter()
                }
        }
    }

    fun syncIfNeeded(): Job? {
        val current = walletId.value ?: return null
        if (current == syncedWalletId) return null
        syncedWalletId = current
        return viewModelScope.launch(Dispatchers.IO) {
            val synced = syncTransactions.syncTransactions()
            if (!synced && syncedWalletId == current) {
                syncedWalletId = null
            }
        }
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        _isRefreshing.update { true }
        try {
            syncTransactions.syncTransactions()
        } finally {
            _isRefreshing.update { false }
        }
    }

    fun applyChainsFilter(chains: List<Chain>) {
        chainsFilter.update { chains }
    }

    fun applyTypesFilter(types: List<TransactionTypeFilter>) {
        typeFilter.update { types }
    }

    fun clearChainsFilter() {
        chainsFilter.update {
            emptyList()
        }
    }

    fun clearTypeFilter() {
        typeFilter.update {
            emptyList()
        }
    }
}
