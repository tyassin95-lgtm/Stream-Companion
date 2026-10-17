package hu.unideb.studyversity

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.databinding.*
import hu.unideb.studyversity.ui.community.ChipSelectionType
import hu.unideb.studyversity.ui.community.member_detail.MemberDetailActivity
import hu.unideb.studyversity.ui.community.selfview.SelfProfileDisplayAndEditActivity
import hu.unideb.studyversity.ui.community.wizard.*
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity
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

/** User journeys through the screens that change data: sign in, registration wizard, mood of the day, profile editing ... */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val backend get() = ServiceLocator.backend

    private fun <T : Activity> start(cls: Class<T>, intent: Intent = Intent(app, cls)): ActivityController<T> {
        val c = Robolectric.buildActivity(cls, intent).setup()
        TestSupport.settle()
        return c
    }

    private fun views(root: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) views(root.getChildAt(i), out)
        return out
    }

    private fun content(a: Activity): View = (a.findViewById<ViewGroup>(android.R.id.content)).getChildAt(0)

    private fun snackbar(a: Activity) = views(a.window.decorView)
        .firstOrNull { it.id == com.google.android.material.R.id.snackbar_text && it.isShown }?.let { (it as TextView).text.toString() }

    private fun dialogView(a: AppCompatActivity, tag: String, id: Int): View {
        val d = a.supportFragmentManager.findFragmentByTag(tag) as? DialogFragment ?: fail("no dialog '$tag'").let { throw IllegalStateException() }
        return d.dialog!!.findViewById(id)
    }

    private fun navFragment(a: AppCompatActivity): Fragment =
        (a.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).childFragmentManager.primaryNavigationFragment!!

    private fun recyclers(root: View) = views(root).filterIsInstance<RecyclerView>()

    // ---- sign in -----------------------------------------------------------------------------------------------
    @Test fun loginScreen() {
        TestSupport.resetApp(false)
        val c = start(LoginActivity::class.java)
        val b = ActivityLocalLoginBinding.bind(content(c.get()))

        b.loginButton.performClick(); TestSupport.settle()
        assertNotNull("blank credentials are refused", snackbar(c.get()))
        assertFalse(c.get().isFinishing)
        assertFalse(ServiceLocator.session.isLoggedIn)

        b.neptunCodeInput.setText("abc123"); b.passwordInput.setText("pw")
        b.loginButton.performClick(); TestSupport.settle()
        assertTrue(c.get().isFinishing)
        assertEquals(LoginActivity.RESULT_LOGGED_IN, shadowOf(c.get()).resultCode)
        assertEquals("ABC123", ServiceLocator.session.currentUser?.neptunCode)
    }

    @Test fun loginCancel() {
        TestSupport.resetApp(false)
        val c = start(LoginActivity::class.java)
        ActivityLocalLoginBinding.bind(content(c.get())).cancelButton.performClick()
        assertTrue(c.get().isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadowOf(c.get()).resultCode)
    }

    @Test fun profileShowsStudentAndLogoutClearsIt() {
        TestSupport.resetApp(true)
        val c = start(MainActivity::class.java)
        val nav = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        nav.navigate(R.id.profileFragment); TestSupport.settle()
        val text = views(navFragment(c.get()).requireView()).filterIsInstance<TextView>().joinToString("|") { it.text }
        assertTrue(text, text.contains("Minta Anna") || text.contains("ABC123"))
        assertTrue("balance shown: $text", text.contains("25"))
    }

    // ---- list content ------------------------------------------------------------------------------------------
    @Test fun listScreensAreFilled() {
        TestSupport.resetApp(true)
        val c = start(MainActivity::class.java)
        val nav = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        val expectList = mapOf(
            R.id.timeTableFragment to null, R.id.examsInformationFragment to null, R.id.coursesInformationFragment to null,
            R.id.libraryFragment to null, R.id.messagesFragment to null, R.id.hakFragment to null,
            R.id.studentScholarShipFragment to null, R.id.overEducationFragment to null,
            R.id.sportSubMenuFragment to null
        )
        for ((dest, _) in expectList) {
            nav.navigate(dest); TestSupport.settle()
            val counts = recyclers(navFragment(c.get()).requireView()).map { it.adapter?.itemCount ?: 0 }
            assertTrue("destination ${app.resources.getResourceEntryName(dest)} has no list rows: $counts", counts.any { it > 0 })
            nav.popBackStack(R.id.homeFragment, false); TestSupport.settle()
        }
    }

    @Test fun academicCalendarPreselectsMyFaculty() {
        TestSupport.resetApp(true)
        val c = start(MainActivity::class.java)
        val nav = (c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        nav.navigate(R.id.academicYearFragment); TestSupport.settle()
        val b = AcademicYearFragmentBinding.bind(navFragment(c.get()).requireView())
        assertEquals(5, b.mySpinnerDropdown.adapter.count)
        assertEquals("Faculty of Informatics", b.mySpinnerDropdown.text.toString())
        assertEquals(View.VISIBLE, b.webView.visibility)
    }

    // ---- registration wizard -----------------------------------------------------------------------------------
    @Test fun registrationWizardCreatesTheCommunityProfile() {
        TestSupport.resetApp(true)
        val c = start(WizardContainerActivity::class.java)
        val activity = c.get()
        FragmentWelcomeUdCommunityBinding.bind(navFragment(activity).requireView()).registrationButton.performClick()
        TestSupport.settle()

        val wizard = navFragment(activity) as WizardFragment
        val wb = FragmentWizardBinding.bind(wizard.requireView())
        fun next() { wb.nextButton.performClick(); TestSupport.settle() }
        fun page() = wb.fragmentPager.currentItem
        fun alertShown() = activity.supportFragmentManager.findFragmentByTag("wizard-alert") != null
        fun dismissAlert() {
            (activity.supportFragmentManager.findFragmentByTag("wizard-alert") as DialogFragment).dismissAllowingStateLoss(); TestSupport.settle()
        }

        // page 1 – picture
        next(); assertTrue("picture is required", alertShown()); assertEquals(0, page()); dismissAlert()
        val pictures = wizard.childFragmentManager.fragments.filterIsInstance<ProfilePictureSelectionFragment>().single()
        val pb = FragmentProfilePictureSelectionBinding.bind(pictures.requireView())
        assertEquals(8, pb.avatarHolderRv.adapter?.itemCount)
        pb.avatarHolderRv.findViewHolderForAdapterPosition(2)!!.itemView.performClick()
        pb.searchTextInputEditText.setText("Hello from the wizard")
        assertEquals("res://avatar_3", activity.viewModel.current.photoUrl)
        next(); assertEquals(1, page()); assertFalse(alertShown())

        // page 2 – interests, faculties, languages
        next(); assertTrue("interests are required", alertShown()); assertEquals(1, page()); dismissAlert()
        runBlocking {
            for (t in ChipSelectionType.values()) {
                val items = backend.categories(listOf(t.code)).take(2)
                activity.viewModel.setSelection(t, items)
            }
        }
        TestSupport.settle()
        val interests = wizard.childFragmentManager.fragments.filterIsInstance<InterestsFragment>().single()
        val ib = FragmentInterestsBinding.bind(interests.requireView())
        // 2 chips + the "add" chip per group
        assertEquals(3, ib.interestsChipGroup.childCount); assertEquals(3, ib.facultiesChipGroup.childCount); assertEquals(3, ib.languagesChipGroup.childCount)
        next(); assertEquals(2, page())

        // page 3 – social links
        next(); assertTrue("one social link is required", alertShown()); dismissAlert()
        val links = wizard.childFragmentManager.fragments.filterIsInstance<SocialLinksFragment>().single()
        FragmentSocialLinksBinding.bind(links.requireView()).facebookInput.setText("anna.minta")
        next(); assertEquals(3, page())

        // page 4 – GDPR
        next(); assertTrue("terms must be accepted", alertShown()); dismissAlert()
        val gdpr = FragmentGdprBinding.bind(wizard.childFragmentManager.fragments.filterIsInstance<GDPRFragment>().single().requireView())
        gdpr.privacyPolicySwitch.isChecked = true
        gdpr.termsOfUseSwitch.isChecked = true
        next()
        assertTrue("finish page", navFragment(activity) is WizardFinishFragment)

        val me = runBlocking { backend.myCommunityProfile() }!!
        assertEquals("res://avatar_3", me.photoUrl)
        assertEquals("Hello from the wizard", me.description)
        assertEquals("https://www.facebook.com/anna.minta", me.facebookLink)
        assertEquals(2, me.interests.size)
        assertEquals("Minta", me.lastName)

        FragmentWizardFinishBinding.bind(navFragment(activity).requireView()).finishRegistration.performClick()
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
    }

    @Test fun chipSelectorTogglesSearchesAndReturnsIds() {
        TestSupport.resetApp(true)
        val c = start(ChipSelectorActivity::class.java, ChipSelectorActivity.intent(app, ChipSelectionType.INTERESTS, listOf(103L)))
        val activity = c.get()
        val list = FragmentChipSelectorBinding.bind(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!.requireView()).chipList
        assertEquals("header + 20 interests", 21, list.adapter?.itemCount)

        fun rowName(i: Int) = list.findViewHolderForAdapterPosition(i)!!.itemView.findViewById<TextView>(R.id.chipName).text.toString()
        assertEquals("selected ones come first", "Reading", rowName(1))
        val second = rowName(2); val third = rowName(3)
        list.findViewHolderForAdapterPosition(2)!!.itemView.performClick(); TestSupport.settle()
        list.findViewHolderForAdapterPosition(3)!!.itemView.performClick(); TestSupport.settle()

        // filter by typing in the header search box; the selection survives filtering
        val search = list.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<android.widget.EditText>(R.id.searchTextInputEditText)
        search.setText("prog"); TestSupport.settle()
        assertEquals(2, list.adapter?.itemCount) // header + Programming
        search.setText(""); TestSupport.settle()
        assertEquals(21, list.adapter?.itemCount)

        val toolbar = views(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!.requireView())
            .filterIsInstance<androidx.appcompat.widget.Toolbar>().first()
        toolbar.menu.performIdentifierAction(R.id.done, 0); TestSupport.settle()
        assertTrue(activity.isFinishing)
        val ids = shadowOf(activity).resultIntent.getLongArrayExtra(ChipSelectorActivity.RESULT_IDS)!!.toSet()
        val names = runBlocking { backend.categories(listOf("interests")) }.filter { it.id in ids }.map { it.name }
        assertEquals(setOf("Reading", second, third), names.toSet())
    }

    @Test fun chipSelectorDoneReturnsTheSelection() {
        TestSupport.resetApp(true)
        val c = start(ChipSelectorActivity::class.java, ChipSelectorActivity.intent(app, ChipSelectionType.LANGUAGES, listOf(300L)))
        val activity = c.get()
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!
        val toolbar = views(fragment.requireView()).filterIsInstance<androidx.appcompat.widget.Toolbar>().first()
        val done = toolbar.menu.findItem(R.id.done)
        toolbar.menu.performIdentifierAction(done.itemId, 0)
        TestSupport.settle()
        assertTrue(activity.isFinishing)
        val result = shadowOf(activity).resultIntent
        assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
        assertEquals("LANGUAGES", result.getStringExtra(ChipSelectorActivity.RESULT_TYPE))
        assertArrayEquals(longArrayOf(300L), result.getLongArrayExtra(ChipSelectorActivity.RESULT_IDS))
    }

    // ---- mood of the day -----------------------------------------------------------------------------------------
    @Test fun moodOfTheDayFlow() {
        TestSupport.resetApp(true)
        val c = start(HappyChallengeActivity::class.java, Intent(app, HappyChallengeActivity::class.java).putExtra("showPackageChoose", true))
        val activity = c.get()
        val first = HappyChallengeFragmentBinding.bind(activity.findViewById<ViewGroup>(R.id.container).getChildAt(0))
        assertTrue(first.greetingText.text.contains("Minta Anna"))
        first.greatCard.performClick(); TestSupport.settle()

        assertEquals(1, runBlocking { backend.moods() }.count { it.moodDate!!.time > System.currentTimeMillis() - 60_000 })
        val grid = ChoosePackageFragmentBinding.bind(activity.findViewById<ViewGroup>(R.id.container).getChildAt(0)).hcGrid
        assertEquals(15, grid.adapter?.itemCount)
        grid.findViewHolderForAdapterPosition(4)!!.itemView.performClick(); TestSupport.settle()

        val quoteTitle = dialogView(activity, "quote", R.id.titleText) as TextView
        assertEquals(app.getString(R.string.quote), quoteTitle.text.toString())
        dialogView(activity, "quote", R.id.positiveButton).performClick(); TestSupport.settle()
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
    }

    @Test fun moodSheetRefusesASecondAnswerOnTheSameDay() {
        TestSupport.resetApp(true)
        runBlocking { backend.saveMood(hu.unideb.studyversity.data.model.MoodItem(moodValue = "NORMAL")) }
        val c = start(HappyChallengeActivity::class.java)
        val activity = c.get()
        HappyChallengeFragmentBinding.bind(activity.findViewById<ViewGroup>(R.id.container).getChildAt(0)).badCard.performClick()
        TestSupport.settle()
        val message = dialogView(activity, "answered", R.id.messageText) as TextView
        assertEquals("You have already told us how you feel today.", message.text.toString())
    }

    // ---- community ---------------------------------------------------------------------------------------------
    private fun registerMyself(): CommunityMember = runBlocking {
        val cats = backend.categories(emptyList())
        backend.registerCommunityMember(CommunityMember(
            photoUrl = "res://avatar_2", description = "About me",
            interests = cats.filter { it.parentCategoryCode == "interests" }.take(3),
            faculties = cats.filter { it.parentCategoryCode == "faculty_names" }.take(1),
            preferredLanguages = cats.filter { it.parentCategoryCode == "languages" }.take(2), facebookLink = "https://www.facebook.com/x"
        ))
    }

    @Test fun communityListShowsSuggestionsAndMyInterests() {
        TestSupport.resetApp(true)
        registerMyself()
        val c = start(hu.unideb.studyversity.ui.community.CommunityActivity::class.java)
        val fragment = c.get().supportFragmentManager.findFragmentById(R.id.nav_host_fragment).let {
            (it as NavHostFragment).childFragmentManager.primaryNavigationFragment!!
        }
        val b = FragmentMemberListBinding.bind(fragment.requireView())
        assertEquals("nobody marked yet: header only", 1, b.memberList.adapter?.itemCount)
        b.tabLayout.getTabAt(1)!!.select(); TestSupport.settle()
        assertTrue("header + suggestions: ${b.memberList.adapter?.itemCount}", b.memberList.adapter!!.itemCount > 3)
    }

    @Test fun communityWithoutProfileOpensTheWizard() {
        TestSupport.resetApp(true)
        val c = start(hu.unideb.studyversity.ui.community.CommunityActivity::class.java)
        assertEquals(WizardContainerActivity::class.java.name, shadowOf(app).nextStartedActivity?.component?.className)
    }

    @Test fun editingMyProfile() {
        TestSupport.resetApp(true)
        registerMyself()
        val c = start(SelfProfileDisplayAndEditActivity::class.java)
        val activity = c.get()
        val b = ActivityEditSelfProfileBinding.bind(content(activity))
        assertEquals("Minta Anna", b.nameText.text.toString())
        assertEquals("About me", b.descriptionText.text.toString())
        assertEquals(View.GONE, b.descriptionInputLayout.visibility)
        assertEquals(View.VISIBLE, b.socialButtonsRow.visibility)

        shadowOf(activity).clickMenuItem(R.id.edit); TestSupport.settle()
        assertEquals(View.VISIBLE, b.descriptionInputLayout.visibility)
        assertEquals(View.GONE, b.socialButtonsRow.visibility)
        assertEquals("About me", b.searchTextInputEditText.text.toString())
        assertEquals("3 chips + add chip", 4, b.interestsChipGroup.childCount)
        b.searchTextInputEditText.setText("Changed text")
        b.instagramInput.setText("annam")
        shadowOf(activity).clickMenuItem(R.id.done); TestSupport.settle()

        val saved = runBlocking { backend.myCommunityProfile() }!!
        assertEquals("Changed text", saved.description)
        assertEquals("https://www.instagram.com/_u/annam", saved.instagramLink)
        assertEquals(View.GONE, b.descriptionInputLayout.visibility)
        assertEquals("Changed text", b.descriptionText.text.toString())
    }

    @Test fun editingRequiresAtLeastOneInterest() {
        TestSupport.resetApp(true)
        registerMyself()
        val c = start(SelfProfileDisplayAndEditActivity::class.java)
        val activity = c.get()
        shadowOf(activity).clickMenuItem(R.id.edit); TestSupport.settle()
        val vm = androidx.lifecycle.ViewModelProvider(activity)[WizardViewModel::class.java]
        vm.setSelection(ChipSelectionType.INTERESTS, emptyList()); TestSupport.settle()
        shadowOf(activity).clickMenuItem(R.id.done); TestSupport.settle()
        assertEquals(app.getString(R.string.interests_and_faculties_error_msg), snackbar(activity))
        assertEquals(3, runBlocking { backend.myCommunityProfile() }!!.interests.size)
    }

    @Test fun deletingMyProfile() {
        TestSupport.resetApp(true)
        registerMyself()
        val c = start(SelfProfileDisplayAndEditActivity::class.java)
        val activity = c.get()
        shadowOf(activity).clickMenuItem(R.id.edit); TestSupport.settle()
        ActivityEditSelfProfileBinding.bind(content(activity)).deleteAccountButton.performClick(); TestSupport.settle()
        // the confirm dialog is a CustomViewDialog without tag-less access: find it through the fragment manager
        val dialog = activity.supportFragmentManager.fragments.filterIsInstance<DialogFragment>().single()
        dialog.dialog!!.findViewById<View>(R.id.positiveButton).performClick(); TestSupport.settle()
        assertNull(runBlocking { backend.myCommunityProfile() })
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_FIRST_USER, shadowOf(activity).resultCode)
    }

    @Test fun markingAStudentAsInteresting() {
        TestSupport.resetApp(true)
        registerMyself()
        val id = runBlocking { backend.searchSuggestions() }.first().id!!
        val c = start(MemberDetailActivity::class.java, Intent(app, MemberDetailActivity::class.java).putExtra("memberId", id))
        val activity = c.get()
        val b = ActivityMemberDetailBinding.bind(content(activity))
        assertEquals(app.getString(R.string.mark_as_my_interest_button), b.markAsFriendButton.text.toString())
        b.markAsFriendButton.performClick(); TestSupport.settle()
        dialogView(activity, "mark", R.id.positiveButton).performClick(); TestSupport.settle()
        assertEquals(listOf(id), runBlocking { backend.searchMyInterests() }.map { it.id })
        assertEquals(app.getString(R.string.remove_from_my_interest_button), b.markAsFriendButton.text.toString())
        b.markAsFriendButton.performClick(); TestSupport.settle()
        assertTrue(runBlocking { backend.searchMyInterests() }.isEmpty())
        assertTrue(activity.isFinishing)
    }
}
