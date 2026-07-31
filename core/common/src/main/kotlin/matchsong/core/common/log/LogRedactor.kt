package matchsong.core.common.log

/**
 * 日志脱敏过滤器（ARCHITECTURE.md §13，FR-PRIV-4，M1.4-3）。
 *
 * 纯函数：将敏感模式替换为 [REDACTED]，Release 构建下由 Logger 实现统一调用。
 * 覆盖模式（§13 禁止输出项）：
 * - 绝对文件路径（Android 数据目录 /data、/storage、/sdcard、/mnt、/cache；Windows 盘符路径）；
 * - UUID（会话号）；
 * - 设备标识（ANDROID_ID、IMEI、序列号，含带前缀与裸值形式）。
 *
 * 保留：聚合指标（帧数、耗时、质量统计）、错误类型与脱敏消息。
 *
 * 注意：原始音频样本/内容不得在任何级别记录——这是调用方约定，本过滤器无法识别二进制内容。
 */
object LogRedactor {
    /** 脱敏替换占位符。 */
    const val REDACTED = "[REDACTED]"

    private val patterns: List<Regex> =
        listOf(
            // UUID（会话号）：8-4-4-4-12 十六进制
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
            // Android 数据目录绝对路径：/data/...、/storage/...、/sdcard/...、/mnt/...、/cache/...
            Regex("(?:/data/|/storage/|/sdcard/|/mnt/|/cache/)[^\\s,\"')]*"),
            // Windows 绝对路径：C:\...、D:\...
            Regex("[A-Za-z]:\\\\[^\\s,\"')]*"),
            // 带前缀的设备标识：ANDROID_ID=...、imei:...、serial=...
            Regex("(?i)(?:android[_-]?id|imei|serial)\\s*[:=]\\s*[0-9A-Za-z-]{4,}"),
            // 裸 ANDROID_ID（16 位十六进制）
            Regex("\\b[0-9a-fA-F]{16}\\b"),
            // 裸 IMEI（15 位数字）
            Regex("\\b\\d{15}\\b"),
        )

    /** 脱敏输入字符串，返回不含敏感原文的副本。 */
    fun redact(input: String): String {
        if (input.isEmpty()) return input
        var result = input
        for (pattern in patterns) {
            result = pattern.replace(result, REDACTED)
        }
        return result
    }
}
