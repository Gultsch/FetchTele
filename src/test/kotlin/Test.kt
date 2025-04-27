import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import lib.fetchtele.TELE_BASE_URL
import lib.fetchtele.TeleCategoryRes
import lib.fetchtele.TeleEntry
import lib.fetchtele.TeleEntryQuery
import lib.fetchtele.TeleEntryRes
import lib.fetchtele.TeleFetcher
import lib.fetchtele.TeleFetcherConfig
import lib.fetchtele.TeleKeywordRes
import lib.fetchtele.TeleList
import lib.fetchtele.TeleListQuery
import lib.fetchtele.TeleLogUtils
import lib.fetchtele.TeleResult
import lib.fetchtele.TeleTagRes
import lib.fetchtele.TeleVideoQuery
import lib.fetchtele.TeleVideoRes
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@TestMethodOrder(MethodOrderer.MethodName::class)
class TeleTest {
    companion object {
        private const val TAG = "TeleTest"
    }

    val teleFetcher = TeleFetcher(
        TeleFetcherConfig(
            ktorHttpClient = HttpClient(CIO) {
                // 设置请求超时时间
                install(HttpTimeout) {
                    requestTimeoutMillis = 10_000
                }
            }
        )
    )

    // TODO：有时间整点和Result以及实体有关的测试（虽然开发者代码写得好，高阶测试都能过，这点恐怕不在话下罢）

