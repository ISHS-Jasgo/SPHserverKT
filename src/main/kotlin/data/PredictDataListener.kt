package data

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

class PredictDataListener {
    companion object {
        private val instance = PredictDataListener()
        fun getInstance(): PredictDataListener {
            return instance
        }
        fun getPredictPeopleCount(): HashMap<String, List<Int>> {
            return instance.getPredictPeopleCountWithJSON(instance.getPredictData())
        }
    }
    private fun getPredictData(): JSONObject {
        val url = URL("http://jrh-ishs.kro.kr:8000/predict/all")
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
        return parser.parse(sb.toString()) as JSONObject
    }

    private fun getPredictPeopleCountWithJSON(data: JSONObject): HashMap<String, List<Int>> {
        val result = HashMap<String, List<Int>>()
        data.keys.forEach { key ->
            run {
                val value = data[key] as JSONObject
                val yhat = value["yhat"] as JSONObject
                val pplList = yhat.values.toList()
                result[key.toString()] = pplList.map { it.toString().toDouble().toInt() }
            }
        }
        return result
    }


}