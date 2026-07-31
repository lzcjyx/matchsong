package matchsong.domain.usecase

import matchsong.domain.port.ConsentRepository

/**
 * M2.3-2 同意隐私说明用例（FR-ONB-2/3）。
 */
class AcceptConsentUseCase(
    private val consentRepository: ConsentRepository,
) {
    suspend operator fun invoke(version: String) {
        consentRepository.accept(version)
    }
}
