package lib.fetchtele

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

const val TELE_BASE_URL = "https://cosplaytele.com/"

sealed class TeleResult<RESULT_TYPE> {
    data class Success<RESULT_TYPE>(val data: RESULT_TYPE) : TeleResult<RESULT_TYPE>()
    data class Failure<RESULT_TYPE>(val error: Exception) : TeleResult<RESULT_TYPE>()
}

interface TeleQuery<RESULT_TYPE> {
    val url: String
    fun parseDocument(document: Document): RESULT_TYPE

    fun configureRequest(builder: HttpRequestBuilder) {
    }
}

data class TeleLink(
    // 以后要不要把Url改成实例化时缓存
    val url: String? = null,

    val text: String? = null,
)

// Tele List

data class TeleList(val entrySummaries: List<TeleEntrySummary>, val pageInfo: PageInfo) {
    data class PageInfo(val min: Int, val current: Int, val max: Int)
}

class TeleListQuery private constructor(override val url: String) : TeleQuery<TeleList> {
    override fun parseDocument(document: Document): TeleList {
        try {
            val entries = mutableListOf<TeleEntrySummary>()

            try {
                document.selectFirst(".row.large-columns-3.medium-columns-.small-columns-1.row-masonry")!!
                    .select("div.col.post-item").forEach { element ->
                        try {
                            val imageUrl =
                                element.selectFirst("img.attachment-medium.size-medium.wp-post-image")!!.attr("src")
                            val textElement = element.selectFirst("h5.post-title.is-large a")!!
                            val title = textElement.text()
                            val url = textElement.attr("href")
                            val name = url.substring(24, url.length - 1)

                            TeleLog.d(TAG, "解析", "实体概况", "【标题：$title, Id：$name, 图片链接: $imageUrl】")
                            entries.add(TeleEntrySummary(title = TeleLink(url, title), id = name, imageUrl = imageUrl))
                        } catch (e: Exception) {
                            TeleLog.e(TAG, "解析实体概况失败", e)
                        }
                    }
            } catch (e: NullPointerException) {
                TeleLog.e(TAG, "遇到可能的查询无结果情况，应为NullPtr", e)
                throw TeleException("查询无结果")
            }

            val pager = document.selectFirst(".page-numbers.nav-pagination")

            val pageInfo = if (pager == null) TeleList.PageInfo(1, 1, 1) // 扣一送火麒麟
            else {
                // 防止第一个元素是上一页按钮
                val firstNumEle = pager.firstElementChild()!!.let {
                    if (it.firstElementChild()!!.hasAttr("aria-label")) pager.child(1)
                    else it
                }

                // 防止最后一个元素是下一页按钮
                val lastNumEle = pager.child(pager.childrenSize() - 1).let {
                    if (it.firstElementChild()!!.hasAttr("aria-label")) pager.child(pager.childrenSize() - 2)
                    else it
                }

                val min = firstNumEle.firstElementChild()!!.text().toInt()

                // 当前页码
                val current = pager.selectFirst("span.current")!!.text().toInt()


                val max = lastNumEle.firstElementChild()!!.text().toInt()

                TeleList.PageInfo(min, current, max)
            }

            return TeleList(entries, pageInfo)
        } catch (e: Exception) {
            throw TeleException("Failed to parse list: ${e.stackTraceToString()}")
        }
    }

