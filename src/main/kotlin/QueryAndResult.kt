﻿package lib.fetchtele

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

const val TELE_BASE_URL = "https://cosplaytele.com/"

interface TeleQuery<RESULT_TYPE> {
    fun getUrl(): String
    fun parseDocument(document: Document): RESULT_TYPE
}

data class TeleLink(
    // 以后要不要把Url改成实例化时缓存
    val url: String? = null,

    val text: String? = null,
)

class TeleListQuery(
    val categories: String? = TeleCategories.NONE,
    val tag: String? = TeleTags.NONE,
    val page: Int = 1,
) : TeleQuery<List<TeleEntrySummary>> {
    override fun getUrl(): String {
        var url = TELE_BASE_URL

        // 处理分类或者标签
        if (categories != null) {
            url += "category/$categories/"

            if (tag != null) throw TeleException("Categories page has no tags.")
        } else if (tag != null) {
            url += "tag/$tag/"
        }

        // 处理分页
        if (page > 1) {
            url += "page/$page/"
        }

        println("URL: $url")

        return url
    }

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
}

data class TeleEntrySummary(
    val title: TeleLink,
    val id: String,
    val imageUrl: String
)

class TeleEntryQuery(
    val entryId: String,
) : TeleQuery<TeleEntry> {
    override fun getUrl(): String {
        return "$TELE_BASE_URL$entryId/"
    }

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
            document.selectFirst("blockquote")!!.select("strong").forEach { strong ->
                val strongText = strong.text()

                val pair = strongText.split(": ")

                println("解析-元数据：${pair[0]}")

                when (pair[0]) {
                    "Cosplayer" -> cosplayer = strong.parseLinkableStrong(pair[1])

                    "Character" -> character = strong.parseLinkableStrong(pair[1])

                    "Appear In" -> appearIn = strong.parseLinkableStrong(pair[1])

                    "Photos" -> photos = pair[1]

                    "File Size" -> fileSize = pair[1]

                    "Unzip Password:" -> unzipPassword = strong.selectFirst("input")!!.attr("value")

                    else -> println("解析-找到未知元数据：${pair[0]}")
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