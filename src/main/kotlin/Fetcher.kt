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

    suspend fun <RESULT_TYPE> fetch(query: TeleQuery<RESULT_TYPE>): RESULT_TYPE {
        try {
            val url = query.getUrl()

            val response = httpClient.get(url)

            if (response.status.value != 200) throw TeleException("HttpClient failed to fetch: ${url}.")

            try {
                val bodyText= response.bodyAsText()

                // println("解析-文档：$bodyText")

                val document = Jsoup.parse(bodyText)

                val result = query.parseDocument(document)

                return result
            } catch (e: Exception) {
                throw TeleException("Failed to parse response: ${e.stackTraceToString()}")
            }
        } catch (e: Exception) {
            throw TeleException("Failed to fetch: ${e.stackTraceToString()}")
        }
    }

    companion object {
    }
}

class TeleException(message: String) : Exception("FetchTele > Error: $message")