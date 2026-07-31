package matchsong.spike.audiorecord

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * M-1.4 Spike：前台录音服务。
 *
 * 同时实现 AudioRecord 与 MediaRecorder 两条路径，用于对比：
 * - AudioRecord：PCM 裸数据直读（后续音高分析的基础）
 * - MediaRecorder：系统封装（输出编码文件，无法直接访问 PCM）
 *
 * 本服务仅用于 Spike 实验，不进生产模块。
 */
class RecordingService : Service() {

    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("准备录音"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MainActivity.MODE_AUDIO_RECORD
        if (!running.getAndSet(true)) {
            thread = Thread {
                try {
                    if (mode == MainActivity.MODE_AUDIO_RECORD) runAudioRecord() else runMediaRecorder()
                } catch (e: Exception) {
                    Log.e(TAG, "recording failed", e)
                } finally {
                    running.set(false)
                }
            }.also { it.start() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running.set(false)
        thread?.interrupt()
        super.onDestroy()
    }

    // ---- AudioRecord 路径：直读 PCM ----

    private fun runAudioRecord() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // 语音识别源：适合人声分析
            sampleRate,
            channelConfig,
            encoding,
            bufferSize * 2,
        )
        val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "spike_audiorecord.pcm")
        file.outputStream().use { fos ->
            val buffer = ShortArray(bufferSize / 2)
            var totalFrames = 0L
            var maxAbs = 0
            recorder.startRecording()
            while (running.get()) {
                val n = recorder.read(buffer, 0, buffer.size)
                if (n > 0) {
                    // Spike 统计：帧数与峰值（供 docs 记录）
                    for (i in 0 until n) {
                        val v = buffer[i]
                        if (kotlin.math.abs(v.toInt()) > maxAbs) maxAbs = kotlin.math.abs(v.toInt())
                    }
                    totalFrames += n
                    writeShorts(fos, buffer, n)
                }
            }
            recorder.stop()
            recorder.release()
            Log.i(TAG, "AudioRecord: totalFrames=$totalFrames peak=$maxAbs file=${file.absolutePath} size=${file.length()}")
            updateNotification(
                "AudioRecord 完成: ${file.length() / 1024}KB, ${totalFrames * 1000 / sampleRate}ms, peak=$maxAbs/32767",
            )
        }
    }

    // ---- MediaRecorder 路径：系统封装 ----

    private fun runMediaRecorder() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "spike_mediarecorder.m4a")
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        recorder.start()
        val startMs = System.currentTimeMillis()
        while (running.get()) {
            Thread.sleep(50)
        }
        recorder.stop()
        recorder.release()
        val durationMs = System.currentTimeMillis() - startMs
        Log.i(TAG, "MediaRecorder: durationMs=$durationMs file=${file.absolutePath} size=${file.length()}")
        updateNotification("MediaRecorder 完成: ${file.length() / 1024}KB, ${durationMs}ms")
    }

    private fun writeShorts(fos: FileOutputStream, shorts: ShortArray, n: Int) {
        val bytes = ByteArray(n * 2)
        for (i in 0 until n) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((shorts[i].toInt() shr 8) and 0xFF).toByte()
        }
        fos.write(bytes)
    }

    // ---- 通知 ----

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "录音 Spike", NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("录音 Spike")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RecordingSpike"
        private const val CHANNEL_ID = "recording_spike"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_MODE = "mode"

        fun start(context: Context, mode: String) {
            val intent = Intent(context, RecordingService::class.java).putExtra(EXTRA_MODE, mode)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecordingService::class.java))
        }
    }
}
