package lib.fetchtele

object TeleUtils {
    private val teleResParsers = mutableListOf<TeleResParser<*, *>>()

    // TODO：弱智，仅当Res类被加载时才能触发其自注册，呃呃
    internal fun <RES_TYPE, RAW_TYPE> registerTeleResParser(parser: TeleResParser<RES_TYPE, RAW_TYPE>) {
        println("工具-（内部方法）解析器注册：${parser.javaClass}")

        teleResParsers.add(parser)
    }

    // 尝试解析为资源，返回解析成功的资源列表
    fun Any.tryParseToTeleRes(): List<TeleRes<*>> {
        val resList = mutableListOf<TeleRes<*>>()

        println("工具-尝试解析为资源：$this")
        teleResParsers.forEachIndexed { index, it ->
            try {
                println("工具-解析（${index}）：${it.javaClass}")

                val res = it.call<TeleRes<*>>("parse", this)!! // 比较奇技淫巧的
                resList.add(res)
            } catch (e: Exception) {
                println("工具-解析失败（${index}）：${e.stackTraceToString()}")
            }
        }

        return resList
    }

    internal fun <T> Any.call(methodName: String, vararg args: Any?): T? {
        // 总体比较奇技淫巧的，纯纯的反射
        // 或不可调用Unit方法（？），没试过
        return try {
            println("工具-反射调用：$methodName")
            val method =
                this::class.java.methods.firstOrNull { it.name == methodName && it.parameterCount == args.size }
                    ?: throw NoSuchMethodException("Method $methodName with ${args.size} parameters not found")

            method.invoke(this, *args) as T
        } catch (e: Exception) {
            println("工具-反射调用失败（$methodName）：${e.stackTraceToString()}")
            null
        }
    }
}