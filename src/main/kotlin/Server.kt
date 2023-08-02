import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Integer.max
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.sqrt


class Server {
    private val DEFAULT_HOSTNAME = "localhost"
    private val DEFAULT_PORT = 8080
    private val DEFAULT_BACKLOG = 0
    private var server: HttpServer? = null

    constructor(host: String, port: Int) {
        createServer(host, port)
    }

    @Throws(IOException::class)
    private fun createServer(host: String, port: Int) {
        // HTTP Server 생성
        server = HttpServer.create(InetSocketAddress(host, port), DEFAULT_BACKLOG)
        // HTTP Server Context 설정
        if (server != null) {
            server!!.createContext("/", RootHandler())
            server!!.createContext("/peopleCount", PeopleCountHandler())
            server!!.createContext("/placeSize", PlaceSizeHandler())
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

    inner class PeopleCountHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = JSONObject()
            val peopleCount = getPeopleCount(getRequestQuery(exchange)["place"]!!)
            response["peopleCountMin"] = peopleCount.first
            response["peopleCountMax"] = peopleCount.second
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

    inner class PlaceSizeHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = JSONObject()
            val place = getRequestQuery(exchange)["place"]!!
            val placeSize = getPlaceSize(place)
            response["place"] = place
            response["placeSize"] = placeSize
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

    inner class SPHResultHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = JSONObject();
            val place = getRequestQuery(exchange)["place"]!!
            val result = getSPHResult(place)
            response["place"] = place
            response["MeanSPHResult"] = result.first
            response["MaxSPHResult"] = result.second
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

        private fun getSPHResult(place: String): Pair<Double, Double> {
            testRequest()
            val size = getPlaceSize(place)
            if (size == 0) return Pair(0.0, 0.0)
            val width = sqrt(size.toDouble()).toInt()
            val height = sqrt(size.toDouble()).toInt()
            val meanPeopleCount = getMeanPeopleCount(place)
            val maxPeopleCount = getMaxPeopleCount(place)
            val meanSPH = floor(SPH(meanPeopleCount, width, height) * 100) / 100
            val maxSPH = floor(SPH(maxPeopleCount, width, height)* 100) / 100
            return Pair(meanSPH, maxSPH)
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

    fun getPeopleCount(place: String): Pair<Int, Int> {
        val url = URL(
            "http://openapi.seoul.go.kr:8088/4e574f4441796f7537316758474875/json/citydata_ppltn/1/5/${
                Place.PLACE_CD.list[Place.PLACE_NM.list.indexOf(place.replace("+", " "))]
            }"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val inputStream = conn.inputStream
        val rd = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line: String?
        val sb = StringBuilder()
        while (rd.readLine().also { line = it } != null) {
            sb.append(line)
        }
        rd.close()
        val parser = JSONParser()
        val obj = parser.parse(sb.toString()) as JSONObject
        val data = obj["SeoulRtd.citydata_ppltn"] as JSONArray
        val pplData = data[0] as JSONObject
        val min = pplData["AREA_PPLTN_MIN"] as String
        val max = pplData["AREA_PPLTN_MAX"] as String
        return Pair(min.toInt(), max.toInt())
    }


    private fun testRequest() {
        val url = URL("http://localhost:8000")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Access-Control-Allow-Origin", "*")
        conn.setRequestProperty("Access-Control-Allow-Headers", "*")
        val inputStream = conn.inputStream
        val rd = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line: String?
        val sb = StringBuilder()
        while (rd.readLine().also { line = it } != null) {
            sb.append(line)
        }
        rd.close()
        println(sb.toString())
    }

    private fun getPredictPeopleCount(place: String): List<Int> {
        println("predict")
        // send request with no cors
        val url = URL("http://localhost:8000/predict/${place.replace(" ", "+")}")
        println(url.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Access-Control-Allow-Origin", "*")
        conn.setRequestProperty("Access-Control-Allow-Headers", "*")
        val inputStream = conn.inputStream
        val rd = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var line: String?
        val sb = StringBuilder()
        while (rd.readLine().also { line = it } != null) {
            sb.append(line)
        }
        rd.close()
        val parser = JSONParser()
        val obj = parser.parse(sb.toString()) as JSONObject
        val yhat = obj["yhat"] as JSONObject
        val pplList = yhat.values.toList()
        return pplList.map { it.toString().toDouble().toInt() }
    }
    fun getMaxPeopleCount(place: String): Int {
        val current = getPeopleCount(place)
        val predict = getPredictPeopleCount(place)
        return max(((current.first + current.second) / 2.0).toInt(), predict.max())
    }

    fun getMeanPeopleCount(place: String): Int {
        val current = getPeopleCount(place)
        val predict = getPredictPeopleCount(place)
        var sum = 0
        predict.forEach { sum += it }
        sum += ((current.first + current.second) / 2.0).toInt()
        return sum / (predict.size + 1)
    }

    private fun getPlaceSize(place: String): Int {
        return Place.PLACE_SIZE.list[Place.PLACE_NM.list.indexOf(place.replace("+", " "))].toInt()
    }
}

fun main(args: Array<String>) {
    var httpServerManager: Server? = null

    try {
        // 시작 로그
        println(
            String.format(
                "[%s][HTTP SERVER][START]",
                SimpleDateFormat("yyyy-MM-dd H:mm:ss").format(Date())
            )
        )

        // 서버 생성
        httpServerManager = Server("0.0.0.0", 8080)
        httpServerManager.start()
        // Shutdown Hook
        Runtime.getRuntime().addShutdownHook(Thread { // 종료 로그
            println(
                String.format(
                    "[%s][HTTP SERVER][STOP]",
                    SimpleDateFormat("yyyy-MM-dd H:mm:ss").format(Date())
                )
            )
        })

        // Enter를 입력하면 종료
        print("Please press 'Enter' to stop the server.")
        System.`in`.read()
    } catch (e: IOException) {
        e.printStackTrace()
        throw RuntimeException(e)
    } finally {
        // 종료
        // 0초 대기후  종료
        httpServerManager?.stop(0)
    }
}