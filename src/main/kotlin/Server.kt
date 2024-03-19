import data.CurrentDataListener
import data.PredictDataListener
import event.FirstLoadEndEvent
import event.FirstLoadEndListener
import event.PopulationChangeEvent
import event.PopulationChangeListener
import handler.HttpServer
import simulation.calculateSPH
import util.Place
import java.io.IOException
import java.nio.channels.CompletionHandler
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.sqrt

var resultList = HashMap<String, List<Double>>()

fun main(args: Array<String>) {
    val timer = Timer()
    var count = 0
    val firstLoadEndListener = FirstLoadEndListener()
    val executeServiceWithCached = Executors.newFixedThreadPool(40) {
        val thread = Thread(it)
        thread.isDaemon = true
        thread
    }
    val completionHandler: CompletionHandler<Double, Any?> = object : CompletionHandler<Double, Any?> {
        override fun completed(result: Double, attachment: Any?) {
            println("${++count} completed ${resultList.size} / thread count: ${Thread.activeCount()}")
            if (count == 100) {
                firstLoadEndListener.firstLoadEnd()
                count = 0
            }
        }

        override fun failed(exc: Throwable, attachment: Any?) {
            println("failed")
        }
    }
    val peopleCountData = CurrentDataListener.getPeopleCount()
    val predictPeopleData = PredictDataListener.getPredictPeopleCount()
    var placeList = Place.PLACE_NM.list
    placeList = placeList.filter { place -> getPlaceSize(place) != 0 }
    // group 2 place in one thread
    val groupSize = 2
    for (i in placeList.indices step groupSize) {
        val task = Runnable {
            for (j in i until i + groupSize) {
                if (j < placeList.size) {
                    simulate(placeList[j], predictPeopleData[placeList[j]]!!, peopleCountData[placeList[j]]!!, completionHandler)
                }
            }
        }
        executeServiceWithCached.submit(task)
    }
    val listener = PopulationChangeListener()
    firstLoadEndListener.setOnFirstLoadEnd(object : FirstLoadEndEvent {
        override fun onFirstLoadEnd() {
            timer.schedule(object : TimerTask() {
                override fun run() {
                    CurrentDataListener.update(listener)
                }
            }, 0, 1000 * 60 * 10)
        }
    })
    listener.setOnPopulationChange(object : PopulationChangeEvent {
        override fun onPopulationChange(place: String) {
            if (peopleCountData.containsKey(place)) {
                println("onPopulationChange: $place")
                val task = Runnable {
                    val newPeopleCountData = CurrentDataListener.getPeopleCount()
                    val newPredictPeopleData = PredictDataListener.getPredictPeopleCount()
                    simulate(place, newPredictPeopleData[place]!!, newPeopleCountData[place]!!, completionHandler)
                }
                executeServiceWithCached.submit(task)
            }
        }
    })
    var httpServerManager: HttpServer? = null

    try {
        // 시작 로그
        println(
            String.format(
                "[%s][HTTP SERVER][START]",
                SimpleDateFormat("yyyy-MM-dd H:mm:ss").format(Date())
            )
        )

        // 서버 생성
        httpServerManager = HttpServer("0.0.0.0", 80)
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

fun simulate(place: String, predictData: List<Int>, peopleData: Int, completionHandler: CompletionHandler<Double, Any?>) {
    if (place == "") return
    val size = getPlaceSize(place)
    if (size == 0) return
    val width = sqrt(size.toDouble()).toInt()
    val height = sqrt(size.toDouble()).toInt()
    val result = calculateSPH(width, height, peopleData, predictData)
    resultList[place] = result
    println("$place: $result")
    completionHandler.completed(0.0, null)
}

fun getPlaceSize(place: String): Int {
    return Place.PLACE_SIZE.list[Place.PLACE_NM.list.indexOf(place.replace("+", " "))].toInt()
}