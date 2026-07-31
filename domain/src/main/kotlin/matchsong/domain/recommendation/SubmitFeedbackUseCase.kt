package matchsong.domain.recommendation

import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackRepository

/**
 * M8.5-1 提交反馈用例（FR-HX-3）。
 *
 * 仅保存反馈记录，**不自动调整推荐权重**（PLAN M8.5 明确：MVP 只采集反馈，
 * 权重调优留待数据积累后人工/后续版本，本用例不含任何权重逻辑）。
 */
class SubmitFeedbackUseCase(
    private val feedbackRepository: FeedbackRepository,
) {
    suspend operator fun invoke(feedback: FeedbackItem) {
        feedbackRepository.submit(feedback)
    }
}
