package it.bonificamarche.services.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

const val PATTER_DATE = "yyyy-MM-dd"
private val formatter = DateTimeFormatter.ofPattern(PATTER_DATE, Locale.ITALIAN)

private val TAG = "Utilities Date Time"

/**
 * Get current local date.
 */
fun getLocalDate(): LocalDate {
    return LocalDate.now()
}

/**
 * Get current local date time.
 */
fun getLocalDateTime(): LocalDateTime {
    return LocalDateTime.now()
}

/**
 * Add one day to the date.
 * @param date: date to add one day.
 */
fun addOneDay(date: String): String {
    val formatDate = LocalDate.parse(date.subSequence(0, 10), formatter)
    val newDate = formatDate.plusDays(1)
    return getParsedDate(newDate)
}

/**
 * Date parsed according to the specified format.
 */
fun getParsedDate(date: LocalDate): String {
    val dateToBeFormat = date.toString()
    val formattedDate = LocalDate.parse(dateToBeFormat, formatter)

    val time = getLocalDateTime()

    val month = if (formattedDate.monthValue.toString().length == 1) "0${formattedDate.monthValue}"
    else formattedDate.monthValue

    val day = if (formattedDate.dayOfMonth.toString().length == 1) "0${formattedDate.dayOfMonth}"
    else formattedDate.dayOfMonth

    val hour = if (time.hour.toString().length == 1) "0${time.hour}"
    else time.hour

    val minute = if (time.minute.toString().length == 1) "0${time.minute}"
    else time.minute

    return "${formattedDate.year}-${month}-${day} ${hour}:${minute}"
}

/**
 * Compare two dates. Check if they are the same.
 */
fun compareDate(date1: String, date2: String): Boolean {
    val formatDate1 = LocalDate.parse(date1.subSequence(0, 10), formatter)
    val formatDate2 = LocalDate.parse(date2.subSequence(0, 10), formatter)

    return formatDate1.year == formatDate2.year &&
            formatDate1.monthValue == formatDate2.monthValue &&
            formatDate1.dayOfMonth == formatDate2.dayOfMonth
}

fun differenceInMinute(date1 : Date, date2 : Date): Long {
    val diff = date1.time - date2.time
    return  diff / 60
}