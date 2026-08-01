package matchsong.app.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.core.common.time.Clock
import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackType
import matchsong.domain.recommendation.SubmitFeedbackUseCase
import java.util.UUID
import javax.inject.Inject

/**
 * M10.6 推荐详情页 ViewModel（BUG-001：反馈 UI 接线，FR-HX-3）。
 *
 * 收集六类反馈（适合唱/太高/太低/太难/不喜欢风格/理由不准确），
 * 仅保存（SubmitFeedbackUseCase），不调整推荐权重（PLAN M8.5）。
 * 保存成功后 [submitted] 置位，UI 切换「已收到反馈」提示。
 */
@HiltViewModel
class RecommendationDetailViewModel
    @Inject
    constructor(
        private val submitFeedback: SubmitFeedbackUseCase,
        private val clock: Clock,
    ) : ViewModel() {
        private val _submitted = MutableStateFlow(false)
        val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

        fun submitFeedback(
            songId: String,
            resultId: String?,
            type: FeedbackType,
        ) {
            if (_submitted.value) return
            viewModelScope.launch {
                submitFeedback(
                    FeedbackItem(
                        feedbackId = UUID.randomUUID().toString(),
                        resultId = resultId,
                        songId = songId,
                        feedbackType = type,
                        createdAtMs = clock.nowMillis(),
                        appVersion = matchsong.app.BuildConfig.VERSION_NAME,
                    ),
                )
                _submitted.value = true
            }
        }
    }
