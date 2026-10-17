package hu.unideb.studyversity.data.local

import hu.unideb.studyversity.data.model.MailItem
import hu.unideb.studyversity.data.model.StaffExam
import hu.unideb.studyversity.data.model.TimeTableItem
import java.util.Date

/** What the university knows about the student; the instructor console edits this record and the student screens read it. */
class StudentRecord(
    var id: Long = 1,
    var firstName: String = "",
    var lastName: String = "",
    /** University ID / code (Neptun code) */
    var neptun: String = "",
    var email: String = "",
    var balance: Int = 0,
    var trainingPlanEnd: Date? = null,
    var exams: MutableList<StaffExam> = mutableListOf(),
    var classes: MutableList<TimeTableItem> = mutableListOf(),
    var mails: MutableList<MailItem> = mutableListOf()
) {
    val fullName get() = listOf(lastName, firstName).filter { it.isNotBlank() }.joinToString(" ")
}
