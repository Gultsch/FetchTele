import kotlinx.coroutines.runBlocking
import lib.fetchtele.TeleCategoryRes
import lib.fetchtele.TeleEntryQuery
import lib.fetchtele.TeleFetcher
import lib.fetchtele.TeleFetcherConfig
import lib.fetchtele.TeleKeywordRes
import lib.fetchtele.TeleListQuery
import lib.fetchtele.TeleResult
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test

@TestMethodOrder(MethodOrderer.MethodName::class)
class TeleTest {
    val teleFetcher = TeleFetcher(
        TeleFetcherConfig(
            proxy = "http://127.0.0.1:7890" // 此处代理配置为开发者测试用
        )
    )

    @Test
    fun test01() = runBlocking {
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
            }
        }
    }

    @Test
    fun test02() = runBlocking {
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
            }
        }
    }

    @Test
    fun test03() = runBlocking {
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
            }
        }
    }

    @Test
    fun test04() = runBlocking {
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
            }
        }
    }
}