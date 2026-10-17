package hu.unideb.studyversity.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import hu.unideb.studyversity.R
import java.util.Date

data class CalendarDetail(val id: Long, val displayName: String)
data class CalendarEvent(val startTime: Date, val endTime: Date, val title: String, val description: String?, val location: String?)

/** Writes the timetable into a local, on-device calendar (the "private" LOCAL account of the original app). */
class CalendarService(private val context: Context) {
    private val accountName = "private"
    private val calendarsUri: Uri = CalendarContract.Calendars.CONTENT_URI

    private fun defaultCalendarValues(calendarName: String) = ContentValues().apply {
        put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        put(CalendarContract.Calendars.NAME, "private_$calendarName")
        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarName)
        put(CalendarContract.Calendars.CALENDAR_COLOR, ContextCompat.getColor(context, R.color.colorPrimary))
        put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
        put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
        put(CalendarContract.Calendars.VISIBLE, 1)
        put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        put(CalendarContract.Calendars._ID, STUDYVERSITY_CALENDAR_ID)
    }

    private fun defaultCalendarUri(): Uri = calendarsUri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    fun addCalendar(name: String): Uri? = runCatching { context.contentResolver.insert(defaultCalendarUri(), defaultCalendarValues(name)) }.getOrNull()

    fun deleteCalendar(calendarId: Long): Int? =
        runCatching { context.contentResolver.delete(ContentUris.withAppendedId(calendarsUri, calendarId), null, null) }.getOrNull()

    fun addEvent(event: CalendarEvent) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, event.startTime.time)
            put(CalendarContract.Events.DTEND, event.endTime.time)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.CALENDAR_ID, STUDYVERSITY_CALENDAR_ID)
            put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Budapest")
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
        }
        runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }
    }

    /** Calendars the app may write to (access level OWNER). */
    fun getCalendars(): List<CalendarDetail> {
        val out = mutableListOf<CalendarDetail>()
        runCatching {
            context.contentResolver.query(
                calendarsUri,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_TIME_ZONE,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(3) == CalendarContract.Calendars.CAL_ACCESS_OWNER) out.add(CalendarDetail(c.getLong(0), c.getString(2)))
                }
            }
        }
        return out
    }

    companion object { const val STUDYVERSITY_CALENDAR_ID = 123 }
}
