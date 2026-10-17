package hu.unideb.studyversity

import androidx.test.core.app.ApplicationProvider
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.*
import hu.unideb.studyversity.data.local.LocalBackend
import hu.unideb.studyversity.data.model.*
import hu.unideb.studyversity.ui.community.wizard.SocialLinks
import hu.unideb.studyversity.ui.happy_challenge.MoodCommons
import hu.unideb.studyversity.ui.happy_challenge.MoodScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = StudyversityApp::class)
class LocalBackendTest {
    private val backend get() = ServiceLocator.backend

    @Before fun setUp() = TestSupport.resetApp(loggedIn = false)

    @Test fun personalDataNeedsLogin() = runBlocking {
        assertNull(backend.currentUser())
        for (call in listOf<suspend () -> Any?>(
            { backend.trainings() }, { backend.timetable() }, { backend.courses() }, { backend.exams() }, { backend.modules() },
            { backend.loanedBooks() }, { backend.messages() }, { backend.myCurrentGame() }, { backend.myCommunityProfile() }
        )) {
            try { call(); fail("expected AuthRequiredException") } catch (e: AuthRequiredException) { /* ok */ }
        }
    }

    @Test fun publicContentNeedsNoLogin() = runBlocking {
        assertTrue(backend.academicYears().isNotEmpty())
        assertTrue(backend.scholarships().isNotEmpty())
        NewsChannel.values().forEach { assertTrue(it.name, backend.rss(it).isNotEmpty()) }
        assertTrue(backend.institutions(null, null).isNotEmpty())
        assertTrue(backend.mapCategories(null).isNotEmpty())
        assertTrue(backend.searchLibrary("algorithms", 0, 20).isNotEmpty())
        Unit
    }

    @Test fun loginRejectsBlankCredentials() = runBlocking {
        try { backend.login("", "x"); fail() } catch (e: InvalidCredentialsException) { }
        val u = backend.login("abc123", "pw")
        assertEquals("ABC123", u.neptunCode)
        assertEquals("ABC123", backend.currentUser()?.neptunCode)
        backend.logout()
        assertNull(backend.currentUser())
    }

    @Test fun studentDataIsConsistent() = runBlocking {
        backend.login("ABC123", "pw")
        assertTrue(backend.trainings().isNotEmpty())
        val tt = backend.timetable()
        assertTrue(tt.isNotEmpty())
        assertTrue(tt.all { it.classStart!! < it.classEnd!! })
        val semesters = backend.courses().mapNotNull { it.semester }.distinct()
        assertEquals(5, semesters.size)
        // the semester shown first in the exam list has results; the running one has upcoming exams
        assertTrue(backend.exams().any { it.examValue == null && it.examDate!! > Date() })
        Unit
    }

    @Test fun phoneBookPagesAndFilters() = runBlocking {
        val first = backend.searchEmployees("", 0, 20)
        assertEquals(20, first.size)
        val rest = backend.searchEmployees("", 20, 30)
        assertTrue(rest.isNotEmpty() && rest.none { r -> first.any { it.id == r.id } })
        val hits = backend.searchEmployees("szabo", 0, 20)   // accent / case insensitive
        assertTrue(hits.any { it.name == "Szabó Réka" })
        assertNotNull(backend.employeeDetail(hits.first().id))
    }

    @Test fun onlyOneMoodPerDay() = runBlocking {
        backend.login("ABC123", "pw")
        backend.saveMood(MoodItem(moodValue = "GREAT"))
        try { backend.saveMood(MoodItem(moodValue = "BAD")); fail() } catch (e: HappyChallengeTodayAnsweredException) { }
        val today = backend.moodSummary(Date(), Date())
        assertEquals("1", today.first { it.mood == "GREAT" }.count)
        assertEquals(21, backend.myCurrentGame().reportedDays.size) // 20 seeded + today
    }

