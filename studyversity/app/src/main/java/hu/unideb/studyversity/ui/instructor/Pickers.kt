package hu.unideb.studyversity.ui.instructor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Date

/** Small date / time picker helpers for the console forms. */
object Pickers {
    fun date(context: Context, initial: Date?, neutralText: String? = null, onClear: (() -> Unit)? = null, onPick: (year: Int, month: Int, day: Int) -> Unit) {
        val c = Calendar.getInstance().apply { time = initial ?: Date() }
        val dialog = DatePickerDialog(context, { _, y, m, d -> onPick(y, m, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        if (neutralText != null && onClear != null) {
            dialog.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, neutralText) { _, _ -> onClear() }
        }
        dialog.show()
    }

    fun time(context: Context, hour: Int, minute: Int, onPick: (hour: Int, minute: Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPick(h, m) }, hour, minute, true).show()
    }

    fun withDay(base: Date?, y: Int, m: Int, d: Int): Date = Calendar.getInstance().apply {
        time = base ?: Date().also { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }
        set(y, m, d)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    fun withTime(base: Date?, h: Int, min: Int): Date = Calendar.getInstance().apply {
        time = base ?: Date()
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
}
