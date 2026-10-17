package hu.unideb.studyversity.core

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Date formatting identical to the original DateBindingHandler (short, locale-specific styles). */
object Formats {
    fun date(d: Date?): String = d?.let { DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(it) } ?: ""
    fun dateAndTime(d: Date?): String = d?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(it) } ?: ""
    fun hourMinute(d: Date?): String = d?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: ""
}
