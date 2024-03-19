package handler

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.json.simple.JSONObject
import resultList
import java.io.IOException
import java.net.InetSocketAddress
import java.util.HashMap

class HttpServer(host: String, port: Int) {
    private val DEFAULT_HOSTNAME = "0.0.0.0"
    private val DEFAULT_PORT = 80
    private val DEFAULT_BACKLOG = 0;
    private var server: HttpServer? = null
    init {
        createServer(host, port)
    }

    @Throws(IOException::class)
    private fun createServer(host: String, port: Int) {
        // HTTP Server 생성
        server = HttpServer.create(InetSocketAddress(host, port), DEFAULT_BACKLOG)
        // HTTP Server Context 설정
        if (server != null) {
            server!!.createContext("/", RootHandler())
            server!!.createContext("/sphResult", SPHResultHandler())
        }
    }

    /**
     * 서버 실행
     */
    fun start() {
        server?.start()
    }

    /**
     * 서버 중지
     */
    fun stop(delay: Int) {
        server?.stop(delay)
    }

    inner class RootHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = "Hello World!"
            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
            exchange.close()
        }
    }

    inner class SPHResultHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = JSONObject()
            response["result"] = JSONObject(resultList)
            val headers = exchange.responseHeaders
            headers.add("Content-Type", "application/json;charset=UTF-8")
            // response.toJSONString()을 사용하여 응답을 문자열로 변환한 뒤 "\n" 문자를 제거합니다.
            val responseString = response.toJSONString().replace("\n", "")
            // Content-Length를 수동으로 설정할 필요 없이, 바이트 배열의 길이를 사용하여 자동으로 설정합니다.
            exchange.sendResponseHeaders(200, responseString.toByteArray(Charsets.UTF_8).size.toLong())
            val os = exchange.responseBody
            os.write(responseString.toByteArray(Charsets.UTF_8))
            os.close()
        }
    }
    fun getRequestQuery(exchange: HttpExchange): Map<String, String> {
        val query = exchange.requestURI.query
        val queryMap = HashMap<String, String>()
        if (query != null) {
            val querySplit = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (queryParam in querySplit) {
                val entry = queryParam.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (entry.size > 1) {
                    queryMap[entry[0]] = entry[1]
                } else {
                    queryMap[entry[0]] = ""
                }
            }
        }
        return queryMap
    }
}