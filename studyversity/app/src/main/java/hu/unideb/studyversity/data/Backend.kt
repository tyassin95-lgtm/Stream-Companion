package hu.unideb.studyversity.data

import hu.unideb.studyversity.data.model.*
import java.util.Date

class AuthRequiredException : Exception("Authentication required")
class InvalidCredentialsException : Exception("Invalid credentials")
class HappyChallengeTodayAnsweredException(message: String = "") : Exception(message)
class NoMoodGameException : Exception("No mood game")

/**
 * Everything the UI needs from a server. The app currently runs on [hu.unideb.studyversity.data.local.LocalBackend]
 * (seed data + on-device persistence). To connect a real server implement this interface on top of
 * [hu.unideb.studyversity.data.remote.ApiService] (see RemoteBackend) and flip BackendConfig.USE_REMOTE.
 *
 * Methods that need a signed-in user throw [AuthRequiredException] when nobody is logged in.
 */
interface Backend {
    // ---- session -----------------------------------------------------------------------------------------------
    suspend fun login(userName: String, password: String): User
    suspend fun logout()
    /** The signed-in user or null. */
    suspend fun currentUser(): User?

    // ---- student data (Neptun) ---------------------------------------------------------------------------------
    suspend fun trainings(): List<StudentTrainingItem>
    suspend fun timetable(): List<TimeTableItem>
    suspend fun courses(): List<StudentCourseItem>
    suspend fun exams(): List<StudentExamItem>
    suspend fun modules(): List<StudentModuleItem>

    // ---- university content ------------------------------------------------------------------------------------
    suspend fun academicYears(): List<AcademicYearItem>
    suspend fun scholarships(): List<StudentScholarShipItem>
    suspend fun rss(channel: NewsChannel): List<RssFeedItem>
    suspend fun searchEmployees(name: String, startRow: Int, endRow: Int): List<PhoneBookSearchItem>
    suspend fun employeeDetail(id: Long): EmployeeDetailItem?
    suspend fun mapCategories(parentCodes: List<String>?): List<MapCategoryItem>
    suspend fun institutions(categoryCodes: List<String>?, name: String?): List<CachedInstitution>
    suspend fun institution(poiId: Long): Institution?
    suspend fun loanedBooks(): List<LibraryLoanedContainerItem>
    suspend fun searchLibrary(term: String, startRow: Int, endRow: Int): List<LibrarySearchItem>

    // ---- messages & device ------------------------------------------------------------------------------------
    suspend fun messages(): List<MessageListItem>
    suspend fun message(id: Long): MessageDetail?
    suspend fun registerDevice(token: String)
    suspend fun devicePreferences(): DevicePreference?
    suspend fun saveDevicePreferences(p: DevicePreference): DevicePreference

    // ---- mood game / happy challenge -----------------------------------------------------------------------
    suspend fun randomQuote(): QuoteItem?
    suspend fun saveMood(mood: MoodItem)
    suspend fun moods(): List<MoodItem>
    suspend fun moodSummary(from: Date, to: Date): List<MoodCountItem>
    suspend fun moodPoints(): PrizeItem?
    suspend fun myCurrentGame(): StudentMoodGame
    suspend fun moodGameStages(): List<MoodGameStage>
    suspend fun chooseGift(stageId: Int, giftId: Int)
    suspend fun registerMoodGame()

    // ---- community ---------------------------------------------------------------------------------------------
    suspend fun myCommunityProfile(): CommunityMember?
    suspend fun registerCommunityMember(m: CommunityMember): CommunityMember
    suspend fun updateCommunityMember(m: CommunityMember): CommunityMember
    suspend fun deleteCommunityProfile(id: Long)
    suspend fun communityMember(id: Long): CommunityMember?
    suspend fun searchMyInterests(): List<CommunityMember>
    suspend fun searchSuggestions(): List<CommunityMember>
    suspend fun addToMyInterests(id: Long): CommunityMember?
    suspend fun removeFromMyInterests(id: Long)
    suspend fun categories(parentCodes: List<String>): List<CategoryItem>
    suspend fun uploadProfileImage(bytes: ByteArray): String
}
