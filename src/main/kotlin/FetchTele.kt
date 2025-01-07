package lib.fetchtele

import io.ktor.client.*
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.http
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup

class TeleFetcher {
    val httpClient = HttpClient(CIO) {
        // 设置代理？
        engine {
            proxy = ProxyBuilder.http("http://127.0.0.1:7890")
        }

        // 设置请求超时时间
        /*install(HttpTimeout) {
            requestTimeoutMillis = 10_000
        }*/
    }

    init {
    }

    // 缓存页码
    suspend fun fetch(
        teleType: TeleType = TeleType.default,
        pageNumber: TelePageNumber = TelePageNumber.default
    ): TeleResult {
        val url = TeleUrl.buildUrl(teleType, pageNumber)

        try {
            val response = httpClient.get(url.url)

            if (response.status.value != 200) throw TeleException("HttpClient failed to fetch: ${url.url}.")

            try {
                val document = Jsoup.parse(response.bodyAsText())

                val teleResult = TeleResult()

                if (url.teleUrlType == TeleUrl.TeleUrlType.LIST) {
                    try {
                        val entries = mutableListOf<TeleEntrySummary>()
                        document.select(".row.large-columns-3.medium-columns-.small-columns-1.row-masonry").first()!!
                            .select("div.col.post-item").forEach { element ->
                                try {
                                    val imageUrl =
                                        element.select("img.attachment-medium.size-medium.wp-post-image").first()!!
                                            .attr("src")
                                    val textElement = element.select("h5.post-title.is-large a").first()!!
                                    val title = textElement.text()
                                    val url = textElement.attr("href")
                                    val name = url.substring(24, url.length - 1)

                                    // println("Title: $title, Name: $name,  Image: $imageUrl")
                                    entries.add(TeleEntrySummary(TeleLink(url, title), name, imageUrl))
                                } catch (e: Exception) {
                                    println("Failed to parse entry summary: \n${e.stackTraceToString()}\n$element")
                                }
                            }
                        teleResult["main"] = entries

                    } catch (e: Exception) {
                        throw TeleException("Failed to parse list: ${e.stackTraceToString()}")
                    }
                } else if (url.teleUrlType == TeleUrl.TeleUrlType.ENTRY) {
                    try {
                        // 元数据
                        document.select("blockquote").first()!!.select("strong").forEach {
                            println(it.text())
                        }

                        // 下载链接
                        document.select(".button.alert").forEach {
                            println(it.text())
                        }

                        // 视频
                        document.select(".jw-video.jw-reset").first()?.let {
                            println(it.attr("src"))
                        }

                        // 图片
                        document.select(".attachment-full.size-full").forEach {
                            println(it.attr("src"))
                        }
                    } catch (e: Exception) {
                        throw TeleException("Failed to parse entry: ${e.stackTraceToString()}")
                    }
                } else if (url.teleUrlType == TeleUrl.TeleUrlType.SPECIAL_SECTION) {
                    try {

                    } catch (e: Exception) {
                        throw TeleException("Failed to parse special section: ${e.stackTraceToString()}")
                    }
                }

                return teleResult
            } catch (e: Exception) {
                throw TeleException("Failed to parse response: ${e.stackTraceToString()}")
            }
        } catch (e: Exception) {
            throw TeleException("Failed to fetch: ${e.stackTraceToString()}")
        }
    }

    companion object {
    }
}

class TeleException(message: String) : Exception("FetchTele: $message")

class TeleUrl internal constructor(val url: String, val teleUrlType: TeleUrlType) {
    enum class TeleUrlType {
        LIST,
        SPECIAL_SECTION,
        ENTRY,
    }

    companion object {
        fun buildUrl(teleType: TeleType, page: TelePageNumber): TeleUrl {
            val sections = teleType.sections
            val categories = teleType.categories
            val tag = teleType.tag
            val desiredPage = page.desiredPage

            var url = "https://cosplaytele.com/"
            var type = TeleUrlType.LIST

            if (sections != null) {
                url += "$sections/"
                type = TeleUrlType.SPECIAL_SECTION

                if (desiredPage != 1 || categories != null || tag != null) throw TeleException("Special section page has no page number, categories or tags.")
            } else {
                if (categories != null) {
                    url += "category/$categories/"

                    if (tag != null) throw TeleException("Categories page has no tags.")
                } else if (tag != null) {
                    url += "tag/$tag/"
                }

                if (desiredPage > 1) {
                    url += "page/$desiredPage/"
                }
            }

            println("URL: $url, Type: $type")

            return TeleUrl(url, type)
        }

        fun buildEntryUrl(entryName: String): TeleUrl {
            return TeleUrl("https://cosplaytele.com/$entryName/", TeleUrlType.ENTRY)
        }
    }
}

