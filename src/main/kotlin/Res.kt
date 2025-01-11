package lib.fetchtele

import io.ktor.http.encodeURLQueryComponent

interface TeleRes<RES_TYPE> {
    val type: String
    val data: RES_TYPE
}

interface TeleResParser<RES_TYPE> {
    fun parse(data: RES_TYPE): TeleRes<RES_TYPE> // TODO：要不要改为 tryParse；怎么搞“统一解析”
}

class TeleCategoryRes(override val data: String) : TeleRes<String> {
    override val type: String = "category"

    companion object : TeleResParser<String> {
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

        override fun parse(data: String): TeleCategoryRes {
            TODO("从Url解析")
        }
    }
}

class TeleTagRes(override val data: String) : TeleRes<String> {
    override val type: String = "tag"

    companion object : TeleResParser<String> {
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

        override fun parse(data: String): TeleTagRes {
            TODO("从Url解析")
        }
    }
}

class TeleSectionRes(override val data: String) : TeleRes<String> {
    override val type: String = "section"

    companion object : TeleResParser<String> {
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

    companion object : TeleResParser<String> {
        override fun parse(data: String): TeleKeywordRes {
            TODO()
        }
    }
}

class TeleEntryRes(override val data: String) : TeleRes<String> {
    override val type: String = "entry"

    companion object : TeleResParser<String> {
        override fun parse(data: String): TeleEntryRes {
            TODO()
        }
    }
}