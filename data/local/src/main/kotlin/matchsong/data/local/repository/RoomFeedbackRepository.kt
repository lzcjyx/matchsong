package matchsong.data.local.repository

import matchsong.data.local.db.dao.FeedbackDao
import matchsong.data.local.db.entity.FeedbackEntity
import matchsong.domain.port.FeedbackItem
import matchsong.domain.port.FeedbackRepository
import matchsong.domain.port.FeedbackType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * domain FeedbackRepository Port 的 Room 实现（M8.5-1，data-model §2.13 / §3.2）。
 *
 * 提交策略（M8.5-1 [推测]）：同一（resultId + songId）重复提交时**更新**原记录
 * （刷新反馈类型/时间/版本），而非新增——避免同一推荐结果对同一歌曲堆积多条反馈。
 * 仅保存，不自动调整推荐权重（PLAN M8.5 明确）。
 */
@Singleton
class RoomFeedbackRepository
    @Inject
    constructor(
        private val feedbackDao: FeedbackDao,
    ) : FeedbackRepository {
        override suspend fun submit(feedback: FeedbackItem) {
            val existing = feedbackDao.findByResultAndSong(feedback.resultId, feedback.songId)
            if (existing != null) {
                feedbackDao.update(
                    existing.copy(
                        feedbackType = feedback.feedbackType.name,
                        createdAtMs = feedback.createdAtMs,
                        appVersion = feedback.appVersion,
                    ),
                )
            } else {
                feedbackDao.insert(feedback.toEntity())
            }
        }

        override suspend fun getAll(): List<FeedbackItem> = feedbackDao.getAll().map { it.toFeedbackItem() }

        override suspend fun clear() {
            feedbackDao.clearAll()
        }
    }

/** [FeedbackItem]（domain）→ [FeedbackEntity]（Room）；主键交给自增列生成。 */
internal fun FeedbackItem.toEntity(): FeedbackEntity =
    FeedbackEntity(
        songId = songId,
        resultId = resultId,
        feedbackType = feedbackType.name,
        createdAtMs = createdAtMs,
        appVersion = appVersion,
    )

/** [FeedbackEntity]（Room）→ [FeedbackItem]（domain）。 */
internal fun FeedbackEntity.toFeedbackItem(): FeedbackItem =
    FeedbackItem(
        feedbackId = feedbackId.toString(),
        resultId = resultId,
        songId = songId,
        feedbackType = FeedbackType.valueOf(feedbackType),
        createdAtMs = createdAtMs,
        appVersion = appVersion,
    )