    @Test fun moodGameNeedsRegistrationWhenNoGameExists() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<StudyversityApp>()
        ctx.getSharedPreferences("studyversity_local_backend", 0).edit()
            .putString("state", """{"neptun":"ABC123","readMessages":[],"moods":[],"interests":[]}""").commit()
        val fresh = LocalBackend(ctx, ServiceLocator.gson)
        try { fresh.myCurrentGame(); fail() } catch (e: NoMoodGameException) { }
        fresh.registerMoodGame()
        val g = fresh.myCurrentGame()
        assertEquals(1, g.currentStage?.stageOrder)
        assertEquals(2, g.nextStage?.stageOrder)
        assertTrue(g.reportedDays.isEmpty())
    }

    @Test fun levelsAdvanceAndGameEnds() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<StudyversityApp>()
        // stage 5 with 72 of 73 days done: the next mood completes the last level
        val days = (72 downTo 1).map { d -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }.timeInMillis }
        val json = """{"neptun":"ABC123","readMessages":[],"moods":[],"interests":[],"game":{"start":"2024-01-01T00:00:00.000+0000",
            "stage":5,"stageStart":"2024-06-01T00:00:00.000+0000","reported":[${days.joinToString { "\"" + java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(it) + "\"" }}],
            "completed":[]}}"""
        ctx.getSharedPreferences("studyversity_local_backend", 0).edit().putString("state", json).commit()
        val b = LocalBackend(ctx, ServiceLocator.gson)
        val before = b.myCurrentGame()
        assertEquals(5, before.currentStage?.stageOrder)
        assertEquals(72, before.reportedDays.size)
        b.saveMood(MoodItem(moodValue = "GREAT"))
        val after = b.myCurrentGame()
        assertEquals(6, after.currentStage?.stageOrder)
        assertNull(after.nextStage)
        assertEquals(1, after.completedStages.size)
        assertEquals(MoodScreen.GAME_ENDED, MoodCommons(ServiceLocator.prefs).processStudentMoodGame(after))
        // the reward of the finished level can be picked once
        val gift = after.completedStages.first().gifts.first()
        b.chooseGift(5, gift.id!!)
        b.chooseGift(5, after.completedStages.first().gifts.last().id!!)
        assertEquals(gift.id, b.myCurrentGame().completedStages.first().chosenGift?.id)
    }

    @Test fun levelUpgradeIsDetectedFromStoredStage() = runBlocking {
        backend.login("ABC123", "pw")
        val commons = MoodCommons(ServiceLocator.prefs)
        val game = backend.myCurrentGame()
        // first time the game is seen with stage 2 while stage 1 is remembered → level up
        ServiceLocator.prefs.lastCurrentGame = game.copy(currentStage = game.currentStage?.copy(stageOrder = 1))
        assertEquals(MoodScreen.LEVEL_UPGRADE, commons.processStudentMoodGame(game))
        assertEquals(MoodScreen.NONE, commons.processStudentMoodGame(game))
    }

    @Test fun communitySuggestionsFollowSharedCategories() = runBlocking {
        backend.login("ABC123", "pw")
        assertNull(backend.myCommunityProfile())
        val cats = backend.categories(listOf("interests", "faculty_names", "languages"))
        val interest = cats.first { it.parentCategoryCode == "interests" }
        val faculty = cats.first { it.parentCategoryCode == "faculty_names" }
        val language = cats.first { it.parentCategoryCode == "languages" }
        val me = backend.registerCommunityMember(
            CommunityMember(photoUrl = "res://avatar_1", interests = listOf(interest), faculties = listOf(faculty), preferredLanguages = listOf(language))
        )
        assertEquals("Minta", me.lastName)
        assertEquals(me, backend.myCommunityProfile())
        val suggestions = backend.searchSuggestions()
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all { s -> s.interests.any { it.id == interest.id } || s.faculties.any { it.id == faculty.id } || s.preferredLanguages.any { it.id == language.id } })
        val pick = suggestions.first()
        assertTrue(backend.searchMyInterests().isEmpty())
        backend.addToMyInterests(pick.id!!)
        assertEquals(listOf(pick.id), backend.searchMyInterests().map { it.id })
        assertTrue(backend.searchSuggestions().none { it.id == pick.id })
        backend.removeFromMyInterests(pick.id!!)
        assertTrue(backend.searchMyInterests().isEmpty())
        backend.deleteCommunityProfile(me.id!!)
        assertNull(backend.myCommunityProfile())
    }

    @Test fun messagesBecomeRead() = runBlocking {
        backend.login("ABC123", "pw")
        val unread = backend.messages().filter { !it.isRead }
        assertTrue(unread.isNotEmpty())
        backend.message(unread.first().id!!)
        assertEquals(unread.size - 1, backend.messages().count { !it.isRead })
    }

    @Test fun stateSurvivesRecreation() = runBlocking {
        backend.login("ABC123", "pw")
        backend.saveMood(MoodItem(moodValue = "NORMAL"))
        val again = LocalBackend(ApplicationProvider.getApplicationContext<StudyversityApp>(), ServiceLocator.gson)
        assertEquals("ABC123", again.currentUser()?.neptunCode)
        try { again.saveMood(MoodItem(moodValue = "BAD")); fail() } catch (e: HappyChallengeTodayAnsweredException) { }
    }

    @Test fun socialLinksGetPrefixesLikeTheOriginal() {
        val m = SocialLinks.normalise(CommunityMember(
            facebookLink = "john.doe", linkedInLink = "https://www.linkedin.com/in/jd", instagramLink = " jd ", tiktokLink = ""
        ))
        assertEquals("https://www.facebook.com/john.doe", m.facebookLink)
        assertEquals("https://www.linkedin.com/in/jd", m.linkedInLink)
        assertEquals("https://www.instagram.com/_u/jd", m.instagramLink)
        assertEquals("", m.tiktokLink)
    }
}
