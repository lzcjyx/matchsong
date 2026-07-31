package matchsong.core.audio.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import matchsong.domain.recording.RecordingState

/**
 * M3.2-1 录音前台服务（FR-REC-3/9，ARCHITECTURE.md §8.3）。
 *
 * - startForegroundService 入口，5s 内 startForeground（Android 12+ 强制）；
 * - 通知含"停止录音"动作；
 * - onTaskRemoved/onDestroy 兜底停止并清理（联动 M3.5-2）；
 * - 录音执行委托 [RecordingSessionRunner]（与 AndroidRecordingPort 共享实例）。
 */
class RecordingService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("准备录音"))
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val action = intent?.action
        if (ACTION_STOP == action) {
            RecordingSessionRunner.instance?.stop(interrupted = false)
            stopSelf()
            return START_NOT_STICKY
        }
        // 首次启动：开始录音会话（若尚未开始）
        RecordingSessionRunner.instance?.let { runner ->
            if (runner.stateMachine.state == matchsong.domain.recording.RecordingState.IDLE) {
                runner.start(this)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 用户划掉任务：兜底停止并清理（ARCHITECTURE.md §8.3）
        RecordingSessionRunner.instance?.stop(interrupted = true)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        RecordingSessionRunner.instance?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "录音",
                    NotificationManager.IMPORTANCE_LOW,
                )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent =
            PendingIntent.getService(
                this,
                0,
                Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        // minSdk 26：Notification.Builder(this, channelId) 可用（无 androidx.core 依赖）
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("MatchSong 录音中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(stopIntent)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1002
        private const val ACTION_STOP = "matchsong.recording.STOP"

        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(
            context: Context,
            interrupted: Boolean,
        ) {
            RecordingSessionRunner.instance?.stop(interrupted = interrupted)
            context.stopService(Intent(context, RecordingService::class.java))
        }
    }
}
