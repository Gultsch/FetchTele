package lib.fetchtele

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.isSuccess
import org.jsoup.Jsoup

// TODO：其实应该允许直接传入一个 Http客户端
class TeleFetcherConfig(
    val ktorHttpClient: HttpClient,
    val teleLogger: ((TeleLogUtils.Log) -> Unit)? = null
)

class TeleFetcher(teleFetcherConfig: TeleFetcherConfig) {
    private val httpClient = teleFetcherConfig.ktorHttpClient

    init {
        teleFetcherConfig.teleLogger?.let { TeleLogUtils.setLogger(it) }
    }

    suspend fun <RESULT_TYPE> fetch(query: TeleQuery<RESULT_TYPE>): TeleResult<RESULT_TYPE> {
        return try {
            val url = query.url
            val response = httpClient.get(url) { query.configureRequest(this) }

            if (response.status.isSuccess()) {
                try {
                    // TODO：以后没准引入让query自行处理返回的机制
                    val bodyText = response.bodyAsText()
                    val document = Jsoup.parse(bodyText)

                    val result = query.parseDocument(document)

                    TeleResult.Success(result)
                } catch (e: Exception) {
                    TeleResult.Failure(TeleException("Failed to parse response: ${e.stackTraceToString()}"))
                }
            } else TeleResult.Failure(TeleException("HttpClient failed to fetch (${response.status.value}): $url"))
        } catch (e: Exception) {
            TeleResult.Failure(TeleException("Failed to fetch: ${e.stackTraceToString()}"))
        }
    }

    companion object {
    }
}

class TeleException(message: String) : Exception("FetchTele > Error: $message")