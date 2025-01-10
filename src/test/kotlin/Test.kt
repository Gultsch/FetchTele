import kotlinx.coroutines.runBlocking
import lib.fetchtele.TeleEntryQuery
import lib.fetchtele.TeleFetcher
import lib.fetchtele.TeleFetcherConfig
import lib.fetchtele.TeleListQuery
import kotlin.test.Test
import kotlin.test.assertNotNull

class TeleTest {
    val teleFetcher = TeleFetcher(TeleFetcherConfig(proxy = "http://127.0.0.1:7890"))

    @Test
    fun testList() = runBlocking {
        println("访问首页列表")
        var teleList = teleFetcher.fetch(TeleListQuery())

        assert(teleList.isNotEmpty())
        teleList.forEach {
            println(it)
        }

        println("访问特定列表：分类：Nude，第二页")
        assert(teleList.isNotEmpty())
        teleList = teleFetcher.fetch(TeleListQuery(categories = "nude", page = 2))

        teleList.forEach {
            println(it)
        }
    }

    @Test
    fun testEntry() = runBlocking {
        println("访问特定作品：raiden-shogun-38")

        val teleEntry = teleFetcher.fetch(TeleEntryQuery(entryId = "raiden-shogun-38"))

        println(teleEntry)
    }
}