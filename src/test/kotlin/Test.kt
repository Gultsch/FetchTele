import kotlinx.coroutines.runBlocking
import lib.fetchtele.TeleFetcher
import org.junit.jupiter.api.Test

class Test {
    @Test
    fun test1() = runBlocking {
        val teleFetcher = TeleFetcher()
        val teleResult = teleFetcher.fetch()

        teleResult.getTeleEntrySummaries().forEach { it ->
            println(it.title.text)
        }
    }
}