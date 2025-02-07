package lib.fetchtele

import io.ktor.http.encodeURLQueryComponent
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

const val TELE_BASE_URL = "https://cosplaytele.com/"

sealed class TeleResult<RESULT_TYPE> {
    data class Success<RESULT_TYPE>(val data: RESULT_TYPE) : TeleResult<RESULT_TYPE>()
    data class Failure<RESULT_TYPE>(val error: Throwable) : TeleResult<RESULT_TYPE>()
}

interface TeleQuery<RESULT_TYPE> {
    val url: String
    fun parseDocument(document: Document): RESULT_TYPE
}

data class TeleLink(
    // 以后要不要把Url改成实例化时缓存
    val url: String? = null,

    val text: String? = null,
)

class TeleListQuery private constructor(override val url: String) : TeleQuery<List<TeleEntrySummary>> {
    override fun parseDocument(document: Document): List<TeleEntrySummary> {
        try {
            val entries = mutableListOf<TeleEntrySummary>()

            document.selectFirst(".row.large-columns-3.medium-columns-.small-columns-1.row-masonry")!!
                .select("div.col.post-item").forEach { element ->
                    try {
                        val imageUrl =
                            element.selectFirst("img.attachment-medium.size-medium.wp-post-image")!!.attr("src")
                        val textElement = element.selectFirst("h5.post-title.is-large a")!!
                        val title = textElement.text()
                        val url = textElement.attr("href")
                        val name = url.substring(24, url.length - 1)

                        println("解析-实体概况：【标题：$title, Id：$name, 图片链接: $imageUrl】")
                        entries.add(TeleEntrySummary(title = TeleLink(url, title), id = name, imageUrl = imageUrl))
                    } catch (e: Exception) {
                        println("Failed to parse entry summary: \n${e.stackTraceToString()}\n$element")
                    }
                }

            return entries
        } catch (e: Exception) {
            throw TeleException("Failed to parse list: ${e.stackTraceToString()}")
        }
    }

    companion object {
        fun build(
            categories: TeleCategoryRes? = null,
            tag: TeleTagRes? = null,
            keyword: TeleKeywordRes? = null,
            page: Int = 1,
        ): TeleListQuery {
            var url = TELE_BASE_URL

            // 如果有关键词，优先处理关键词逻辑
            if (keyword != null) {
                if (page > 1) url += "page/$page/"

                url += "?s=${keyword.encoded}"
            } else {
                // 处理分类或者标签的逻辑
                if (categories != null) {
                    if (tag != null) throw TeleException("Categories page has no tags")
                    url += "category/${categories.data}/"
                } else if (tag != null) {
                    url += "tag/${tag.data}/"
                }

                // 如果有分页，并且不是关键词情况，统一加页码
                if (page > 1) url += "page/$page/"
            }

            println("创建查询-列表：$url")

            return TeleListQuery(url)
        }
    }
}

data class TeleEntrySummary(
    val title: TeleLink,
    val id: String,
    val imageUrl: String
)

class TeleEntryQuery(
    override val url: String
) : TeleQuery<TeleEntry> {
    override fun parseDocument(document: Document): TeleEntry {
        try {
            // 标题
            val title = document.selectFirst(".entry-title")!!.text()

            println("解析-标题：$title")

            // 分类
            val categories = mutableListOf<TeleLink>()
            document.selectFirst(".entry-category.is-xsmall")!!.select("a").forEach {
                val url = it.attr("href")
                val text = it.text()

                println("解析-分类：【$text：$url】")

                categories.add(TeleLink(url, text))
            }

            // 元数据
            var cosplayer: TeleLink? = null
            var character: TeleLink? = null
            var appearIn: TeleLink? = null
            var photos: String? = null
            var fileSize: String? = null
            var unzipPassword: String? = null
            document.selectFirst("blockquote")!!.let {
                it.select("strong").forEach { strong ->
                    val strongText = strong.text()

                    val pair = strongText.split(": ")

                    println("解析-元数据：${pair[0]}")

                    when (pair[0]) {
                        "Cosplayer" -> cosplayer = strong.parseLinkableStrong(pair[1])

                        "Character" -> character = strong.parseLinkableStrong(pair[1])

                        "Appear In" -> appearIn = strong.parseLinkableStrong(pair[1])

                        "Photos" -> photos = pair[1]

                        "File Size" -> fileSize = pair[1]

                        "Unzip Password:" -> unzipPassword = it.selectFirst("input")!!.attr("value")

                        else -> println("解析-找到未知元数据：${pair[0]}")
                    }
                }
            }

            // 下载链接
            val downloadLinks = mutableListOf<TeleLink>()
            document.select(".button.alert").forEach { button ->
                val buttonText = button.text()

                // 如果有链接才添加
                button.attr("href").let {
                    if (it.isNotEmpty()) {
                        println("解析-下载链接：【${buttonText}：$it】")
                        downloadLinks.add(TeleLink(url = it, text = buttonText))
                    }
                }
            }

            // 视频
            var video: TeleLink? = null
            document.select("iframe").forEach { iframe ->
                val src = iframe.attr("src")

                if (!src.startsWith("https://cossora.stream/embed/")) return@forEach

                println("解析-视频：$src")

                video = TeleLink(url = src)

                // TODO：从视频iFrame解析视频地址
            }

            // 图片
            val images = mutableListOf<TeleLink>()
            document.select(".attachment-full.size-full").forEach {
                val src = it.attr("src")

                println("解析-图片：$src")

                images.add(TeleLink(url = src))
            }

            val teleEntry = TeleEntry(
                title = title,
                categories = categories,
                pageMetaData = TeleEntry.TeleEntryMetaData(
                    cosplayer = cosplayer,
                    character = character,
                    appearIn = appearIn,
                    photos = photos,
                    fileSize = fileSize,
                    unzipPassword = unzipPassword,
                ),
                downloadLinks = downloadLinks,
                video = video,
                images = images,
            )

            return teleEntry
        } catch (e: Exception) {
            throw TeleException("Failed to parse entry: ${e.stackTraceToString()}")
        }
    }

    companion object {
        // 解析可能是超链接的强调文本
        internal fun Element.parseLinkableStrong(fallbackText: String): TeleLink {
            val linkElement = selectFirst("a")
            return if (linkElement != null) {
                // 如果是超链接
                TeleLink(
                    url = linkElement.attr("href"),
                    text = linkElement.text()
                )
            } else {
                // 如果不是超链接
                TeleLink(
                    text = fallbackText
                )
            }
        }

        fun build(entryId: String): TeleEntryQuery {
            val url = "$TELE_BASE_URL$entryId/"

            println("创建查询-实体：$url")

            return TeleEntryQuery(url)
        }
    }
}

data class TeleEntry(
    val title: String,
    val categories: List<TeleLink>,
    val pageMetaData: TeleEntryMetaData,
    val downloadLinks: List<TeleLink>,
    val video: TeleLink?,
    val images: List<TeleLink>,
) {
    data class TeleEntryMetaData(
        val cosplayer: TeleLink?,
        val character: TeleLink?,
        val appearIn: TeleLink?,
        val photos: String?,
        val fileSize: String?,
        val unzipPassword: String?,
    )
}