class TeleResult internal constructor() : MutableMap<String, Any> by mutableMapOf() {
    internal operator fun set(key: String, value: Any) {
        println("Set $key to $value")
        (this as MutableMap<String, Any>)[key] = value
    }

    inline fun <reified T> getMainElement(): T {
        val mainElement = this["main"]
        if (mainElement == null) throw TeleException("TeleResult has no main element.")
        else if (mainElement !is T) throw TeleException("TeleResult's main element is not a ${T::class.simpleName}.")
        else return mainElement as T
    }

    fun getTeleEntrySummaries(): List<TeleEntrySummary> = getMainElement<List<TeleEntrySummary>>()

    fun getTeleEntry(): TeleEntry = getMainElement<TeleEntry>()
}

// 这个类是为了正确获取内容页的，就怕超界；
class TelePageNumber(val desiredPage: Int = 1) {
    companion object {
        val default = TelePageNumber()
    }
}

class TeleType(
    val sections: String? = TeleSections.NONE,
    val categories: String? = TeleCategories.NONE,
    val tag: String? = TeleTags.NONE
) {
    companion object {
        val default = TeleType()
    }
}

object TeleCategories {
    val NONE = null

    // 由社区推荐
    val VIDEO_COSPLAY = "video-cosplay" // 视频

    // 分级
    val NUDE = "nude" // 裸体
    val NO_NUDE = "no-nude" // 非裸体
    val COSPLAY = "cosplay" // 角色扮演

    // 游戏：是的这几个是类别
    val GENSHIN_IMPACT = "genshin-impact" // 原神
    val FATE_GRAND_ORDER = "fate-grand-order" // 命运-冠位指定
    val AZUR_LANE = "azur-lane" // 碧蓝航线

    // 人物
    val NEKOKOYOSHI = "nekokoyoshi" // 爆机少女喵小吉
    val ARTY_HUANG = "artyhuang" // Arty亚缇
    val AQUA = "aqua" // 水淼
    val UMEKO_J = "umeko-j"
    val BYORU = "byoru" // ビョル
    val XIAO_DING = "xiaoding" // 小丁
    val RIOKO = "rioko" // 凉凉子
    val STICKY_BUNNY = "sticky-bunny" // 咬一口兔娘ovo

}

object TeleTags {
    val NONE = null

    // 游戏：是的这几个是标签
    val GODNESS_OF_VICTORY_NIKKE = "nikke"
    val HONKAI_STAR_RAIL = "honkai-star-rail"
    val BLUE_ARCHIVE = "blue-archive"
    val LEAGUE_OF_LEGENDS = "league-of-legends"
    val FINAL_FANTASY = "final-fantasy"
    val ARKNIGHTS = "arknights"
    val VALORANT = "valorant"

    // 动漫
    val RE_ZERO = "rezero"
    val NIE_RI_AUTOMATA = "nierautomata"
    val SONO_BISQUE_DOLL = "sono-bisque-doll"
    val SPY_X_FAMILY = "spy-x-family"
    // TODO

    // 自由派
    val MAID = "maid"
    val SCHOOL_GIRL = "school-girl"
    val ELF = "elf"
}

object TeleSections {
    val NONE = null

    // TODO：这些是“特别页面”，不知道要不要特别的解析方式
    val BEST_COSPLAYERS = "best-cosplayers"
    val EXPLORE_CATEGORIES = "explore-categories"

    // 时间
    val TWENTY_FOUR_HOURS = "24-hours"
    val THREE_DAYS = "3-day"
    val SEVEN_DAYS = "7-day"
    val THIRTY_DAYS = "30-day"
}

data class TeleEntrySummary(
    val title: TeleLink,
    val name: String,
    val imageUrl: String
)

data class TeleLink(
    val url: String,
    val text: String? = null,
)

data class TeleEntry(
    val title: String,
    val tags: List<String>,
    val pageMetaData: TeleEntryMetaData,
    val downloadLinks: List<TeleLink>,
    val video: TeleLink?,
    val images: List<TeleLink>,
) {
    data class TeleEntryMetaData(
        val cosplayer: String,
        val character: TeleLink,
        val appearIn: TeleLink,
        val photos: String,
        val fileSize: String,
        val unzipPassword: String,
    )
}

/*
data class TeleVideo(
    val url: String,
)

data class TeleImage(
    val url: String,
)
*/
