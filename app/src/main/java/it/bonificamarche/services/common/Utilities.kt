package it.bonificamarche.services.common

import android.annotation.SuppressLint
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

const val PATH = "/storage/emulated/0/Android/data/it.bonificamarche"
private val packages: List<String> = listOf(".colture/")
const val PATTER_DATE = "yyyyMMdd"
const val PATTER_HOUR = "HHmmss"

/**
 * Used for the logging.
 */
val show = { TAG: String, log: Any ->
    Log.e(TAG, "[$TAG] $log")
}

/**
 * Get current date.
 * @return current date of integer type.
 */
fun currentDate(): Int {
    val current = LocalDateTime.now()
    val formattedDate = DateTimeFormatter.ofPattern(PATTER_DATE)
    return current.format(formattedDate).toInt()

}

/**
 * Get current hour.
 * @return current hour of string type.
 */
fun currentHour(): String {
    val current = LocalDateTime.now()
    val formattedHour = DateTimeFormatter.ofPattern(PATTER_HOUR)
    return current.format(formattedHour)
}

/**
 * Add one day to the date.
 * @param date: date to add one day.
 */
@SuppressLint("SimpleDateFormat")
fun addOneDay(date: Int): Int {

    val formattedDate = SimpleDateFormat(PATTER_DATE)
    val cal: Calendar = Calendar.getInstance()
    cal.time = formattedDate.parse(date.toString())
    cal.add(Calendar.DATE, 1)
    val dt = formattedDate.format(cal.time)

    return dt.toInt()
}


/**
 * Find photos to send to the server.
 * @return number of photos to send.
 */
fun findPhotoToSend(appName: String): Int {

    val imagesDir = File("/storage/emulated/0/Pictures/${appName}/")
    var counterImg = 0

    if (imagesDir.exists() && imagesDir.isDirectory) {
        show("Fun find photo", "Search photos...")
        imagesDir.listFiles()?.forEach { _ ->
            counterImg += 1
        }
    }
    return counterImg
}