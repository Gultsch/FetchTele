package lib.fetchtele

import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLQueryComponent

interface TeleRes<RES_TYPE> {
    val type: String
    val data: RES_TYPE
}

interface TeleResParser<RES_TYPE, RAW_TYPE> {
    fun parse(raw: RAW_TYPE): TeleRes<RES_TYPE> // TODO：要不要改为 tryParse；怎么搞“统一解析”
}

data class TeleCategoryRes(override val data: String) : TeleRes<String> {
    override val type: String = "category"

    companion object : TeleResParser<String, String> {
        private const val TAG = "TeleCategoryRes"

        override fun parse(raw: String): TeleCategoryRes {
            // https://cosplaytele.com/category/DATA/

            if (raw.length < 33 || !raw.startsWith("${TELE_BASE_URL}category/") && raw.last() != '/')
                throw IllegalArgumentException("Illegal TeleCategoryRes url: $raw")

            // Extract the data between "tag/" and the first instance of "/page/" (if present) or the end of the URL
            val tagPath = raw.substring(33, raw.length - 1)
            val data = tagPath.split("/page/").firstOrNull()
                ?: throw IllegalArgumentException("Null data in TeleCategoryRes url: $raw")

            if (data.isEmpty())
                throw IllegalArgumentException("Empty data in TeleCategoryRes url: $raw")

            TeleLog.d(TAG, "解析", data)
            return TeleCategoryRes(data)
        }
    }
}

data class TeleTagRes(override val data: String) : TeleRes<String> {
    override val type: String = "tag"

    companion object : TeleResParser<String, String> {
        private const val TAG = "TeleTagRes"

        override fun parse(raw: String): TeleTagRes {
            // Check for valid URL prefix and ensure it ends with '/'
            if (raw.length < 28 || !raw.startsWith("${TELE_BASE_URL}tag/") && raw.last() != '/') {
                throw IllegalArgumentException("Illegal TeleTagRes url: $raw")
            }

            // Extract the data between "tag/" and the first instance of "/page/" (if present) or the end of the URL
            val tagPath = raw.substring(28, raw.length - 1)
            val data = tagPath.split("/page/").firstOrNull()
                ?: throw IllegalArgumentException("Null data in TeleTagRes url: $raw")

            if (data.isEmpty()) {
                throw IllegalArgumentException("Empty data in TeleTagRes url: $raw")
            }

            TeleLog.d(TAG, "TeleTagRes解析：$data")
            return TeleTagRes(data)
        }
    }
}

/*// TODO：也需要弃用
class TeleSectionRes(override val data: String) : TeleRes<String> {
    override val type: String = "section"

    companion object : TeleResParser<String, String> {
        init {
            TeleUtils.registerTeleResParser(this)
        }

        // TODO：这些是“特别页面”，不知道要不要特别的解析方式
        val BEST_COSPLAYERS = TeleSectionRes("best-cosplayers")
        val EXPLORE_CATEGORIES = TeleSectionRes("explore-categories")

        // 时间
        val TWENTY_FOUR_HOURS = TeleSectionRes("24-hours")
        val THREE_DAYS = TeleSectionRes("3-day")
        val SEVEN_DAYS = TeleSectionRes("7-day")
        val THIRTY_DAYS = TeleSectionRes("30-day")

        override fun parse(data: String): TeleSectionRes {
            TODO()
        }
    }
}*/

data class TeleKeywordRes(override val data: Keyword) : TeleRes<TeleKeywordRes.Keyword> {
    override val type: String = "keyword"

    data class Keyword(val raw: String) {
        val encoded: String = raw.encodeURLQueryComponent()
    }

