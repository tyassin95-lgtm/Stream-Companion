package hu.unideb.studyversity

import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.AccessDeniedException
import hu.unideb.studyversity.data.local.Accounts
import hu.unideb.studyversity.data.model.StaffExam
import hu.unideb.studyversity.data.model.StudentUpdate
import hu.unideb.studyversity.data.model.TimeTableItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date

/** What the instructor does is what the student sees afterwards. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class)
class InstructorBackendTest {
    private val backend get() = ServiceLocator.backend

    @Before fun setUp() = TestSupport.resetApp(loggedIn = false)

    private suspend fun asInstructor() { backend.logout(); backend.login(Accounts.instructor.username, Accounts.instructor.password) }
    private suspend fun asStudent() { backend.logout(); backend.login(Accounts.student.username, Accounts.student.password) }
    private suspend fun studentId() = backend.managedStudents().single().id

    @Test fun renamingTheStudentAndChangingTheUniversityCode() = runBlocking {
        asInstructor()
        val s = backend.managedStudents().single()
        assertEquals("DZ7K4M", s.neptunCode)
        val grad = Calendar.getInstance().apply { clear(); set(2028, 0, 31) }.time
        val saved = backend.updateStudent(StudentUpdate(s.id, "Anna", "Kovács", " ab12cd ", "anna.kovacs@students.example.org", -45000, grad))
        assertEquals("AB12CD", saved.neptunCode)
        assertEquals("Kovács Anna", saved.fullName)
        asStudent()
        val me = backend.currentUser()!!
        assertEquals("Kovács Anna", me.name)
        assertEquals("AB12CD", me.neptunCode)
        assertEquals(-45000, me.balance)
        assertEquals("anna.kovacs@students.example.org", me.email)
        assertEquals(2028, Calendar.getInstance().apply { time = backend.trainings().single().trainingPlanEnd!! }.get(Calendar.YEAR))
        // the sign-in name does not change with the code
        asInstructor(); assertEquals(1, backend.managedStudents().size)
    }

    @Test fun invalidStudentChangesAreRefused() = runBlocking {
        asInstructor()
        val s = backend.managedStudents().single()
        for (bad in listOf(StudentUpdate(s.id, "A", "B", "  ", "", 0, null), StudentUpdate(s.id, "", "", "X1", "", 0, null))) {
            try { backend.updateStudent(bad); fail() } catch (e: IllegalArgumentException) { }
        }
        assertEquals("DZ7K4M", backend.managedStudents().single().neptunCode)
    }

    @Test fun openingTheInternalMedicineExamAndGradingIt() = runBlocking {
        asInstructor()
        val id = studentId()
        val exam = backend.studentExams(id).first { it.subjectName == "Internal Medicine" }
        assertFalse(exam.opened); assertNull(exam.grade)

        val date = Calendar.getInstance().apply { clear(); set(2027, 0, 12, 9, 0) }.time
        backend.saveStudentExam(id, exam.copy(examDate = date, opened = true))
        asStudent()
        val upcoming = backend.exams().single { it.subjectName == "Internal Medicine" }
        assertNull(upcoming.examValue)
        assertEquals(date, upcoming.examDate)
        assertEquals("2026/27/1", upcoming.semester)

        asInstructor()
        backend.saveStudentExam(id, exam.copy(examDate = date, opened = true, grade = 4))
        asStudent()
        val graded = backend.exams().single { it.subjectName == "Internal Medicine" }
        assertEquals("4", graded.examValue)
        assertTrue(graded.examSuccess)
        assertNotNull(graded.examRecordDate)
    }

    @Test fun gradeOutsideOneToFiveIsDropped() = runBlocking {
        asInstructor()
        val id = studentId()
        val e = backend.studentExams(id).first { it.subjectName == "Internal Medicine" }
        assertNull(backend.saveStudentExam(id, e.copy(opened = true, grade = 7)).grade)
        assertEquals(1, backend.saveStudentExam(id, e.copy(opened = true, grade = 1)).grade)
        asStudent()
        assertFalse(backend.exams().single { it.subjectName == "Internal Medicine" }.examSuccess)
    }

    @Test fun existingGradesCanBeChangedAndRecordsOpenedForRetakes() = runBlocking {
        asInstructor()
        val id = studentId()
        val anatomy = backend.studentExams(id).first { it.subjectName == "Anatomy I" }
        assertTrue(anatomy.grade in 3..5)
        backend.saveStudentExam(id, anatomy.copy(grade = 5))
        val option = backend.studentCourseOptions(id).first { it.subjectName == "Biochemistry I" }
        val retake = backend.saveStudentExam(id, StaffExam(courseCode = option.courseCode, subjectName = option.subjectName, semester = "2026/27/1", credit = option.credit, opened = true, examDate = Date(System.currentTimeMillis() + 86_400_000L * 30)))
        assertNotNull(retake.id)
        asStudent()
        val exams = backend.exams()
        assertEquals("5", exams.first { it.subjectName == "Anatomy I" }.examValue)
        assertEquals(2, exams.count { it.subjectName == "Biochemistry I" })
        asInstructor()
        backend.deleteStudentExam(id, retake.id!!)
        asStudent()
        assertEquals(1, backend.exams().count { it.subjectName == "Biochemistry I" })
    }

    @Test fun timetableCanBeExtendedAndTrimmed() = runBlocking {
        asInstructor()
        val id = studentId()
        val start = Calendar.getInstance().apply { clear(); set(2026, 9, 28, 14, 0) }.time
        val end = Calendar.getInstance().apply { clear(); set(2026, 9, 28, 15, 30) }.time
        val added = backend.addStudentClass(id, TimeTableItem(subjectName = "Internal Medicine consultation", courseType = "P", roomName = "2 MC", classStart = start, classEnd = end))
        assertNotNull(added.id)
        asStudent()
        val classes = backend.timetable()
        assertEquals(57, classes.size)
        assertEquals("Practice", classes.first { it.subjectName == "Internal Medicine consultation" }.courseType)
        asInstructor()
        backend.deleteStudentClass(id, added.id!!)
        assertEquals(56, backend.studentClasses(id).size)
        try { backend.addStudentClass(id, TimeTableItem(subjectName = "X", classStart = end, classEnd = start)); fail() } catch (e: IllegalArgumentException) { }
        try { backend.addStudentClass(id, TimeTableItem(subjectName = " ", classStart = start, classEnd = end)); fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun emailsArriveInTheStudentsMessages() = runBlocking {
        asInstructor()
        val id = studentId()
        val mail = backend.sendEmail(id, "Exam date", "Hello,\nthe exam is on <b>January 12</b>.")
        assertEquals("Dr. Kelemen Zoltán", mail.from)
        assertEquals(listOf("Exam date"), backend.sentEmails(id).map { it.subject })
        try { backend.sendEmail(id, " ", "text"); fail() } catch (e: IllegalArgumentException) { }
        try { backend.sendEmail(id, "s", " "); fail() } catch (e: IllegalArgumentException) { }

        asStudent()
        val inbox = backend.messages()
        val received = inbox.first { it.messageHeader == "Exam date" }
        assertFalse(received.isRead)
        assertEquals(received.id, inbox.first().id) // newest first
        val detail = backend.message(received.id!!)!!
        assertTrue(detail.messageBody!!.contains("Dr. Kelemen Zoltán"))
        assertTrue("html escaped", detail.messageBody!!.contains("&lt;b&gt;January 12&lt;/b&gt;"))
        assertTrue(detail.messageBody!!.contains("Hello,<br/>the exam"))
        assertTrue(backend.messages().first { it.id == received.id }.isRead)
    }

    @Test fun studentsCannotUseTheConsole() = runBlocking {
        asStudent()
        for (call in listOf<suspend () -> Any?>({ backend.studentExams(1) }, { backend.sendEmail(1, "a", "b") }, { backend.studentClasses(1) }, { backend.studentCourseOptions(1) })) {
            try { call(); fail() } catch (e: AccessDeniedException) { }
        }
    }
}