    // 测试Res的构建和解析、Query的构建
    @Test
    fun test01() {
        TeleLogUtils.i(TAG, "测试构建：TeleCategoryRes")
        assertEquals("cosplay", TeleCategoryRes("cosplay").data) // 测试正确构建

        TeleLogUtils.i(TAG, "测试解析器：TeleCategoryRes")
        assertFails { TeleCategoryRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleCategoryRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleCategoryRes.parse("${TELE_BASE_URL}category//") } // 测试报错：不含有数据内容
        assertEquals("nude", TeleCategoryRes.parse("${TELE_BASE_URL}category/nude/").data) // 测试正确解析
        assertEquals("no-nude", TeleCategoryRes.parse("${TELE_BASE_URL}category/no-nude/page/2/").data) // 测试正确解析

        TeleLogUtils.i(TAG, "测试构建：TeleTagRes")
        assertEquals("blue-archive", TeleTagRes("blue-archive").data) // 测试正确构建

        TeleLogUtils.i(TAG, "测试解析器：TeleTagRes")
        assertFails { TeleTagRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleTagRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleTagRes.parse("${TELE_BASE_URL}tag//") } // 测试报错：不含有数据内容
        assertEquals("final-fantasy", TeleTagRes.parse("${TELE_BASE_URL}tag/final-fantasy/").data) // 测试正确解析
        assertEquals("arknights", TeleTagRes.parse("${TELE_BASE_URL}tag/arknights/page/2/").data) // 测试正确解析

        // Sessions的不好搞

        TeleLogUtils.i(TAG, "测试构建：TeleKeywordRes")
        assertEquals("凉", TeleKeywordRes("凉").data) // 测试正确构建
        assertEquals("%E5%96%B5%E5%B0%8F", TeleKeywordRes("喵小").encoded) // 测试正确编码

        TeleLogUtils.i(TAG, "测试解析器：TeleKeywordRes")
        assertFails { TeleKeywordRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleKeywordRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleKeywordRes.parse("${TELE_BASE_URL}?=") } // 测试报错：不含有数据内容
        assertEquals("向日", TeleKeywordRes.parse("${TELE_BASE_URL}?s=%E5%90%91%E6%97%A5").data) // 测试正确解析
        assertEquals(
            "咬一口", TeleKeywordRes.parse("${TELE_BASE_URL}page/2/?s=%E5%92%AC%E4%B8%80%E5%8F%A3").data
        ) // 测试正确解析
        assertEquals("%E9%82%A6", TeleKeywordRes.parse("${TELE_BASE_URL}page/2/?s=%E9%82%A6").encoded) // 测试正确解析

        TeleLogUtils.i(TAG, "测试构建：TeleEntryRes")
        assertEquals("mita", TeleEntryRes("mita").data) // 测试正确构建

        TeleLogUtils.i(TAG, "测试解析器：TeleEntryRes")
        assertFails { TeleEntryRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleEntryRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleEntryRes.parse("$TELE_BASE_URL/") } // 测试报错：不含有数据内容
        assertFails { TeleEntryRes.parse("${TELE_BASE_URL}tag/dd/") } // 测试报错：拿非实体URL解析
        assertEquals("mita", TeleEntryRes.parse("${TELE_BASE_URL}mita/").data) // 测试正确解析

        TeleLogUtils.i(TAG, "测试构建：TeleVideoRes")
        val instance = TeleVideoRes("114514-senpai", TeleVideoRes.VideoType.COSSORA)
        assertEquals("114514-senpai", instance.data) //构建测试：Data不匹配
        assertEquals(TeleVideoRes.VideoType.COSSORA, instance.videoType) //构建测试：Type不匹配

        TeleLogUtils.i(TAG, "测试解析器：TeleVideoRes")
        assertFails { TeleVideoRes.parse("https://some.other.domain/video/12345") } // 测试报错：不支持的域
        assertFails { TeleVideoRes.parse("这根本不是URL") } // 测试报错：完全无效的字符串
        assertFails { TeleVideoRes.parse(TeleVideoRes.VideoType.COSSORA.baseUrl) } // 测试报错：只有Base URL
        assertFails { TeleVideoRes.parse("${TeleVideoRes.VideoType.COSSORA.baseUrl}invalid/114514/yaju") } //测试报错：标识符含"/"
        assertEquals(
            "114514-senpai", TeleVideoRes.parse("${TeleVideoRes.VideoType.COSSORA.baseUrl}114514-senpai").data
        ) // 测试正确解析：Data提取错误

        TeleLogUtils.i(TAG, "测试构建：TeleListQuery")
        assertEquals(TELE_BASE_URL, TeleListQuery.build().url) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}category/nude/",
            TeleListQuery.build(categories = TeleCategoryRes("nude")).url
        ) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}tag/final-fantasy/",
            TeleListQuery.build(tag = TeleTagRes("final-fantasy")).url
        ) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}tag/final-fantasy/page/2/",
            TeleListQuery.build(tag = TeleTagRes("final-fantasy"), page = 2).url
        ) // 测试正确构建
        assertEquals("${TELE_BASE_URL}?s=%E9%82%A6", TeleListQuery.build(keyword = TeleKeywordRes("邦")).url) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}page/2/?s=%E5%90%91%E6%97%A5",
            TeleListQuery.build(keyword = TeleKeywordRes("向日"), page = 2).url
        ) // 测试正确构建

        TeleLogUtils.i(TAG, "测试构建：TeleEntryQuery")
        assertEquals("${TELE_BASE_URL}mita/", TeleEntryQuery.build(entryId = TeleEntryRes("mita")).url)

        TeleLogUtils.i(TAG, "测试构建：TeleVideoQuery")
        assertEquals(
            "${TeleVideoRes.VideoType.COSSORA.baseUrl}114514-senpai",
            TeleVideoQuery.build(TeleVideoRes("114514-senpai", TeleVideoRes.VideoType.COSSORA)).url
        )
    }

    @Test
    fun test02() = runBlocking {
        TeleLogUtils.i(TAG, "请求首页列表")
        val result = teleFetcher.fetch(TeleListQuery.build())

        result.checkList()
    }

    @Test
    fun test03() = runBlocking {
        TeleLogUtils.i(TAG, "请求特定列表：分类：Nude，第二页")
        val result = teleFetcher.fetch(TeleListQuery.build(categories = TeleCategoryRes("nude"), page = 2))

        result.checkList()
    }

    @Test
    fun test04() = runBlocking {
        TeleLogUtils.i(TAG, "请求特定列表：关键词：rika，第二页")
        val result = teleFetcher.fetch(TeleListQuery.build(keyword = TeleKeywordRes("rika"), page = 2))

        result.checkList()
    }

    @Test
    fun test05() = runBlocking {
        TeleLogUtils.i(TAG, "请求特定列表：关键词：向日")
        val result = teleFetcher.fetch(TeleListQuery.build(keyword = TeleKeywordRes("向日")))

        result.checkList()
    }

    fun TeleResult<TeleEntry>.checkEntry() = when (this) {
        is TeleResult.Success -> {
            val teleEntry = data
            TeleLogUtils.i(TAG, "请求成功", teleEntry)
        }

        is TeleResult.Failure -> {
            TeleLogUtils.e(TAG, "请求失败", error)

            throw error
        }
    }

    @Test
    fun test06() = runBlocking {
        TeleLogUtils.i(TAG, "请求特定实体：mashu-kyrielight-dancer")

        val result = teleFetcher.fetch(TeleEntryQuery.build(entryId = TeleEntryRes("mashu-kyrielight-dancer")))

        result.checkEntry()
    }

    @Test
    fun test07() = runBlocking {
        TeleLogUtils.i(TAG, "请求特定实体：jk-7（测试错版兼容性）")

        val result = teleFetcher.fetch(TeleEntryQuery.build(entryId = TeleEntryRes("jk-7")))

        result.checkEntry()
    }

    @Test
    fun test08() = runBlocking {
        TeleLogUtils.i(TAG, "请求特定实体：mai-shiranui-7")

        val result = teleFetcher.fetch(TeleEntryQuery.build(entryId = TeleEntryRes("mai-shiranui-7")))

        result.checkEntry()
    }

    @Test
    fun test09() = runBlocking {
        TeleLogUtils.i(TAG, "测试视频解析")

        val result =
            teleFetcher.fetch(TeleVideoQuery.build(videoRes = TeleVideoRes.parse("https://cossora.stream/embed/bcbe1033-02ff-4f58-8eb4-2670e07b38a4")))

        when (result) {
            is TeleResult.Success -> {
                TeleLogUtils.i(TAG, "请求成功，请品鉴：${result.data}")
            }

            is TeleResult.Failure -> {
                TeleLogUtils.e(TAG, "请求失败", result.error)
                throw result.error
            }
        }
    }

    fun TeleResult<TeleList>.checkList() = when (this) {
        is TeleResult.Success -> {
            val teleList = data

            assert(teleList.entrySummaries.isNotEmpty()) { "列表为空，显然不对" }
            teleList.entrySummaries.forEach {
                TeleLogUtils.i(TAG, it)
            }

            TeleLogUtils.i(TAG, "请求成功", "列表页信息：${teleList.pageInfo}")
        }

        is TeleResult.Failure -> {
            TeleLogUtils.e(TAG, "请求失败", error)
            throw error
        }
    }
}