package matchsong.core.audio.android

import matchsong.core.common.log.LogTags
import matchsong.core.common.log.Logger

/**
 * 已初始化的 AudioRecord 探测句柄（M3.3-3）。
 *
 * 纯标记接口：JVM 测试注入 Fake 实现；真实实现由 [AndroidAudioRecordFactory] 提供
 * （封装 android.media.AudioRecord 的构造与 STATE_INITIALIZED 状态校验）。
 * 探测结果仅用于采样率可用性判断，探测实例在工厂内部随即释放。
 */
interface InitializedAudioRecord

/**
 * AudioRecord 构造工厂（M3.3-3，JVM 可测）。
 *
 * 按采样率尝试构造并初始化 AudioRecord；注入 Fake 工厂即可在纯 JVM 上测试
 * [SampleRateFallback] 的降级链逻辑，无需真实设备或模拟器。
 */
fun interface AudioRecordFactory {
    /**
     * 尝试按 [sampleRateHz] 构造并初始化 AudioRecord。
     *
     * @return 构造且状态为 STATE_INITIALIZED 返回非空句柄；不支持/无权限/未初始化返回 null。
     */
    fun create(sampleRateHz: Int): InitializedAudioRecord?
}

/**
 * 采样率运行时降级策略（M3.3-3，ADR-002，R-1）。
 *
 * 优先尝试配置采样率；不可用时沿降级链 44100 → 48000 → 16000 依次探测，
 * 返回第一个可用采样率；全部失败返回 null（调用方映射 [matchsong.core.common.error.AppError.RecordingError.InitFailed]）。
 *
 * JVM 可测：构造时注入 [AudioRecordFactory] Fake，无需 Android 运行时。
 */
class SampleRateFallback(
    private val factory: AudioRecordFactory = AndroidAudioRecordFactory(),
    private val logger: Logger = AndroidLogLogger(),
) {
    /**
     * 探测可用采样率。
     *
     * @param preferredRate 配置采样率（优先尝试，DATA-MODEL §2.2 R-1 默认 44100）。
     * @param fallbackChain 降级链（默认 ADR-002：44100 → 48000 → 16000）。
     * @return 第一个可用采样率；全部不可用返回 null。
     */
    fun findWorkingSampleRate(
        preferredRate: Int,
        fallbackChain: List<Int> = DEFAULT_FALLBACK_CHAIN,
    ): Int? {
        val candidates = LinkedHashSet<Int>()
        candidates.add(preferredRate)
        candidates.addAll(fallbackChain)
        for (rate in candidates) {
            val handle =
                try {
                    factory.create(rate)
                } catch (e: Exception) {
                    // P9：禁止空 catch —— 记录日志并按"该采样率不可用"继续降级
                    logger.w(LogTags.AUDIO, "采样率 $rate 探测异常，继续降级", e)
                    null
                }
            if (handle != null) return rate
        }
        logger.w(LogTags.AUDIO, "所有候选采样率均不可用: $candidates")
        return null
    }

    companion object {
        /** ADR-002 降级链：44100 → 48000 → 16000。 */
        val DEFAULT_FALLBACK_CHAIN: List<Int> = listOf(44_100, 48_000, 16_000)
    }
}
