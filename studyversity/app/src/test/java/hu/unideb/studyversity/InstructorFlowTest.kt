package hu.unideb.studyversity

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.local.Accounts
import hu.unideb.studyversity.databinding.*
import hu.unideb.studyversity.ui.instructor.*
import hu.unideb.studyversity.ui.main.MainActivity
import hu.unideb.studyversity.ui.user.login.LoginActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The instructor signs in, edits the student, opens exams, enters grades, changes the timetable and sends e-mail. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstructorFlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val backend get() = ServiceLocator.backend

    private fun <T : Activity> start(cls: Class<T>): ActivityController<T> {
        val c = Robolectric.buildActivity(cls, Intent(app, cls)).setup()
        TestSupport.settle()
        return c
    }

    private fun views(root: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) views(root.getChildAt(i), out)
        return out
    }

    private fun snackbar(a: Activity) = views(a.window.decorView)
        .firstOrNull { it.id == com.google.android.material.R.id.snackbar_text && it.isShown }?.let { (it as TextView).text.toString() }

    private fun console(): ActivityController<InstructorActivity> {
        TestSupport.resetApp(loggedIn = true, instructor = true)
        return start(InstructorActivity::class.java)
    }

    private inline fun <reified F : androidx.fragment.app.Fragment> InstructorActivity.tab(): F =
        supportFragmentManager.fragments.filterIsInstance<F>().single()

    private fun dialog(): AlertDialog = org.robolectric.shadows.ShadowDialog.getLatestDialog() as AlertDialog

    /** The list row whose title view shows [text]. */
    private fun row(list: RecyclerView, id: Int, text: String): View =
        (0 until list.childCount).map { list.getChildAt(it) }.first { v -> v.findViewById<TextView>(id)?.text?.toString() == text }

    // ---- sign in ---------------------------------------------------------------------------------------------------
    @Test fun instructorSignInOpensTheConsole() {
        TestSupport.resetApp(false)
        val c = start(LoginActivity::class.java)
        val b = ActivityLocalLoginBinding.bind((c.get().findViewById<ViewGroup>(android.R.id.content)).getChildAt(0))
        b.neptunCodeInput.setText(Accounts.instructor.username); b.passwordInput.setText(Accounts.instructor.password)
        b.loginButton.performClick(); TestSupport.settle()
        assertTrue(c.get().isFinishing)
        assertEquals(LoginActivity.RESULT_INSTRUCTOR, shadowOf(c.get()).resultCode)
        assertEquals(InstructorActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
        assertTrue(ServiceLocator.session.currentUser!!.isInstructor)
    }

    @Test fun consoleIsClosedForStudentsAndSignedOutUsers() {
        TestSupport.resetApp(false)
        assertTrue(start(InstructorActivity::class.java).get().isFinishing)
        TestSupport.resetApp(true)
        assertTrue(start(InstructorActivity::class.java).get().isFinishing)
    }

    @Test fun homeMenuOfAnInstructor() {
        TestSupport.resetApp(true, instructor = true)
        val c = start(MainActivity::class.java)
        val home = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).childFragmentManager.primaryNavigationFragment!!
        val shown = views(home.requireView()).filter { it.id == R.id.menu_item_title && (it.parent.parent as View).visibility == View.VISIBLE }.map { (it as TextView).text.toString() }
        assertTrue(shown.toString(), shown.contains(app.getString(R.string.menu_instructor_console)))
        assertTrue(shown.contains(app.getString(R.string.menu_logout)))
        for (hidden in listOf(R.string.menu_profile, R.string.menu_time_table, R.string.menu_taken_exams, R.string.menu_my_messages)) {
            assertFalse(shown.contains(app.getString(hidden)))
        }
        // the entry opens the console
        views(home.requireView()).first { it.id == R.id.sub_menu_parent && views(it).any { t -> t is TextView && t.text == app.getString(R.string.menu_instructor_console) } }.performClick()
        TestSupport.settle()
        assertEquals(InstructorActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
    }

    // ---- student record --------------------------------------------------------------------------------------------
    @Test fun editingTheStudent() {
        val activity = console().get()
        val tab = activity.tab<InstructorStudentFragment>()
        val b = FragmentInstructorStudentBinding.bind(tab.requireView())
        assertEquals("Szilágyi Máté", b.studentHeader.text.toString())
        assertEquals("DZ7K4M", b.universityIdInput.text.toString())
        assertEquals("Szilágyi", b.lastNameInput.text.toString())

        b.lastNameInput.setText("Varga"); b.firstNameInput.setText("Bence"); b.universityIdInput.setText("zz99xx")
        b.emailInput.setText("bence.varga@students.example.org"); b.balanceInput.setText("-1200")
        b.saveStudentButton.performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.student_saved), snackbar(activity))
        assertEquals("Varga Bence", b.studentHeader.text.toString())

        runBlocking { backend.logout(); backend.login(Accounts.student.username, Accounts.student.password) }
        val me = runBlocking { backend.currentUser()!! }
        assertEquals("Varga Bence", me.name); assertEquals("ZZ99XX", me.neptunCode); assertEquals(-1200, me.balance)
    }

    @Test fun studentFormValidates() {
        val activity = console().get()
        val b = FragmentInstructorStudentBinding.bind(activity.tab<InstructorStudentFragment>().requireView())
        b.universityIdInput.setText("")
        b.saveStudentButton.performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.student_required), snackbar(activity))
        b.universityIdInput.setText("AB1"); b.balanceInput.setText("12-3")
        b.saveStudentButton.performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.student_balance_invalid), snackbar(activity))
        assertEquals("DZ7K4M", runBlocking { backend.managedStudents().single().neptunCode })
    }

    // ---- exams and grades ----------------------------------------------------------------------------------------------
    @Test fun openingAndGradingInternalMedicine() {
        val activity = console().get()
        val b = FragmentInstructorListBinding.bind(activity.tab<InstructorExamsFragment>().requireView())
        assertTrue(b.instructorList.adapter!!.itemCount > 90)
        row(b.instructorList, R.id.examSubject, "Internal Medicine").performClick(); TestSupport.settle()

        val d = dialog()
        val grade = d.findViewById<AutoCompleteTextView>(R.id.examGradeInput)!!
        assertEquals(app.getString(R.string.exam_no_grade), grade.text.toString())
        grade.setText("4", false)
        d.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.examOpenSwitch)!!.isChecked = true
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); TestSupport.settle()
        assertFalse(d.isShowing)
        assertEquals(app.getString(R.string.exam_saved), snackbar(activity))

        runBlocking { backend.logout(); backend.login(Accounts.student.username, Accounts.student.password) }
        val exam = runBlocking { backend.exams() }.single { it.subjectName == "Internal Medicine" }
        assertEquals("4", exam.examValue)
    }

    @Test fun openingANewExamNeedsACourse() {
        val activity = console().get()
        val b = FragmentInstructorListBinding.bind(activity.tab<InstructorExamsFragment>().requireView())
        val before = b.instructorList.adapter!!.itemCount
        b.addButton.performClick(); TestSupport.settle()
        val d = dialog()
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); TestSupport.settle()
        assertTrue("dialog stays open", d.isShowing)
        assertEquals(app.getString(R.string.exam_choose_course), snackbar(activity))

        val course = d.findViewById<AutoCompleteTextView>(R.id.examCourseInput)!!
        course.onItemClickListener.onItemClick(null, null, 0, 0)
        course.setText(course.adapter.getItem(0).toString(), false)
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); TestSupport.settle()
        assertFalse(d.isShowing)
        assertEquals(before + 1, b.instructorList.adapter!!.itemCount)
    }

    @Test fun deletingAnExamRecord() {
        val activity = console().get()
        val b = FragmentInstructorListBinding.bind(activity.tab<InstructorExamsFragment>().requireView())
        val before = b.instructorList.adapter!!.itemCount
        row(b.instructorList, R.id.examSubject, "Final Examination (written)").performClick(); TestSupport.settle()
        dialog().getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick(); TestSupport.settle()
        assertEquals(before - 1, b.instructorList.adapter!!.itemCount)
    }

    // ---- timetable -------------------------------------------------------------------------------------------------
    @Test fun cancellingAClass() {
        val activity = console().get()
        val b = FragmentInstructorListBinding.bind(activity.tab<InstructorTimetableFragment>().requireView())
        assertEquals(56, b.instructorList.adapter!!.itemCount)
        views(b.instructorList).first { it.id == R.id.classRemove }.performClick(); TestSupport.settle()
        dialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); TestSupport.settle()
        assertEquals(55, b.instructorList.adapter!!.itemCount)
        assertEquals(55, runBlocking { backend.studentClasses(1) }.size)
        runBlocking { backend.logout(); backend.login(Accounts.student.username, Accounts.student.password) }
        assertEquals(55, runBlocking { backend.timetable() }.size)
    }

    @Test fun addingAClassNeedsSubjectAndTimes() {
        val activity = console().get()
        val b = FragmentInstructorListBinding.bind(activity.tab<InstructorTimetableFragment>().requireView())
        b.addButton.performClick(); TestSupport.settle()
        val d = dialog()
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.class_subject_required), snackbar(activity))
        d.findViewById<TextView>(R.id.classSubjectInput)!!.text = "Consultation"
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.class_date_required), snackbar(activity))
        assertEquals(56, runBlocking { backend.studentClasses(1) }.size)
    }

    // ---- e-mail ----------------------------------------------------------------------------------------------------
    @Test fun sendingAnEmailToTheStudent() {
        val activity = console().get()
        val b = FragmentInstructorMailBinding.bind(activity.tab<InstructorMailFragment>().requireView())
        assertTrue(b.mailToInput.text.toString().contains("Szilágyi Máté"))
        b.sendMailButton.performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.email_fields_required), snackbar(activity))

        b.mailSubjectInput.setText("Internal Medicine exam")
        b.mailBodyInput.setText("Your exam is on January 12 at 9:00.")
        b.sendMailButton.performClick(); TestSupport.settle()
        assertEquals(app.getString(R.string.email_sent, "Szilágyi Máté"), snackbar(activity))
        assertEquals("", b.mailSubjectInput.text.toString())
        assertEquals(1, b.mailHistory.childCount)
        assertEquals(View.GONE, b.mailEmptyText.visibility)

        runBlocking { backend.logout(); backend.login(Accounts.student.username, Accounts.student.password) }
        val msg = runBlocking { backend.messages() }.first()
        assertEquals("Internal Medicine exam", msg.messageHeader)
        assertFalse(msg.isRead)
    }

    @Test fun signingOutFromTheConsole() {
        val activity = console().get()
        shadowOf(activity).clickMenuItem(R.id.instructor_logout); TestSupport.settle()
        assertTrue(activity.isFinishing)
        assertFalse(ServiceLocator.session.isLoggedIn)
    }
}
