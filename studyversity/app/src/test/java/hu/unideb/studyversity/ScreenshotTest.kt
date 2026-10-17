package hu.unideb.studyversity

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ApplicationProvider
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.databinding.*
import hu.unideb.studyversity.ui.community.CommunityActivity
import hu.unideb.studyversity.ui.community.member_detail.MemberDetailActivity
import hu.unideb.studyversity.ui.community.selfview.SelfProfileDisplayAndEditActivity
import hu.unideb.studyversity.ui.community.wizard.WizardContainerActivity
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity
import hu.unideb.studyversity.ui.main.MainActivity
import hu.unideb.studyversity.ui.mood.ActivityMoodHolder
import hu.unideb.studyversity.ui.mood.form.ActivityMoodForm
import hu.unideb.studyversity.ui.mood.gameEnded.ActivityMoodGameEnded
import hu.unideb.studyversity.ui.mood.levelUpgrade.ActivityMoodLevelUpgrade
import hu.unideb.studyversity.ui.university_matters.university_locale.InstitutionListActivity
import hu.unideb.studyversity.ui.user.login.LoginActivity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens of the demo data to PNG files (build/screenshots) so the layouts can be reviewed without a
 * device. Robolectric's native graphics draws real pixels, but fonts and some effects differ from a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val out = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }

    private fun <T : Activity> start(cls: Class<T>, intent: Intent = Intent(app, cls)): ActivityController<T> {
        val c = Robolectric.buildActivity(cls, intent).setup()
        TestSupport.settle()
        return c
    }

    private fun shot(name: String, activity: Activity) {
        TestSupport.settle()
        val v = activity.window.decorView
        val w = activity.resources.displayMetrics.widthPixels
        val h = activity.resources.displayMetrics.heightPixels
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun nav(a: AppCompatActivity) = (a.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    private fun register() = runBlocking {
        val cats = ServiceLocator.backend.categories(emptyList())
        ServiceLocator.backend.registerCommunityMember(CommunityMember(
            photoUrl = "res://avatar_2", description = "I like long walks, board games and coffee between lectures.",
            interests = cats.filter { it.parentCategoryCode == "interests" }.take(4), faculties = cats.filter { it.parentCategoryCode == "faculty_names" }.take(1),
            preferredLanguages = cats.filter { it.parentCategoryCode == "languages" }.take(2),
            facebookLink = "https://www.facebook.com/x", instagramLink = "https://www.instagram.com/x"
        ))
    }

    @Test fun mainScreens() {
        TestSupport.resetApp(true)
        val c = start(MainActivity::class.java)
        val a = c.get()
        shot("home", a)
        for ((name, id) in listOf(
            "profile" to R.id.profileFragment, "timetable" to R.id.timeTableFragment, "exams" to R.id.examsInformationFragment,
            "courses" to R.id.coursesInformationFragment, "phonebook" to R.id.phonebookFragment, "library" to R.id.libraryFragment,
            "librarysearch" to R.id.librarySearchFragment, "hak" to R.id.hakFragment, "scholarship" to R.id.studentScholarShipFragment,
            "academicyear" to R.id.academicYearFragment, "overeducation" to R.id.overEducationFragment, "sportsub" to R.id.sportSubMenuFragment,
            "messages" to R.id.messagesFragment, "settings" to R.id.settingsFragment, "contact" to R.id.contactFragment
        )) {
            nav(a).navigate(id); TestSupport.settle()
            shot(name, a)
            nav(a).popBackStack(R.id.homeFragment, false); TestSupport.settle()
        }
        nav(a).navigate(R.id.rssListFragment, android.os.Bundle().apply {
            putInt("list_item_layout", R.layout.news_rss_list_item); putString("fragmentTitle", "News"); putIntArray("channelID", intArrayOf(119))
        }); TestSupport.settle(); shot("news", a)
    }

    @Test fun placesAndPhonebook() {
        TestSupport.resetApp(true)
        val c = start(MainActivity::class.java)
        nav(c.get()).navigate(R.id.phonebookFragment); TestSupport.settle()
        val frag = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).childFragmentManager.primaryNavigationFragment!!
        val rv = frag.requireView().let { v -> generateSequence(listOf(v)) { l -> l.flatMap { (it as? ViewGroup)?.let { g -> (0 until g.childCount).map(g::getChildAt) } ?: emptyList() }.ifEmpty { null } }.flatten() }
            .filterIsInstance<androidx.recyclerview.widget.RecyclerView>().firstOrNull()
        val edit = frag.requireView().let { v -> generateSequence(listOf(v)) { l -> l.flatMap { (it as? ViewGroup)?.let { g -> (0 until g.childCount).map(g::getChildAt) } ?: emptyList() }.ifEmpty { null } }.flatten() }
            .filterIsInstance<android.widget.EditText>().firstOrNull()
        edit?.setText("kov"); TestSupport.settle()
        edit?.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH); TestSupport.settle()
        shot("phonebook_search", c.get())

        val p = start(InstitutionListActivity::class.java, Intent(app, InstitutionListActivity::class.java)
            .putExtra("fragmentTitle", "Places").putExtra("autoload", true).putExtra("headerEnabled", true)
            .putExtra("hideBottomLayout", false).putExtra("searchIsForSport", false))
        shot("places", p.get())
    }

    @Test fun moodScreens() {
        TestSupport.resetApp(true)
        val holder = start(ActivityMoodHolder::class.java)
        shot("mood_statistics", holder.get())
        val pager = holder.get().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.fragmentPager)
        pager.setCurrentItem(1, false); TestSupport.settle(); shot("mood_rewards", holder.get())
        pager.setCurrentItem(2, false); TestSupport.settle(); shot("mood_history", holder.get())
        shot("mood_add", start(HappyChallengeActivity::class.java).get())
        shot("mood_levelup", start(ActivityMoodLevelUpgrade::class.java).get())
        shot("mood_ended", start(ActivityMoodGameEnded::class.java).get())
        shot("mood_form", start(ActivityMoodForm::class.java).get())
    }

    @Test fun communityScreens() {
        TestSupport.resetApp(true)
        shot("wizard_welcome", start(WizardContainerActivity::class.java).get())
        register()
        val c = start(CommunityActivity::class.java)
        shot("community_interests", c.get())
        val frag = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).childFragmentManager.primaryNavigationFragment!!
        FragmentMemberListBinding.bind(frag.requireView()).tabLayout.getTabAt(1)!!.select(); TestSupport.settle()
        shot("community_suggestions", c.get())
        val id = runBlocking { ServiceLocator.backend.searchSuggestions() }.first().id!!
        shot("member_detail", start(MemberDetailActivity::class.java, Intent(app, MemberDetailActivity::class.java).putExtra("memberId", id)).get())
        shot("self_profile", start(SelfProfileDisplayAndEditActivity::class.java).get())
    }

    @Test fun instructorConsole() {
        TestSupport.resetApp(true, instructor = true)
        val c = start(hu.unideb.studyversity.ui.instructor.InstructorActivity::class.java)
        val pager = c.get().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.instructorPager)
        for ((i, name) in listOf("instructor_student", "instructor_exams", "instructor_timetable", "instructor_email").withIndex()) {
            pager.setCurrentItem(i, false); TestSupport.settle(); shot(name, c.get())
        }
        pager.setCurrentItem(1, false); TestSupport.settle()
        val list = c.get().window.decorView.let { v -> generateSequence(listOf(v)) { l -> l.flatMap { (it as? ViewGroup)?.let { g -> (0 until g.childCount).map(g::getChildAt) } ?: emptyList() }.ifEmpty { null } }.flatten() }
            .filterIsInstance<androidx.recyclerview.widget.RecyclerView>().first { it.isShown }
        list.getChildAt(1).performClick(); TestSupport.settle()
        shot("instructor_exam_dialog", c.get())
        shot("home_instructor", start(MainActivity::class.java).get())
    }

    @Test fun login() {
        TestSupport.resetApp(false)
        shot("login", start(LoginActivity::class.java).get())
        val c = start(MainActivity::class.java)
        shot("home_logged_out", c.get())
    }
}
