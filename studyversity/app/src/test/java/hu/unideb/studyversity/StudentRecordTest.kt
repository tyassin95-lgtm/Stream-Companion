package hu.unideb.studyversity

import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.local.Accounts
import hu.unideb.studyversity.data.model.NewsChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date

/** The dentistry student: started in autumn 2020, one course (Internal Medicine) left, graduation expected in 2027. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class)
class StudentRecordTest {
    private val backend get() = ServiceLocator.backend

    @Before fun setUp() = TestSupport.resetApp(loggedIn = true)

    private fun cal(d: Date) = Calendar.getInstance().apply { time = d }

    @Test fun profileAndTraining() = runBlocking {
        val user = backend.currentUser()!!
        assertEquals("Szilágyi Máté", user.name)
        assertEquals("DZ7K4M", user.neptunCode)
        assertEquals("Faculty of Dentistry", user.facultyNameList)
        val t = backend.trainings().single()
        assertEquals("Dentistry", t.moduleName)
        assertEquals(2020, cal(t.trainingStart!!).get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, cal(t.trainingStart!!).get(Calendar.MONTH))
        assertEquals(2027, cal(t.trainingPlanEnd!!).get(Calendar.YEAR))
        assertEquals("Active", t.moduleStatus)
    }

    @Test fun onlyInternalMedicineIsLeft() = runBlocking {
        val courses = backend.courses()
        val semesters = courses.mapNotNull { it.semester }.distinct().sorted()
        assertEquals("2020/21/1", semesters.first())
        assertEquals("2026/27/1", semesters.last())
        assertFalse("autumn 2025 was a passive semester", semesters.contains("2025/26/1"))
        // Internal Medicine was attended in spring 2026 and is repeated now; nothing else is current
        assertEquals(setOf("Internal Medicine"), courses.filter { it.semester == "2026/27/1" }.map { it.subjectName }.toSet())
        assertEquals(setOf("Internal Medicine"), courses.filter { it.semester == "2025/26/2" }.map { it.subjectName }.toSet())

        val graded = backend.exams().filter { it.examValue != null }
        assertTrue(graded.all { it.examValue!!.toInt() in 3..5 })
        val withoutResult = courses.mapNotNull { it.subjectName }.toSet() - graded.mapNotNull { it.subjectName }.toSet()
        assertEquals(setOf("Internal Medicine"), withoutResult)
        // nothing is open yet: no upcoming exam is shown
        assertTrue(backend.exams().none { it.examValue == null })
        assertTrue(graded.size > 80)
    }

    @Test fun timetableFollowsTheUploadedTimetables() = runBlocking {
        val all = backend.timetable()
        assertTrue(all.all { it.subjectName == "Internal Medicine" && it.roomName == "2 MC" })
        val autumn = all.filter { it.semester == "2026/27/1" }
        assertEquals(28, autumn.size)
        val lectures = autumn.filter { it.courseType == "Lecture" }
        val practices = autumn.filter { it.courseType == "Practice" }
        assertEquals(14, lectures.size); assertEquals(14, practices.size)
        // lecture: Wednesday 10-11, practice: Thursday 15-16, weeks 1-14 of the semester (Sep 7 - Dec 11, 2026)
        assertTrue(lectures.all { cal(it.classStart!!).get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY && cal(it.classStart!!).get(Calendar.HOUR_OF_DAY) == 10 && cal(it.classEnd!!).get(Calendar.HOUR_OF_DAY) == 11 })
        assertTrue(practices.all { cal(it.classStart!!).get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY && cal(it.classStart!!).get(Calendar.HOUR_OF_DAY) == 15 && cal(it.classEnd!!).get(Calendar.HOUR_OF_DAY) == 16 })
        val first = cal(autumn.minOf { it.classStart!! }); val last = cal(autumn.maxOf { it.classStart!! })
        assertEquals(listOf(2026, Calendar.SEPTEMBER, 9), listOf(first.get(Calendar.YEAR), first.get(Calendar.MONTH), first.get(Calendar.DAY_OF_MONTH)))
        assertEquals(listOf(2026, Calendar.DECEMBER, 10), listOf(last.get(Calendar.YEAR), last.get(Calendar.MONTH), last.get(Calendar.DAY_OF_MONTH)))

        // spring 2025/26: Monday lecture 11-13 and practice 14-16
        val spring = all.filter { it.semester == "2025/26/2" }
        assertEquals(28, spring.size)
        assertTrue(spring.all { cal(it.classStart!!).get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY })
        assertTrue(spring.filter { it.courseType == "Lecture" }.all { cal(it.classStart!!).get(Calendar.HOUR_OF_DAY) == 11 && cal(it.classEnd!!).get(Calendar.HOUR_OF_DAY) == 13 })
        assertTrue(spring.filter { it.courseType == "Practice" }.all { cal(it.classStart!!).get(Calendar.HOUR_OF_DAY) == 14 && cal(it.classEnd!!).get(Calendar.HOUR_OF_DAY) == 16 })
        assertEquals(56, all.size)
    }

    @Test fun academicCalendarOfTheFacultyIsBundled() = runBlocking {
        val dentistry = backend.academicYears().first { it.name == "Faculty of Dentistry" }
        assertEquals("file:///android_asset/calendar/dentistry_2026_27.html", dentistry.url)
        val html = ApplicationProviderAssets.read("calendar/dentistry_2026_27.html")
        for (text in listOf("September 7, 2026", "December 14, 2026", "January 29, 2027", "June 14, 2027", "April 19", "August 9")) assertTrue(text, html.contains(text))
    }

    @Test fun noDemoWordingAnywhere() = runBlocking {
        val bad = Regex("(?i)\\bdemo|demó|sample|minta|placeholder|test user|dummy")
        val texts = ArrayList<String>()
        backend.currentUser()?.let { texts += listOfNotNull(it.name, it.email) }
        backend.messages().forEach { texts += listOfNotNull(it.messageHeader, it.messageBody) }
        NewsChannel.values().forEach { c -> backend.rss(c).forEach { texts += listOfNotNull(it.title, it.description) } }
        backend.academicYears().forEach { texts += listOfNotNull(it.name) }
        backend.scholarships().forEach { texts += listOfNotNull(it.name) }
        val hits = texts.filter { bad.containsMatchIn(it) }
        assertTrue("demo wording in data: $hits", hits.isEmpty())
        assertTrue("demo wording in strings: ${StringScan.hits(bad)}", StringScan.hits(bad).isEmpty())
    }
}
