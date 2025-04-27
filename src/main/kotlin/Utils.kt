package lib.fetchtele

import java.io.PrintWriter
import java.io.StringWriter
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets

object TeleLogUtils {
    enum class Level(val value: String) { DEBUG("D"), INFO("I"), WARN("W"), ERROR("E") }

    /**
     * Data class holding log information.
     * Cannot be a `data class` because of the `vararg` constructor parameter.
     *
     * @property level The severity level of the log message.
     * @property tag A tag identifying the source of the log message (e.g., class name).
     * @property messages The content of the log message(s). The last element might be a Throwable for ERROR level.
     */
    class Log(val level: Level, val tag: String, vararg val messages: Any?) {
    }

    /**
     * The default logger implementation that prints to standard out/err.
     * Handles the convention that the last argument for ERROR might be a Throwable.
     */
    private val defaultLogger: (Log) -> Unit = { log ->
        var throwable: Throwable? = null
        val messageContent: List<Any?>

        // Check if the last argument for an ERROR log is a Throwable
        if (log.level == Level.ERROR && log.messages.isNotEmpty() && log.messages.last() is Throwable) {
            throwable = log.messages.last() as Throwable
            // Use all messages except the last one for the main log string
            messageContent = log.messages.dropLast(1)
        } else {
            // Use all messages for the main log string
            messageContent = log.messages.toList() // Convert vararg to list for consistent handling
        }

        // Format the message part by joining the non-throwable arguments
        val messageString = messageContent.joinToString(" ") { it?.toString() ?: "null" }

        // Prepare the final log line
        val logLine = "[${log.level.name}] ${log.tag} > $messageString"

        // Print log line to appropriate stream (stderr for WARN/ERROR)
        when (log.level) {
            Level.WARN, Level.ERROR -> System.err.println(logLine)
            else -> println(logLine)
        }

        // Print stack trace to stderr if a throwable was found
        throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            System.err.print(sw.toString()) // Use print to avoid extra newline after stacktrace
        }
    }

    /** The currently active logger function. Initially set to the default logger. */
    @Volatile // Ensure visibility across threads, though assignment isn't atomic
    private var currentLogger: (Log) -> Unit = defaultLogger

    /**
     * Sets a logger implementation.
     * When a logger is set (not null), the default logger is disabled,
     * and all log messages are routed to the provided logger.
     * If `null` is passed, the logger reverts to the default one.
     *
     * @param logger The logger function `(TeleLogger.Log) -> Unit`, or `null` to reset to default.
     */
    internal fun setLogger(logger: ((Log) -> Unit)?) {
        currentLogger = logger ?: defaultLogger
    }

    /** Logs a DEBUG message. */
    fun d(tag: String, vararg messages: Any?) {
        // Optimization: Could check log level here if the logger supported it,
        // but for now, always create the Log object and delegate.
        currentLogger(Log(Level.DEBUG, tag, *messages)) // Use spread operator (*)
    }

    /** Logs an INFO message. */
    fun i(tag: String, vararg messages: Any?) {
        currentLogger(Log(Level.INFO, tag, *messages))
    }

    /** Logs a WARN message. */
    fun w(tag: String, vararg messages: Any?) {
        currentLogger(Log(Level.WARN, tag, *messages))
    }

    /**
     * Logs an ERROR message.
     * Conventionally, the last argument in `messages` may be a `Throwable`.
     * The active logger implementation is responsible for handling this.
     */
    fun e(tag: String, vararg messages: Any?) {
        currentLogger(Log(Level.ERROR, tag, *messages))
    }
}

object TeleUtils {
    /**
     * 使用 AES/CBC/PKCS5Padding 解密经过特定格式编码的链接或数据。
     *
     * @param base64CiphertextWithIv Base64 编码的字符串，前 16 字节是 IV，后面是密文。
     * @param keyString 用于解密的 UTF-8 密钥字符串。其字节长度必须是 16, 24 或 32。
     * @return 解密后的 UTF-8 字符串，如果解密失败则返回 null。
     */
    fun decryptCossoraLink(base64CiphertextWithIv: String, keyString: String): String {
        return try {
            // 1. Base64 解码
            val decodedBytes = Base64.getDecoder().decode(base64CiphertextWithIv)

            // 检查解码后的数据长度是否足够包含 IV (16 字节)
            if (decodedBytes.size < 16) throw IllegalArgumentException("Invalid Base64 data: shorter than 16 bytes")

            // 2. 分离 IV 和密文
            // IV 是前 16 个字节
            val ivBytes = decodedBytes.copyOfRange(0, 16)
            // 密文是 IV 之后的部分
            val ciphertextBytes = decodedBytes.copyOfRange(16, decodedBytes.size)

            // 3. 准备密钥
            // 将密钥字符串转换为 UTF-8 字节数组
            val keyBytes = keyString.toByteArray(Charsets.UTF_8)
            // 创建 AES 密钥规范。注意：未检查 keyBytes 的长度是否为 16, 24, 32
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            // 4. 准备 IV 参数规范
            val ivParameterSpec = IvParameterSpec(ivBytes)

            // 5. 获取并初始化 Cipher 实例
            // 使用 "AES/CBC/PKCS5Padding" (与 CryptoJS 的 Pkcs7 在 AES 上等效)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

            // 6. 执行解密
            val decryptedBytes = cipher.doFinal(ciphertextBytes)

            // 7. 将解密后的字节数组转换回 UTF-8 字符串
            String(decryptedBytes, Charsets.UTF_8)

        } catch (e: Exception) {
            throw RuntimeException("Decryption failed: ${e.message}", e)
        }
    }
}