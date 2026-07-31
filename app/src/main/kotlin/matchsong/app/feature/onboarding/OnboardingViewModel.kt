package matchsong.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.domain.usecase.AcceptConsentUseCase
import javax.inject.Inject

/**
 * M2.3-1 Onboarding ViewModel：同意/不同意（FR-ONB-1/2）。
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val acceptConsentUseCase: AcceptConsentUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UiState>(UiState.Idle)
        val state: StateFlow<UiState> = _state.asStateFlow()

        sealed interface UiState {
            data object Idle : UiState

            data object Agreed : UiState
        }

        fun onAgree() {
            viewModelScope.launch {
                // 版本常量与 AppModule 的 PRIVACY_NOTICE_VERSION 同源
                acceptConsentUseCase(matchsong.app.di.PRIVACY_NOTICE_VERSION)
                _state.value = UiState.Agreed
            }
        }
    }
