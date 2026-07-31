package matchsong.core.audio.algorithm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import matchsong.domain.recording.VolumeLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * M3.6-1 节流操作符测试（FR-REC-4，≤10Hz；ARCHITECTURE.md §14.2）。
 * 使用 kotlinx-coroutines-test 虚拟时间验证节流窗口与不丢尾语义。
 *
 * 注意：collector 用前台 launch（而非 backgroundScope）——advanceUntilIdle 不会恢复
 * backgroundScope 中已挂起的 delay，前台子协程由 runTest 自动推进。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThrottleTest {
    @Test
    fun `50ms 内发射 10 个值最多输出 1 次且保留最新值`() =
        runTest {
            val upstream =
                flow {
                    repeat(10) { i ->
                        emit(i)
                        delay(5)
                    }
                }
            val emissions = mutableListOf<Int>()
            val collector =
                launch {
                    upstream.throttleLatest(100).collect { emissions += it }
                }

            advanceTimeBy(50) // 上游 10 个值已全部发射（t=0..45ms）
            assertEquals(listOf(0), emissions) // 首个值立即发射，窗口内其余值被丢弃

            advanceUntilIdle()
            collector.join()
            assertEquals(listOf(0, 9), emissions) // 窗口结束后补发最新值（不丢尾）
        }

    @Test
    fun `上游立即完成时最后一个值仍被送达`() =
        runTest {
            val result = flowOf(1, 2, 3).throttleLatest(100).toList()

            assertEquals(listOf(1, 3), result) // 中间值 2 被丢弃，最后一个值 3 送达
        }

    @Test
    fun `发射间隔不小于节流周期`() =
        runTest {
            val upstream =
                flow {
                    repeat(10) { i ->
                        emit(i)
                        delay(5)
                    }
                }
            val timestamps = mutableListOf<Long>()
            val collector =
                launch {
                    upstream.throttleLatest(100).collect { timestamps += this@runTest.currentTime }
                }

            advanceUntilIdle()
            collector.join()
            assertEquals(listOf(0L, 100L), timestamps) // 首次立即发射，后续间隔 100ms
        }

    @Test
    fun `超过节流周期的输入原样通过`() =
        runTest {
            val upstream =
                flow {
                    emit(1)
                    delay(150)
                    emit(2)
                }
            val emissions = mutableListOf<Int>()
            val collector =
                launch {
                    upstream.throttleLatest(100).collect { emissions += it }
                }

            advanceUntilIdle()
            collector.join()
            assertEquals(listOf(1, 2), emissions) // 间隔 150ms > 100ms，均按原样发射
        }

    @Test
    fun `throttledVolume 默认 100ms 节流音量流`() =
        runTest {
            val levels =
                listOf(
                    VolumeLevel(rms = 0.5, isTooQuiet = false, isClipping = false, hasInput = true),
                    VolumeLevel(rms = 0.6, isTooQuiet = false, isClipping = false, hasInput = true),
                    VolumeLevel(rms = 0.7, isTooQuiet = false, isClipping = false, hasInput = true),
                )
            val upstream =
                flow {
                    levels.forEach {
                        emit(it)
                        delay(5)
                    }
                }
            val emissions = mutableListOf<VolumeLevel>()
            val collector =
                launch {
                    upstream.throttledVolume().collect { emissions += it }
                }

            advanceUntilIdle()
            collector.join()
            assertEquals(listOf(levels[0], levels[2]), emissions)
        }

    @Test
    fun `periodMs 非正时抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) { flowOf(1).throttleLatest(0) }
        assertThrows(IllegalArgumentException::class.java) { flowOf(1).throttleLatest(-100) }
    }
}
