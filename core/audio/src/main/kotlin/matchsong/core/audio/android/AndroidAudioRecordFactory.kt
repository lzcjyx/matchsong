package matchsong.core.audio.android

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import matchsong.core.common.log.LogTags
import matchsong.core.common.log.Logger

/**
 * [AudioRecordFactory] 真实实现（M3.3-3）：封装 android.media.AudioRecord 的
 * 构造与 STATE_INITIALIZED 状态校验。
 *
 * 探测语义：仅验证采样率可用性——构造成功后立即释放实例（探测不启动录音、
 * 不占用麦克风）；实际采集由 [AndroidAudioRecorder] 自行创建 AudioRecord。
 * 返回 null 表示该采样率不可用（不支持/无权限/未初始化），由降级链继续尝试。
 */
internal class AndroidAudioRecordFactory(
    private val logger: Logger = AndroidLogLogger(),
) : AudioRecordFactory {
    override fun create(sampleRateHz: Int): InitializedAudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRateHz, CHANNEL_IN_MONO, ENCODING_PCM_16BIT)
        if (minBufferSize <= 0) {
            // ERROR_BAD_VALUE / ERROR：该采样率不受支持
            logger.w(LogTags.AUDIO, "getMinBufferSize($sampleRateHz) 返回 $minBufferSize，采样率不可用")
            return null
        }
        return try {
            val record =
                AudioRecord(
                    AUDIO_SOURCE,
                    sampleRateHz,
                    CHANNEL_IN_MONO,
                    ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.release() // 探测完成：不保留实例，仅确认可用
                object : InitializedAudioRecord {}
            } else {
                record.release()
                logger.w(LogTags.AUDIO, "AudioRecord 未初始化（state=${record.state}，$sampleRateHz Hz）")
                null
            }
        } catch (e: SecurityException) {
            logger.w(LogTags.AUDIO, "无麦克风权限，采样率 $sampleRateHz 探测失败", e)
            null
        } catch (e: IllegalArgumentException) {
            logger.w(LogTags.AUDIO, "采样率 $sampleRateHz 不受支持", e)
            null
        }
    }

    private companion object {
        val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
        val CHANNEL_IN_MONO: Int = AudioFormat.CHANNEL_IN_MONO
        val ENCODING_PCM_16BIT: Int = AudioFormat.ENCODING_PCM_16BIT
    }
}
