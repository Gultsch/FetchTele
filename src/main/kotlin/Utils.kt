package lib.fetchtele

import io.ktor.util.reflect.instanceOf
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets

object TeleUtils {
    private val teleResParsers = mutableListOf<TeleResParser<*, *>>()

    // TODO：弱智，仅当Res类被加载时才能触发其自注册，呃呃；还是静态注册罢
    internal fun <RES_TYPE, RAW_TYPE> registerTeleResParser(parser: TeleResParser<RES_TYPE, RAW_TYPE>) {
        println("工具-（内部方法）解析器注册：${parser.javaClass}")

        teleResParsers.add(parser)
    }

    // TODO：尝试解析为资源，返回解析成功的资源列表
    fun Any.tryParseToTeleRes(): List<TeleRes<*>> {
        val resList = mutableListOf<TeleRes<*>>()

        println("工具-尝试解析为资源：$this")

        // 如果是TeleLink就尝试提取其url
        val that = if (this.instanceOf(TeleLink::class)) (this as TeleLink).url else this

        teleResParsers.forEachIndexed { index, it ->
            try {
                println("工具-解析（${index}）：${it.javaClass}")

                val res = it.call<TeleRes<*>>("parse", that)!! // 比较奇技淫巧的
                resList.add(res)
            } catch (e: Exception) {
                println("工具-解析失败（${index}）：${e.stackTraceToString()}")
            }
        }

        return resList
    }

    internal fun <T> Any.call(methodName: String, vararg args: Any?): T? {
        // 总体比较奇技淫巧的，纯纯的反射
        // 或不可调用Unit方法（？），没试过
        return try {
            println("工具-反射调用：$methodName")
            val method =
                this::class.java.methods.firstOrNull { it.name == methodName && it.parameterCount == args.size }
                    ?: throw NoSuchMethodException("Method $methodName with ${args.size} parameters not found")

            method.invoke(this, *args) as T
        } catch (e: Exception) {
            println("工具-反射调用失败（$methodName）：${e.stackTraceToString()}")
            null
        }
    }

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
            if (decodedBytes.size < 16) throw IllegalArgumentException("你怎么这么短啊细狗？连16都没有")

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
            throw RuntimeException("解码失败一个", e)
        }
    }
}