    companion object : TeleResParser<Keyword, String> {
        private const val TAG = "TeleKeywordRes"

        override fun parse(raw: String): TeleKeywordRes {
            // 检查 URL 是否以 BASE_URL 开头，并包含 "?s="
            val queryStartIndex = raw.indexOf("?s=")
            if (!raw.startsWith(TELE_BASE_URL) || queryStartIndex == -1)
                throw IllegalArgumentException("Illegal TeleKeywordRes url: $raw")

            // 提取 "?s=" 的起始位置，处理可能存在的 "page/x/" 部分
            val data = raw.substring(queryStartIndex + 3).decodeURLQueryComponent()

            if (data.isEmpty())
                throw IllegalArgumentException("Empty data in TeleKeywordRes url: $raw")


            TeleLog.d(TAG, "解析：$data")
            return TeleKeywordRes(Keyword(data))
        }

    }
}

data class TeleEntryInfoRes(override val data: EntryInfo) : TeleRes<TeleEntryInfoRes.EntryInfo> {
    override val type: String = "entry_info"

    enum class EntryType {
        COSPLAY,
        FREESTYLE
    }

    data class EntryInfo(
        val title: String,
        val author: String,
        val type: EntryType,
        val photoCount: Int,
        val hasVideo: Boolean
    )

    companion object : TeleResParser<EntryInfo, String> {
        private const val TAG = "TeleEntryInfoRes"

        override fun parse(raw: String): TeleRes<EntryInfo> {
            try {
                // 1. 提取图片和视频信息（这部分格式相对固定，通常在末尾）
                // 正则表达式匹配:
                // - "..." 里的内容 (Group 1)
                // - 或者 "Only Video..." (Group 2)
                val mediaRegex = Regex("""[“”"](.*?)[”“"]|( – Only Video.*$)""")
                val mediaMatch = mediaRegex.find(raw)

                val mediaString = mediaMatch?.let {
                    it.groups[1]?.value?.takeIf { it.isNotBlank() } ?: it.groups[2]?.value
                } ?: ""

                // 从提取出的媒体信息中计算数量
                val photoCount = Regex("""(\d+)\s+photos?""").find(mediaString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                // gif也算为视频？
                val hasVideo =
                    mediaString.contains("video", ignoreCase = true) || mediaString.contains("gif", ignoreCase = true)

                // 2. 获取主要内容部分（移除了媒体信息的部分）
                val mainPart = mediaMatch?.let { raw.substring(0, it.range.first) }?.trim() ?: raw.trim()

                // 3. 根据是否存在 "cosplay" 关键字来区分类型和解析作者/标题
                val author: String
                val title: String
                val type: EntryType

                if (mainPart.contains(" cosplay ", ignoreCase = true)) {
                    type = EntryType.COSPLAY
                    // 按 " cosplay " 分割，不区分大小写
                    val parts = mainPart.split(Regex("\\s+cosplay\\s+", RegexOption.IGNORE_CASE), limit = 2)
                    if (parts.size != 2) // 如果分割失败，这是一个预料之外的格式，打印错误并返回null
                        throw IllegalArgumentException("Cosplay行格式无法解析")
                    author = parts[0].trim()
                    title = parts[1].trim()
                } else {
                    type = EntryType.FREESTYLE
                    // 按 " – " 或 " - " 分割，处理不规范的横杠和空格
                    val parts = mainPart.split(Regex("\\s+[–-]\\s+"), limit = 2)
                    if (parts.size == 2) {
                        author = parts[0].trim()
                        title = parts[1].trim()
                    } else {
                        // Fallback: 如果没有找到 "–" 或 "-" 分隔符
                        // 我们可以做一个合理的假设：最后一个词是标题，前面的是作者
                        // 但根据你的数据，所有FREESTYLE都有分隔符，所以这里更像一个安全保障
                        val words = mainPart.split(' ')
                        if (words.size > 1) {
                            author = words.dropLast(1).joinToString(" ").trim()
                            title = words.last().trim()
                        } else {
                            // 如果只有一个词，无法区分作者和标题，作为 fallback 都设为它
                            author = mainPart
                            title = mainPart
                        }
                    }
                }

                return TeleEntryInfoRes(EntryInfo(title, author, type, photoCount, hasVideo))

            } catch (e: Exception) {
                // 捕获任何意外错误，防止程序崩溃
                throw TeleException("解析【$raw】失败，只因：${e.message ?: e.toString()}")
            }
        }
    }
}

data class TeleEntryRes(override val data: String) : TeleRes<String> {
    override val type: String = "entry"

