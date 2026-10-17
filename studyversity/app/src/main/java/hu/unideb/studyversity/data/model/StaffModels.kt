package hu.unideb.studyversity.data.model

import java.util.Date

/** A student as the instructor console sees (and edits) them. */
data class StaffStudent(
    val id: Long = 0,
    val firstName: String? = null,
    val lastName: String? = null,
    /** The "University ID / code" (Neptun code). */
    val neptunCode: String? = null,
    val email: String? = null,
    val balance: Int? = null,
    val trainingName: String? = null,
    val facultyName: String? = null,
    val trainingStart: Date? = null,
    val trainingPlanEnd: Date? = null
) {
    val fullName get() = listOfNotNull(lastName, firstName).joinToString(" ")
}

/** Fields of a student the instructor may change. */
data class StudentUpdate(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val neptunCode: String,
    val email: String,
    val balance: Int,
    val trainingPlanEnd: Date?
)

/**
 * One exam / grade record. [courseType] is `L` (lecture) or `P` (practice), [examType] is `EXAM`, `MARK` or `FINAL`;
 * they are codes, the app translates them. A record is visible to the student once it has a grade or [opened] is true.
 */
data class StaffExam(
    val id: Long? = null,
    val courseCode: String? = null,
    val subjectName: String? = null,
    val courseType: String? = "L",
    val semester: String? = null,
    val credit: Int? = null,
    val examType: String? = "EXAM",
    val examDate: Date? = null,
    /** 1..5 or null while not graded */
    val grade: Int? = null,
    val recordDate: Date? = null,
    val opened: Boolean = false
) {
    val isPassed get() = grade != null && grade >= 2
}

data class CourseOption(val courseCode: String, val subjectName: String, val semester: String, val credit: Int)

data class MailItem(
    val id: Long? = null,
    val studentId: Long = 0,
    val from: String? = null,
    val to: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val sentAt: Date? = null
)
