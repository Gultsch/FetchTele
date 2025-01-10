import kotlinx.coroutines.runBlocking
import lib.fetchtele.TeleCategories
import lib.fetchtele.TeleEntryQuery
import lib.fetchtele.TeleFetcher
import lib.fetchtele.TeleFetcherConfig
import lib.fetchtele.TeleListQuery
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
        val teleList = teleFetcher.fetch(TeleListQuery())

        assert(teleList.isNotEmpty())
        teleList.forEach {
            println(it)
        }
    }

    @Test
    fun test02() = runBlocking {
        println("请求特定列表：分类：Nude，第二页")
        val teleList = teleFetcher.fetch(TeleListQuery(categories = TeleCategories.NUDE, page = 2))

        assert(teleList.isNotEmpty())
        teleList.forEach {
            println(it)
        }
    }

    @Test
    fun test03() = runBlocking {
        println("请求特定列表：关键词：向日")
        val teleList = teleFetcher.fetch(TeleListQuery(keyword = "向日"))

        assert(teleList.isNotEmpty())
        teleList.forEach {
            println(it)
        }
    }

    @Test
    fun test04() = runBlocking {
        println("访问特定作品：raiden-shogun-38")

        val teleEntry = teleFetcher.fetch(TeleEntryQuery(entryId = "raiden-shogun-38"))

        println(teleEntry)
    }
}