    companion object : TeleResParser<String, String> {
        private const val TAG = "TeleEntryRes"

        override fun parse(raw: String): TeleEntryRes {
            if (!raw.startsWith(TELE_BASE_URL) && raw.length < 24 && raw.last() != '/')
                throw IllegalArgumentException("Illegal TeleEntryRes url: $raw")

            val data = raw.substring(24, raw.length - 1)

            if (data.isEmpty())
                throw IllegalArgumentException("Empty data in TeleEntryRes url: $raw")

            if (data.contains('/'))
                throw IllegalArgumentException("Illegal data in TeleEntryRes url: $raw")

            TeleLog.d(TAG, "解析：$data")
            return TeleEntryRes(data)
        }
    }
}

// 特别一点，仅能由parse创建实例
/**
 * Represents a resolved video resource identifier from a specific domain.
 *
 * @property data The unique identifier for the video within its domain (e.g., "5471f32717").
 * @property videoType The domain/type of the video source.
 */
data class TeleVideoRes(override val data: String, val videoType: VideoType) : TeleRes<String> {
    enum class VideoType(val baseUrl: String) {
        COSSORA("https://cossora.stream/embed/");
        // Add other video types here with their base URLs, e.g.,
        // OTHER_DOMAIN("https://other.video/play/")
    }

    override val type: String = "video" // Consistent type identifier

    companion object : TeleResParser<String, String> {
        private const val TAG = "TeleVideoRes"

        /**
         * Parses a raw URL string into a TeleVideoRes object.
         * Extracts the video identifier and determines the video type.
         *
         * @param raw The raw URL string to parse (e.g., "https://cossora.stream/embed/5471f32717").
         * @return A TeleVideoRes instance containing the identifier and type.
         * @throws IllegalArgumentException if the URL format is invalid, missing the identifier,
         *         or contains unexpected characters in the identifier part.
         * @throws NotImplementedError if the URL does not match any known video domain prefixes.
         */
        override fun parse(raw: String): TeleVideoRes {
            // 移除初始 URL() 检查

            for (videoType in VideoType.entries) {
                if (raw.startsWith(videoType.baseUrl)) {
                    // 检查是否只有 base URL，没有标识符
                    if (raw.length == videoType.baseUrl.length) throw IllegalArgumentException(
                        "URL is missing the video identifier part: $raw"
                    )

                    val data = raw.substring(videoType.baseUrl.length)

                    // 基础校验：不允许为空：此情况理论上被上面的长度检查覆盖
                    /*if (data.isEmpty()) {
                        throw IllegalArgumentException(
                            "Extracted video identifier cannot be empty: $raw"
                        )
                    }*/

                    // 特定校验示例：不允许斜杠
                    if (data.contains('/')) throw IllegalArgumentException(
                        "Illegal character '/' found in video identifier part: $data"
                    )
                    // 添加更多特定域的 ID 格式校验...

                    TeleLog.d(TAG, "解析：类型=${videoType.name}，数据=$data")
                    return TeleVideoRes(data, videoType)
                }
            }

            // 如果循环结束都没有匹配到
            throw NotImplementedError("Unsupported video URL prefix or domain: $raw")
        }

        // Extension function remains useful
        // Consider adding null safety for url if TeleLink.url can be null
        // fun TeleLink.toTeleVideoRes(): TeleVideoRes? = this.url?.let { parse(it) }
        // Or throw if url is null, depending on desired behavior:
        fun TeleLink.toTeleVideoRes(): TeleVideoRes = parse(
            this.url ?: throw IllegalArgumentException("TeleLink URL cannot be null")
        )
    }
}