    companion object {
        private const val TAG = "TeleListQuery"

        fun build(
            categories: TeleCategoryRes? = null, tag: TeleTagRes? = null,
            keyword: TeleKeywordRes? = null, page: Int = 1,
        ): TeleListQuery {
            var url = TELE_BASE_URL

            // 如果有关键词，优先处理关键词逻辑
            if (keyword != null) {
                if (page > 1) url += "page/$page/"

                url += "?s=${keyword.data.encoded}"
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

            TeleLog.d(TAG, "创建查询-列表：$url")

            return TeleListQuery(url)
        }
    }
}

// Tele Entry

data class TeleEntrySummary(
    val title: TeleLink,
    val id: String,
    val imageUrl: String
)

data class TeleEntry(
    val id: String,
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

class TeleEntryQuery private constructor(
    override val url: String,
    private val id: String //
) : TeleQuery<TeleEntry> {
    override fun parseDocument(document: Document): TeleEntry {
        try {
            // 标题
            val title = document.selectFirst(".entry-title")!!.text()

            TeleLog.d(TAG, "解析-标题：$title")

            // 分类
            val categories = mutableListOf<TeleLink>()
            document.selectFirst(".entry-category.is-xsmall")!!.select("a").forEach {
                val url = it.attr("href")
                val text = it.text()

                TeleLog.d(TAG, "解析-分类：【$text：$url】")

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

                    TeleLog.d(TAG, "解析-元数据：${pair[0]}")

                    when (pair[0]) {
                        "Cosplayer" -> cosplayer = strong.parseLinkableStrong(pair[1])

                        "Character" -> character = strong.parseLinkableStrong(pair[1])

                        "Appear In" -> appearIn = strong.parseLinkableStrong(pair[1])

                        "Photos" -> photos = pair[1]

                        "File Size" -> fileSize = pair[1]

                        "Unzip Password:" -> unzipPassword = it.selectFirst("input")!!.attr("value")

                        else -> TeleLog.d(TAG, "解析-找到未知元数据：${pair[0]}")
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
                        TeleLog.d(TAG, "解析-下载链接：【${buttonText}：$it】")
                        downloadLinks.add(TeleLink(url = it, text = buttonText))
                    }
                }
            }

            // 视频
            var video: TeleLink? = null
            document.select("iframe").forEach { iframe ->
                val src = iframe.attr("src")

                if (!src.startsWith("https://cossora.stream/embed/")) return@forEach

                TeleLog.d(TAG, "解析-视频：$src")

                // TODO：整合已有解析能力
                video = TeleLink(url = src)
            }

            // 图片
            val images = mutableListOf<TeleLink>()
            document.select(".gallery-item").forEach {
                val src = it.firstElementChild()!!.firstElementChild()!!.attr("href")

                TeleLog.d(TAG, "解析-图片：$src")

                images.add(TeleLink(url = src))
            }

            val teleEntry = TeleEntry(
                id = id,
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
        private const val TAG = "TeleEntryQuery"

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

        fun build(entryId: TeleEntryRes): TeleEntryQuery {
            val url = "$TELE_BASE_URL${entryId.data}/"

            TeleLog.d(TAG, "创建查询：$url")

            return TeleEntryQuery(url, entryId.data)
        }
    }
}

// Tele Video

data class TeleVideo(val videoUrl: String)

class TeleVideoQuery private constructor(override val url: String, private val videoType: TeleVideoRes.VideoType) :
    TeleQuery<TeleVideo> {
    override fun parseDocument(document: Document): TeleVideo = when (videoType) {
        TeleVideoRes.VideoType.COSSORA -> {
            val chosenOne = document.select("script")[10].data().lines()

            var url = chosenOne[3].run { substring(26, length - 2) }
            var key = chosenOne[6].run { substring(56, length - 3) }

            /*chosenOne.lines().forEachIndexed { index, line ->
                println("第 $index 行啊 $index 行：$line")

                // 第3，6行

                when {
                    line.startsWith("        const videoURL ") -> url = line.substring(26, line.length - 2)
                    line.startsWith("        const videoURLDescrypt ") -> key = line.substring(56, line.length - 3)
                }
            }*/

            val gays = "Url：$url，Key：$key"
            TeleLog.d(TAG, gays)

            if (url.isNotBlank() && key.isNotBlank()) {
                try {
                    TeleVideo(TeleUtils.decryptCossoraLink(url, key))
                } catch (e: Exception) {
                    throw IllegalStateException("Can't decrypt video link: $gays", e)
                }
            } else throw IllegalStateException("The url or key is empty: $gays")
        }

        else -> throw NotImplementedError()
    }

    override fun configureRequest(builder: HttpRequestBuilder) = builder.run {
        when (videoType) {
            TeleVideoRes.VideoType.COSSORA -> header(HttpHeaders.Referrer, TELE_BASE_URL)

            else -> throw NotImplementedError()
        }
    }

    companion object {
        private const val TAG = "TeleVideoQuery"

        fun build(videoRes: TeleVideoRes): TeleVideoQuery {
            val url = videoRes.videoType.baseUrl + videoRes.data
            val videoType = videoRes.videoType

            TeleLog.d(TAG, "创建查询：$url，$videoType")

            return TeleVideoQuery(url, videoType)
        }
    }
}
