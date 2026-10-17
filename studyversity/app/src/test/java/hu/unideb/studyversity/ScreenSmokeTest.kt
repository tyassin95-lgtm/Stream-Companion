package hu.unideb.studyversity

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ApplicationProvider
import hu.unideb.studyversity.ui.community.CommunityActivity
import hu.unideb.studyversity.ui.community.ChipSelectionType
import hu.unideb.studyversity.ui.community.member_detail.MemberDetailActivity
import hu.unideb.studyversity.ui.community.selfview.SelfProfileDisplayAndEditActivity
import hu.unideb.studyversity.ui.community.wizard.ChipSelectorActivity
import hu.unideb.studyversity.ui.community.wizard.WizardContainerActivity
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity
import hu.unideb.studyversity.ui.main.MainActivity
import hu.unideb.studyversity.ui.mood.ActivityMoodHolder
import hu.unideb.studyversity.ui.mood.form.ActivityMoodForm
import hu.unideb.studyversity.ui.mood.gameEnded.ActivityMoodGameEnded
import hu.unideb.studyversity.ui.mood.levelUpgrade.ActivityMoodLevelUpgrade
import hu.unideb.studyversity.ui.splash.SplashActivity
import hu.unideb.studyversity.ui.university_matters.university_locale.InstitutionListActivity
import hu.unideb.studyversity.ui.user.login.LoginActivity
import hu.unideb.studyversity.ui.webview.WebViewActivity
import hu.unideb.studyversity.utils.image.ImageShowActivity
import hu.unideb.studyversity.utils.image.ImageSlideShowActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * There is no emulator in the build environment, so every screen is inflated and driven under Robolectric:
 * each Activity is started, every home menu entry is clicked, and every navigation destination is opened with the
 * arguments its caller passes. A screen "fails" when it throws or when it ends up showing an error snackbar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenSmokeTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    // ---- helpers ---------------------------------------------------------------------------------------------
    private fun <T : Activity> start(cls: Class<T>, intent: Intent = Intent(app, cls)): ActivityController<T> {
        val c = Robolectric.buildActivity(cls, intent).setup()
        TestSupport.settle()
        return c
    }

    private fun allViews(root: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) allViews(root.getChildAt(i), out)
        return out
    }

    /** Text of a visible snackbar, if any (the apps shows errors there). */
    private fun snackbar(activity: Activity): String? =
        allViews(activity.window.decorView).firstOrNull { it.id == com.google.android.material.R.id.snackbar_text && it.isShown }
            ?.let { (it as TextView).text.toString() }

    private fun assertHealthy(activity: Activity, what: String) {
        assertNull("$what shows an error: ${snackbar(activity)}", snackbar(activity))
    }

    private fun navController(a: FragmentActivity): NavController =
        (a.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    private fun nextActivity(): Intent? = shadowOf(app).nextStartedActivity

    // ---- every activity ----------------------------------------------------------------------------------------
    private fun launchAll(loggedIn: Boolean) {
        TestSupport.resetApp(loggedIn)
        val cases: List<Pair<String, () -> Pair<Class<out Activity>, Intent>>> = listOf(
            "Main" to { MainActivity::class.java to Intent(app, MainActivity::class.java) },
            "Login" to { LoginActivity::class.java to Intent(app, LoginActivity::class.java) },
            "ImageShow" to { ImageShowActivity::class.java to Intent(app, ImageShowActivity::class.java).putExtra("imageResource", "res://avatar_1") },
            "ImageSlideShow" to { ImageSlideShowActivity::class.java to Intent(app, ImageSlideShowActivity::class.java).putExtra("imageUrls", arrayOf("res://avatar_1", "res://avatar_2")) },
            "WebView" to { WebViewActivity::class.java to Intent(app, WebViewActivity::class.java).putExtra("url", "about:blank") },
            "InstitutionList" to { InstitutionListActivity::class.java to Intent(app, InstitutionListActivity::class.java)
                .putExtra("fragmentTitle", "Places").putExtra("categorieCodes", arrayOf("sport")).putExtra("autoload", true)
                .putExtra("headerEnabled", true).putExtra("hideBottomLayout", false).putExtra("searchIsForSport", true) },
            "Community" to { CommunityActivity::class.java to Intent(app, CommunityActivity::class.java) },
            "Wizard" to { WizardContainerActivity::class.java to Intent(app, WizardContainerActivity::class.java) },
            "ChipSelector" to { ChipSelectorActivity::class.java to ChipSelectorActivity.intent(app, ChipSelectionType.INTERESTS, listOf(100L)) },
            "SelfProfile" to { SelfProfileDisplayAndEditActivity::class.java to Intent(app, SelfProfileDisplayAndEditActivity::class.java) },
            "MemberDetail" to { MemberDetailActivity::class.java to Intent(app, MemberDetailActivity::class.java).putExtra("memberId", 1000L) },
            "MoodHolder" to { ActivityMoodHolder::class.java to Intent(app, ActivityMoodHolder::class.java) },
            "HappyChallenge" to { HappyChallengeActivity::class.java to Intent(app, HappyChallengeActivity::class.java).putExtra("showPackageChoose", true) },
            "MoodForm" to { ActivityMoodForm::class.java to Intent(app, ActivityMoodForm::class.java) },
            "MoodLevelUpgrade" to { ActivityMoodLevelUpgrade::class.java to Intent(app, ActivityMoodLevelUpgrade::class.java) },
            "MoodGameEnded" to { ActivityMoodGameEnded::class.java to Intent(app, ActivityMoodGameEnded::class.java) }
        )
        for ((name, make) in cases) {
            val (cls, intent) = make()
            val c = try { start(cls, intent) } catch (e: Throwable) { throw AssertionError("$name (loggedIn=$loggedIn) crashed", e) }
            // member profiles are only reachable from the community list, which signs in first
            if (loggedIn || name != "MemberDetail") assertHealthy(c.get(), "$name (loggedIn=$loggedIn)")
            c.pause().stop().destroy()
        }
    }

    @Test fun everyActivityStartsLoggedIn() = launchAll(loggedIn = true)
    @Test fun everyActivityStartsLoggedOut() = launchAll(loggedIn = false)

    @Test fun splashForwardsToMain() {
        TestSupport.resetApp(false)
        val c = Robolectric.buildActivity(SplashActivity::class.java).setup()
        assertTrue(c.get().isFinishing)
        assertEquals(MainActivity::class.java.name, nextActivity()?.component?.className)
    }

    // ---- main navigation ---------------------------------------------------------------------------------------
    private data class Dest(val id: Int, val args: Bundle? = null)

    private fun destinations(): List<Pair<String, Dest>> = listOf(
        "profile" to Dest(R.id.profileFragment),
        "timeTable" to Dest(R.id.timeTableFragment),
        "majorDetails" to Dest(R.id.majorDetailsFragment),
        "exams" to Dest(R.id.examsInformationFragment),
        "courses" to Dest(R.id.coursesInformationFragment),
        "timeTableNavigation" to Dest(R.id.timeTableNavigationFragment),
        "phonebook" to Dest(R.id.phonebookFragment),
        "employee" to Dest(R.id.employeeDetailFragment, Bundle().apply { putLong("phoneBookId", 3L) }),
        "library" to Dest(R.id.libraryFragment),
        "librarySearch" to Dest(R.id.librarySearchFragment),
        "overEducation" to Dest(R.id.overEducationFragment),
        "hak" to Dest(R.id.hakFragment),
        "scholarship" to Dest(R.id.studentScholarShipFragment),
        "academicYear" to Dest(R.id.academicYearFragment),
        "contact" to Dest(R.id.contactFragment),
        "settings" to Dest(R.id.settingsFragment),
        "messages" to Dest(R.id.messagesFragment),
        "sportSub" to Dest(R.id.sportSubMenuFragment),
        "dexpoFaculties" to Dest(R.id.dexpoSubMenuFragment, Bundle().apply { putString("type", "FACULTIES"); putString("title", "Faculties") }),
        "dexpoPrograms" to Dest(R.id.dexpoSubMenuFragment, Bundle().apply { putString("type", "PROGRAMS"); putString("title", "Programs") }),
        "dexpoLocation" to Dest(R.id.dexpoSubMenuFragment, Bundle().apply { putString("type", "LOCATION_INFORMATION"); putString("title", "Location") }),
        "dexpoSocial" to Dest(R.id.dexpoSubMenuFragment, Bundle().apply { putString("type", "SOCIAL_MEDIA"); putString("title", "Social") }),
        "rssNews" to Dest(R.id.rssListFragment, Bundle().apply { putInt("list_item_layout", R.layout.news_rss_list_item); putString("fragmentTitle", "News"); putIntArray("channelID", intArrayOf(119)) }),
        "rssEvents" to Dest(R.id.rssListFragment, Bundle().apply { putInt("list_item_layout", R.layout.events_rss_list_item); putString("fragmentTitle", "Events"); putIntArray("channelID", intArrayOf(120)) }),
        "webUrl" to Dest(R.id.webViewFragment, Bundle().apply { putString("fileName", "about:blank"); putString("fileMode", "URL"); putString("title", "Web") }),
        "webHtml" to Dest(R.id.webViewFragment, Bundle().apply { putString("fileName", "<p>hello</p>"); putString("fileMode", "HTML_STRING"); putString("title", "Html") })
    )

    private fun openDestinations(loggedIn: Boolean) {
        TestSupport.resetApp(loggedIn)
        val c = start(MainActivity::class.java)
        val nav = navController(c.get())
        for ((name, d) in destinations()) {
            try {
                nav.navigate(d.id, d.args)
                TestSupport.settle()
            } catch (e: Throwable) {
                throw AssertionError("destination $name (loggedIn=$loggedIn) crashed", e)
            }
            assertEquals("destination $name", d.id, nav.currentDestination?.id)
            val fragment = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                .childFragmentManager.primaryNavigationFragment
            assertNotNull("destination $name has no fragment", fragment?.view)
            if (loggedIn) assertHealthy(c.get(), "destination $name")
            else nextActivity() // login screens launched for protected data are expected
            nav.popBackStack(R.id.homeFragment, false)
            TestSupport.settle()
        }
    }

    @Test fun everyDestinationOpensLoggedIn() = openDestinations(loggedIn = true)
    @Test fun everyDestinationOpensLoggedOut() = openDestinations(loggedIn = false)

    @Test fun protectedDestinationsAskForLoginWhenLoggedOut() {
        TestSupport.resetApp(false)
        val c = start(MainActivity::class.java)
        val nav = navController(c.get())
        for (dest in listOf(R.id.profileFragment, R.id.timeTableFragment, R.id.examsInformationFragment, R.id.coursesInformationFragment, R.id.libraryFragment, R.id.messagesFragment)) {
            while (nextActivity() != null) { /* drain */ }
            nav.navigate(dest)
            TestSupport.settle()
            val started = nextActivity()
            assertEquals("destination $dest", LoginActivity::class.java.name, started?.component?.className)
            nav.popBackStack(R.id.homeFragment, false)
        }
    }

    // ---- home menu ---------------------------------------------------------------------------------------------
    private fun clickEveryMenuEntry(loggedIn: Boolean, minimum: Int = 27) {
        TestSupport.resetApp(loggedIn)
        val c = start(MainActivity::class.java)
        val nav = navController(c.get())
        val home = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .childFragmentManager.primaryNavigationFragment!!
        val entries = allViews(home.requireView()).filter { it.id == R.id.sub_menu_parent }
        assertTrue("menu entries: ${entries.size}", entries.size >= minimum)
        val visible = entries.count { (it.parent as View).isShown || (it.parent as View).visibility == View.VISIBLE }
        assertTrue(visible > 0)
        entries.forEachIndexed { i, entry ->
            if ((entry.parent as View).visibility != View.VISIBLE) return@forEachIndexed
            while (nextActivity() != null) { /* drain */ }
            try {
                entry.performClick()
                TestSupport.settle()
            } catch (e: Throwable) {
                val label = allViews(entry).filterIsInstance<TextView>().firstOrNull()?.text
                throw AssertionError("menu entry #$i \"$label\" (loggedIn=$loggedIn) crashed", e)
            }
            if (loggedIn) assertHealthy(c.get(), "menu entry #$i")
            nav.popBackStack(R.id.homeFragment, false)
            TestSupport.settle()
        }
    }

    @Test fun everyMenuEntryWorksLoggedIn() = clickEveryMenuEntry(loggedIn = true)
    @Test fun everyMenuEntryWorksLoggedOut() = clickEveryMenuEntry(loggedIn = false)

    /** Hungarian shows the long DEXPO section (10 entries instead of 3). */
    @Test @Config(qualifiers = "hu-rHU-w411dp-h891dp-xxhdpi")
    fun everyMenuEntryWorksInHungarian() = clickEveryMenuEntry(loggedIn = true, minimum = 34)

    @Test fun logoutEntryOnlyWhenLoggedIn() {
        for (loggedIn in listOf(true, false)) {
            TestSupport.resetApp(loggedIn)
            val c = start(MainActivity::class.java)
            val home = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                .childFragmentManager.primaryNavigationFragment!!
            val titles = allViews(home.requireView()).filter { it.id == R.id.menu_item_title && (it.parent.parent as View).visibility == View.VISIBLE }
                .map { (it as TextView).text.toString() }
            assertEquals("logout visible (loggedIn=$loggedIn)", loggedIn, titles.contains(app.getString(R.string.menu_logout)))
        }
    }
}
