import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import lib.fetchtele.TELE_BASE_URL
import lib.fetchtele.TeleCategoryRes
import lib.fetchtele.TeleEntryQuery
import lib.fetchtele.TeleEntryRes
import lib.fetchtele.TeleFetcher
import lib.fetchtele.TeleFetcherConfig
import lib.fetchtele.TeleKeywordRes
import lib.fetchtele.TeleListQuery
import lib.fetchtele.TeleResult
import lib.fetchtele.TeleTagRes
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@TestMethodOrder(MethodOrderer.MethodName::class)
class TeleTest {
    val teleFetcher = TeleFetcher(
        TeleFetcherConfig(
            ktorEngine = CIO,
            // enableDebugLog = true,
            // httpProxyUrl = "http://127.0.0.1:7890"
        )
    )

    // TODO：有时间整点和Result以及实体有关的测试（虽然开发者代码写得好，高阶测试都能过，这点恐怕不在话下罢）

    // 测试Res的构建和解析、Query的构建
    @Test
    fun test01() {
        println("测试构建：TeleCategoryRes")
        assertEquals("cosplay", TeleCategoryRes("cosplay").data) // 测试正确构建

        println("测试解析器：TeleCategoryRes")
        assertFails { TeleCategoryRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleCategoryRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleCategoryRes.parse("${TELE_BASE_URL}category//") } // 测试报错：不含有数据内容
        assertEquals(
            TeleCategoryRes.NUDE.data,
            TeleCategoryRes.parse("${TELE_BASE_URL}category/nude/").data
        ) // 测试正确解析
        assertEquals(
            TeleCategoryRes.NO_NUDE.data,
            TeleCategoryRes.parse("${TELE_BASE_URL}category/no-nude/page/2/").data
        ) // 测试正确解析

        println("测试构建：TeleTagRes")
        assertEquals("blue-archive", TeleTagRes("blue-archive").data) // 测试正确构建

        println("测试解析器：TeleTagRes")
        assertFails { TeleTagRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleTagRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleTagRes.parse("${TELE_BASE_URL}tag//") } // 测试报错：不含有数据内容
        assertEquals(
            TeleTagRes.FINAL_FANTASY.data,
            TeleTagRes.parse("${TELE_BASE_URL}tag/final-fantasy/").data
        ) // 测试正确解析
        assertEquals(
            TeleTagRes.ARKNIGHTS.data,
            TeleTagRes.parse("${TELE_BASE_URL}tag/arknights/page/2/").data
        ) // 测试正确解析

        println("测试构建：TeleKeywordRes")
        assertEquals("凉", TeleKeywordRes("凉").data) // 测试正确构建
        assertEquals("%E5%96%B5%E5%B0%8F", TeleKeywordRes("喵小").encoded) // 测试正确编码

        println("测试解析器：TeleKeywordRes")
        assertFails { TeleKeywordRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleKeywordRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleKeywordRes.parse("${TELE_BASE_URL}?=") } // 测试报错：不含有数据内容
        assertEquals("向日", TeleKeywordRes.parse("${TELE_BASE_URL}?s=%E5%90%91%E6%97%A5").data) // 测试正确解析
        assertEquals(
            "咬一口",
            TeleKeywordRes.parse("${TELE_BASE_URL}page/2/?s=%E5%92%AC%E4%B8%80%E5%8F%A3").data
        ) // 测试正确解析
        assertEquals("%E9%82%A6", TeleKeywordRes.parse("${TELE_BASE_URL}page/2/?s=%E9%82%A6").encoded) // 测试正确解析

        println("测试构建：TeleEntryRes")
        assertEquals("mita", TeleEntryRes("mita").data) // 测试正确构建

        println("测试解析器：TeleEntryRes")
        assertFails { TeleEntryRes.parse("乱七八糟") } // 测试报错：不是合法的URL
        assertFails { TeleEntryRes.parse(TELE_BASE_URL) } // 测试报错：Url不含有数据部分
        assertFails { TeleEntryRes.parse("$TELE_BASE_URL/") } // 测试报错：不含有数据内容
        assertFails { TeleEntryRes.parse("${TELE_BASE_URL}tag/dd/") } // 测试报错：拿非实体URL解析
        assertEquals("mita", TeleEntryRes.parse("${TELE_BASE_URL}mita/").data) // 测试正确解析

        println("测试构建：TeleListQuery")
        assertEquals(TELE_BASE_URL, TeleListQuery.build().url) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}category/nude/",
            TeleListQuery.build(categories = TeleCategoryRes.NUDE).url
        ) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}tag/final-fantasy/",
            TeleListQuery.build(tag = TeleTagRes.FINAL_FANTASY).url
        ) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}tag/final-fantasy/page/2/",
            TeleListQuery.build(tag = TeleTagRes.FINAL_FANTASY, page = 2).url
        ) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}?s=%E9%82%A6",
            TeleListQuery.build(keyword = TeleKeywordRes("邦")).url
        ) // 测试正确构建
        assertEquals(
            "${TELE_BASE_URL}page/2/?s=%E5%90%91%E6%97%A5",
            TeleListQuery.build(keyword = TeleKeywordRes("向日"), page = 2).url
        ) // 测试正确构建
    }

    @Test
    fun test02() = runBlocking {
        println("请求首页列表")
        val result = teleFetcher.fetch(TeleListQuery.build())

        when (result) {
            is TeleResult.Success -> {
                val teleList = result.data

                assert(teleList.isNotEmpty()) { "列表为空，显然不对" }
                teleList.forEach {
                    println(it)
                }

                println("请求成功")
            }

            is TeleResult.Failure -> {
                println("请求失败：${result.error.message}")
                throw result.error
            }
        }
    }

    @Test
    fun test03() = runBlocking {
        println("请求特定列表：分类：Nude，第二页")
        val result = teleFetcher.fetch(TeleListQuery.build(categories = TeleCategoryRes.NUDE, page = 2))

        when (result) {
            is TeleResult.Success -> {
                val teleList = result.data

                assert(teleList.isNotEmpty()) { "列表为空，显然不对" }
                teleList.forEach {
                    println(it)
                }

                println("请求成功")
            }

            is TeleResult.Failure -> {
                println("请求失败：${result.error.message}")
                throw result.error
            }
        }
    }

    @Test
    fun test04() = runBlocking {
        println("请求特定列表：关键词：向日")
        val result = teleFetcher.fetch(TeleListQuery.build(keyword = TeleKeywordRes("向日")))

        when (result) {
            is TeleResult.Success -> {
                val teleList = result.data

                assert(teleList.isNotEmpty())
                teleList.forEach {
                    println(it)
                }

                println("请求成功")
            }

            is TeleResult.Failure -> {
                println("请求失败：${result.error.message}")
                throw result.error
            }
        }
    }

    @Test
    fun test05() = runBlocking {
        println("请求特定实体：raiden-shogun-38")

        val result = teleFetcher.fetch(TeleEntryQuery.build(entryId = "raiden-shogun-38"))

        when (result) {
            is TeleResult.Success -> {
                val teleEntry = result.data
                println(teleEntry)
                println("请求成功")
            }

            is TeleResult.Failure -> {
                println("请求失败：${result.error.message}")
                throw result.error
            }
        }
    }
}