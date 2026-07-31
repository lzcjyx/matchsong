package matchsong.domain.usecase

import matchsong.domain.port.ConsentRepository

/**
 * M2.3-2 启动分流用例（FR-ONB-3）：返回当前隐私说明版本是否已同意。
 *
 * 版本不一致 → 视为未同意，重新展示 Onboarding（SPEC §10.6）。
 */
class GetOnboardingStatusUseCase(
    private val consentRepository: ConsentRepository,
    private val currentPrivacyVersion: String,
) {
    suspend operator fun invoke(): OnboardingStatus =
        if (consentRepository.isAccepted(currentPrivacyVersion)) {
            OnboardingStatus.COMPLETED
        } else {
            OnboardingStatus.NEEDED
        }

    enum class OnboardingStatus { COMPLETED, NEEDED }
}
