package data

import event.PopulationChangeListener
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import kotlin.math.abs

class CurrentDataListener {

    companion object {
        private val instance = CurrentDataListener()
        fun getInstance(): CurrentDataListener {
            return instance
        }
        fun getPeopleCount(): HashMap<String, Int> {
            return instance.getPeopleCount()
        }

        fun getPopulationChange(): HashMap<String, Double> {
            return instance.getPopulationChange()
        }

        fun update(listener: PopulationChangeListener) {
            instance.update(listener)
        }
    }

    

    private fun getPeopleCount(): HashMap<String, Int> {
        val url = URL("http://jrh-ishs.kro.kr:5000/people")
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
        val result = HashMap<String, Int>()
        obj.keys.forEach { key ->
            run {
                val value = obj[key] as JSONObject
                val minValue = value["ppl_min"] as Long
                val maxValue = value["ppl_max"] as Long
                result[key.toString()] = (minValue + maxValue).toInt() / 2
            }
        }
        return result
    }

    private fun getPopulationChange(): HashMap<String, Double> {
        val url = URL("http://jrh-ishs.kro.kr:5000/peopleChangeRate")
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
        val result = HashMap<String, Double>()
        obj.keys.forEach { key ->
            run {
                val value = obj[key] as JSONObject
                val minRate = value["ppl_min_rate"] as Double
                val maxRate = value["ppl_max_rate"] as Double
                result[key.toString()] = (minRate + maxRate) / 2
            }
        }
        return result
    }

    private fun update(listener: PopulationChangeListener) {
        getPopulationChange().forEach { (place, rate) ->
            run {
                if (abs(rate) > 0.1) {
                    listener.populationChange(place)
                }
            }
        }
    }

}