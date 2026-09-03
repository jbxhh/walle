package com.gemwallet.android.features.settings.security.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemwallet.android.data.coordinators.FakeDataRepository
import com.gemwallet.android.data.coordinators.FakeMode
import com.gemwallet.android.data.services.gemstone.config.UserConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val userConfig: UserConfig,
    private val fakeDataRepository: FakeDataRepository,
) : ViewModel() {

    val isHideBalances = userConfig.isHideBalances()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lockInterval = userConfig.getLockInterval()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val fakeMode = fakeDataRepository.fakeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, FakeMode.REAL)

    fun authRequired(): Boolean {
        return userConfig.authRequired()
    }

    fun setAuthRequired(required: Boolean) {
        userConfig.setAuthRequired(required)
    }

    fun setLockInterval(minutes: Int) = viewModelScope.launch(Dispatchers.IO) {
        userConfig.setLockInterval(minutes)
    }

    fun setHideBalances() {
        viewModelScope.launch(Dispatchers.IO) {
            userConfig.hideBalances()
        }
    }

    fun setFakeMode(mode: FakeMode) {
        fakeDataRepository.setFakeMode(mode)
    }
}
