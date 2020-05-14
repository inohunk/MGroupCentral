package utils

import java.text.SimpleDateFormat
import java.util.*

//Date patterns
const val PATTERN_FULL_DATE = "yyyy.MM.dd HH:mm:ss"
const val PATTERN_YEAR_MONTH_DAY = "yyyy-MM-dd"
const val PATTERN_DAY_MONTH_YEAR = "dd.MM.yyyy"
const val PATTERN_HOUR_MINUTE_SECOND = "HH:mm:ss"
const val PATTERN_HOUR_MINUTE_SECOND_MILLISECOND = "HH:mm:ss.SSS"
const val PATTERN_FULL_DATE_INVERSE = "$PATTERN_HOUR_MINUTE_SECOND dd.MM.yyyy"
const val PATTERN_FULL_DATE_WITH_MILLISECONDS = "yyyy.MM.dd HH:mm:ss.SSSS"

//Convert functions (from milliseconds)
fun convertMillisToTime(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
    format.timeZone = TimeZone.getTimeZone("Europe/Moscow")
    return format.format(date)
}

fun convertMillisToTime(time: Long, pattern: String): String {
    val date = Date(time)
    val format = SimpleDateFormat(pattern)
    format.timeZone = TimeZone.getTimeZone("Europe/Moscow")
    return format.format(date)
}

fun convertMillisToTime(time: Long, pattern: String, timeZone: TimeZone): String {
    val data = Date(time)
    val format = SimpleDateFormat(pattern)
    format.timeZone = timeZone
    return format.format(data)
}

fun convertTimeToMillis(date: String): Long {
    val format = SimpleDateFormat(PATTERN_FULL_DATE_WITH_MILLISECONDS)
    val convertedDate = format.parse(date)
    return convertedDate.time
}

fun convertTimeToMillis(date: String, pattern: String): Long {
    val format = SimpleDateFormat(pattern)
    val convertedDate = format.parse(date)
    return convertedDate.time
}

/**
 * Function rounding milliseconds
 * @param ms Milliseconds for rounding
 */
fun roundMilliseconds(ms: Long): Long {
    return (1000 * (ms / 1000))
}