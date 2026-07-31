package matchsong.core.audio.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * M3.2-3 音频焦点管理（ARCHITECTURE.md §8.4）。
 *
 * 录音开始时申请 TRANSIENT 焦点；焦点丢失（来电/其他 App）→ 回调 [onFocusLost]，
 * 由调用方优雅停止录音并标记 interrupted（MVP 无 Pause，FR-REC-6）。
 * 焦点被占用（获取失败）→ 调用方以 MicBusy 失败处理。
 */
class AudioFocusManager(
    private val context: Context,
    private val onFocusLost: () -> Unit,
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var focusRequest: AudioFocusRequest? = null

    /**
     * 申请焦点。
     *
     * @return true 获取成功；false 被占用（不应开始录音）。
     */
    fun requestFocus(): Boolean {
        val request = buildRequest()
        focusRequest = request
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    /** 释放焦点（停止/取消时调用，try/finally 保证对称性）。 */
    fun abandonFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    private fun buildRequest(): AudioFocusRequest {
        val attrs =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
    }

    private val focusListener =
        AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                -> onFocusLost()

                else -> Unit
            }
        }
}
