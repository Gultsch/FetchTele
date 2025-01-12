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

class TeleCategoryRes(override val data: String) : TeleRes<String> {
    override val type: String = "category"

    companion object : TeleResParser<String, String> {
        init {
            TeleUtils.registerTeleResParser(this)
        }

        // 由社区推荐
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
        val STICKY_BUNNY = TeleCategoryRes("sticky-bunny") // 咬一口兔娘ovo

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

            println("TeleCategoryRes解析：$data")
            return TeleCategoryRes(data)
        }
    }
}

class TeleTagRes(override val data: String) : TeleRes<String> {
    override val type: String = "tag"

    companion object : TeleResParser<String, String> {
        init {
            TeleUtils.registerTeleResParser(this)
        }

        // 游戏：是的这几个是标签
        val GODNESS_OF_VICTORY_NIKKE = TeleTagRes("nikke")
        val HONKAI_STAR_RAIL = TeleTagRes("honkai-star-rail")
        val BLUE_ARCHIVE = TeleTagRes("blue-archive")
        val LEAGUE_OF_LEGENDS = TeleTagRes("league-of-legends")
        val FINAL_FANTASY = TeleTagRes("final-fantasy")
        val ARKNIGHTS = TeleTagRes("arknights")
        val VALORANT = TeleTagRes("valorant")

        // 动漫
        val RE_ZERO = TeleTagRes("rezero")
        val NIE_RI_AUTOMATA = TeleTagRes("nierautomata")
        val SONO_BISQUE_DOLL = TeleTagRes("sono-bisque-doll")
        val SPY_X_FAMILY = TeleTagRes("spy-x-family")
        // TODO

        // 其他
        val MAID = TeleTagRes("maid")
        val SCHOOL_GIRL = TeleTagRes("school-girl")
        val ELF = TeleTagRes("elf")

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

            println("TeleTagRes解析：$data")
            return TeleTagRes(data)
        }
    }
}

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
}

class TeleKeywordRes(override val data: String) : TeleRes<String> {
    val encoded: String = data.encodeURLQueryComponent()

    override val type: String = "keyword"

    companion object : TeleResParser<String, String> {
        init {
            TeleUtils.registerTeleResParser(this)
        }

        override fun parse(raw: String): TeleKeywordRes {
            // 检查 URL 是否以 BASE_URL 开头，并包含 "?s="
            val queryStartIndex = raw.indexOf("?s=")
            if (!raw.startsWith(TELE_BASE_URL) || queryStartIndex == -1)
                throw IllegalArgumentException("Illegal TeleKeywordRes url: $raw")

            // 提取 "?s=" 的起始位置，处理可能存在的 "page/x/" 部分
            val data = raw.substring(queryStartIndex + 3).decodeURLQueryComponent()

            if (data.isEmpty())
                throw IllegalArgumentException("Empty data in TeleKeywordRes url: $raw")


            println("TeleKeywordRes解析：$data")
            return TeleKeywordRes(data)
        }

    }
}

class TeleEntryRes(override val data: String) : TeleRes<String> {
    override val type: String = "entry"

    companion object : TeleResParser<String, String> {
        init {
            TeleUtils.registerTeleResParser(this)
        }

        override fun parse(raw: String): TeleEntryRes {
            if (!raw.startsWith(TELE_BASE_URL) && raw.length < 24 && raw.last() != '/')
                throw IllegalArgumentException("Illegal TeleEntryRes url: $raw")

            val data = raw.substring(24, raw.length - 1)

            if (data.isEmpty())
                throw IllegalArgumentException("Empty data in TeleEntryRes url: $raw")

            if (data.contains('/'))
                throw IllegalArgumentException("Illegal data in TeleEntryRes url: $raw")

            println("TeleEntryRes解析：$data")
            return TeleEntryRes(data)
        }
    }
}