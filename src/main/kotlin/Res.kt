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
        // 不再内建
        /*// 由社区推荐
        val VIDEO_COSPLAY = TeleCategoryRes("video-cosplay") // 视频

        // 分级
        val NUDE = TeleCategoryRes("nude") // 裸体
        val NO_NUDE = TeleCategoryRes("no-nude") // 非裸体
        val COSPLAY = TeleCategoryRes("cosplay") // 角色扮演

        // 游戏：是的这几个是类别
        val GENSHIN_IMPACT = TeleCategoryRes("genshin-impact") // 原神
        val FATE_GRAND_ORDER = TeleCategoryRes("fate-grand-order") // 命运-冠位指定
        val AZUR_LANE = TeleCategoryRes("azur-lane") // 碧蓝航线

        // 人物
        val NEKOKOYOSHI = TeleCategoryRes("nekokoyoshi") // 爆机少女喵小吉
        val ARTY_HUANG = TeleCategoryRes("artyhuang") // Arty亚缇
        val AQUA = TeleCategoryRes("aqua") // 水淼
        val UMEKO_J = TeleCategoryRes("umeko-j")
        val BYORU = TeleCategoryRes("byoru") // ビョル
        val XIAO_DING = TeleCategoryRes("xiaoding") // 小丁
        val RIOKO = TeleCategoryRes("rioko") // 凉凉子
        val STICKY_BUNNY = TeleCategoryRes("sticky-bunny") // 咬一口兔娘ovo*/

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

            TeleLogUtils.d(TAG, "解析", data)
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

            TeleLogUtils.d(TAG, "TeleTagRes解析：$data")
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

data class TeleKeywordRes(override val data: String) : TeleRes<String> {
    val encoded: String = data.encodeURLQueryComponent()

    override val type: String = "keyword"

    companion object : TeleResParser<String, String> {
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


            TeleLogUtils.d(TAG, "解析：$data")
            return TeleKeywordRes(data)
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

            TeleLogUtils.d(TAG, "解析：$data")
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

                    TeleLogUtils.d(TAG, "解析：类型=${videoType.name}，数据=$data")
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