package lib.fetchtele

import io.ktor.client.*
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.http
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup

data class TeleFetcherConfig(
    val proxy: String? = null,
    val timeout: Long = 10_000,
)

class TeleFetcher(teleFetcherConfig: TeleFetcherConfig = TeleFetcherConfig()) {
    val httpClient = HttpClient(CIO) {
        // 如果有代理配置，就设置代理
        teleFetcherConfig.proxy?.let {
            engine {
                proxy = ProxyBuilder.http(it)
            }
        }

        // 设置请求超时时间
        install(HttpTimeout) {
            requestTimeoutMillis = teleFetcherConfig.timeout
        }
    }

    init {
    }

    suspend fun <RESULT_TYPE> fetch(query: TeleQuery<RESULT_TYPE>): TeleResult<RESULT_TYPE> {
        return try {
            val url = query.url
            val response = httpClient.get(url)

            if (response.status.value != 200) {
                TeleResult.Failure(TeleException("HttpClient failed to fetch (${response.status.value}): $url"))
            } else {
                try {
                    val bodyText = response.bodyAsText()
                    val document = Jsoup.parse(bodyText)
                    val result = query.parseDocument(document)

                    TeleResult.Success(result)
                } catch (e: Exception) {
                    TeleResult.Failure(TeleException("Failed to parse response: ${e.stackTraceToString()}"))
                }
            }
        } catch (e: Exception) {
            TeleResult.Failure(TeleException("Failed to fetch: ${e.stackTraceToString()}"))
        }
    }

    companion object {
    }
}

class TeleException(message: String) : Exception("FetchTele > Error: $message")