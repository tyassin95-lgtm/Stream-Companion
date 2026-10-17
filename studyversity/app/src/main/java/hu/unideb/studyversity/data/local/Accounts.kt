package hu.unideb.studyversity.data.local

import hu.unideb.studyversity.data.model.ROLE_INSTRUCTOR
import hu.unideb.studyversity.data.model.ROLE_STUDENT

/**
 * The user table of the on-device backend. A real server replaces this with its own identity provider
 * (passwords must never be kept in the app there).
 */
object Accounts {
    class Account(val username: String, val password: String, val role: String)

    val student = Account("student", "Student#2026", ROLE_STUDENT)
    val instructor = Account("instructor", "Instructor#2026", ROLE_INSTRUCTOR)

    fun find(username: String, password: String): Account? =
        listOf(student, instructor).firstOrNull { it.username.equals(username.trim(), ignoreCase = true) && it.password == password }
}
