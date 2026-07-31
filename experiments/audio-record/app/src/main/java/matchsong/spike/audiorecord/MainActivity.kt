package matchsong.spike.audiorecord

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * M-1.4 Spike 入口：验证 AudioRecord / MediaRecorder 两种方案。
 *
 * 本 Spike 只做最小实验：
 * 1. 麦克风权限申请
 * 2. 开始/停止录音（AudioRecord 或 MediaRecorder 模式）
 * 3. 显示录音时长、文件大小/PCM 帧数
 *
 * 注意：实验结果记录在 docs/experiments/audio-recording-spike-results.md，
 * 本实验代码不得进入生产模块（PLAN.md §3.2 / M-1.4 验收条件）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var recordButton: Button

    private var recording = false
    private var startedAt: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply {
            text = "状态：未开始\n点击开始录音"
        }
        recordButton = Button(this).apply {
            text = "开始录音（AudioRecord）"
            setOnClickListener { onToggle() }
        }
        // 两个按钮：AudioRecord 与 MediaRecorder 模式切换
        val switchButton = Button(this).apply {
            text = "切换到 MediaRecorder 模式"
            setOnClickListener {
                if (!recording) {
                    currentMode = if (currentMode == MODE_AUDIO_RECORD) MODE_MEDIA_RECORDER else MODE_AUDIO_RECORD
                    text = if (currentMode == MODE_AUDIO_RECORD) "当前：AudioRecord" else "当前：MediaRecorder"
                    recordButton.text = if (currentMode == MODE_AUDIO_RECORD) "开始录音（AudioRecord）" else "开始录音（MediaRecorder）"
                }
            }
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(statusText)
            addView(recordButton)
            addView(switchButton)
        }
        setContentView(container)

        requestMicPermission()
    }

    private fun onToggle() {
        if (recording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text = "缺少麦克风权限"
            requestMicPermission()
            return
        }
        recording = true
        startedAt = System.currentTimeMillis()
        statusText.text = "录音中…（${if (currentMode == MODE_AUDIO_RECORD) "AudioRecord" else "MediaRecorder"}）"
        // Spike 简化：录音由 RecordingService 承载（前台服务，验证后台/息屏录音能力）
        RecordingService.start(this, currentMode)
    }

    private fun stopRecording() {
        recording = false
        val durationMs = System.currentTimeMillis() - startedAt
        RecordingService.stop(this)
        statusText.text = "已停止，时长 ${durationMs / 1000.0}s\n" +
            "详情见 RecordingService 统计（文件大小 / PCM 帧数）"
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            statusText.text = if (granted) "权限已授予" else "权限被拒绝：无法录音"
        }
    }

    companion object {
        const val MODE_AUDIO_RECORD = "audiorecord"
        const val MODE_MEDIA_RECORDER = "mediarecorder"
        private var currentMode: String = MODE_AUDIO_RECORD
    }
}
