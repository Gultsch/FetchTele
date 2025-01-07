import kotlinx.coroutines.runBlocking
import lib.fetchtele.TeleFetcher
import lib.fetchtele.TeleType
import lib.fetchtele.TeleUrl
import org.junit.jupiter.api.Test

class Test {
    @Test
    fun test1() = runBlocking {
        val teleFetcher = TeleFetcher()
        val teleResult = teleFetcher.fetch()

        teleResult.getTeleEntrySummaries().forEach {
            println(it)
        }

        teleFetcher.fetch(TeleType(entryName = "sora-kasugano-11"))

        return@runBlocking
    }
}