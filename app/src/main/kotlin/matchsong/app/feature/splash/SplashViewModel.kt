package matchsong.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.domain.usecase.GetOnboardingStatusUseCase
import javax.inject.Inject

/**
 * M2.3-2 启动分流：根据同意状态决定去向（ACC-1）。
 */
@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val getOnboardingStatusUseCase: GetOnboardingStatusUseCase,
    ) : ViewModel() {
        private val _destination = MutableStateFlow<Destination?>(null)
        val destination: StateFlow<Destination?> = _destination.asStateFlow()

        enum class Destination { ONBOARDING, HOME }

        init {
            viewModelScope.launch {
                _destination.value =
                    when (getOnboardingStatusUseCase()) {
                        GetOnboardingStatusUseCase.OnboardingStatus.COMPLETED -> Destination.HOME
                        GetOnboardingStatusUseCase.OnboardingStatus.NEEDED -> Destination.ONBOARDING
                    }
            }
        }
    }
