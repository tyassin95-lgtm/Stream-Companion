package hu.unideb.studyversity.data

import hu.unideb.studyversity.data.model.*
import java.util.Date

class AuthRequiredException : Exception("Authentication required")
class InvalidCredentialsException : Exception("Invalid credentials")
class HappyChallengeTodayAnsweredException(message: String = "") : Exception(message)
class NoMoodGameException : Exception("No mood game")
/** The signed-in user has the wrong role for the call (e.g. an instructor asking for student data). */
class AccessDeniedException : Exception("Access denied")

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

    // ---- instructor console (role INSTRUCTOR only; otherwise AccessDeniedException) -----------------------------
    /** Students the instructor looks after. */
    suspend fun managedStudents(): List<StaffStudent>
    suspend fun updateStudent(update: StudentUpdate): StaffStudent
    /** Every exam / grade record of the student, newest first; unopened exams included. */
    suspend fun studentExams(studentId: Long): List<StaffExam>
    /** Creates (id == null) or changes an exam: its date, whether it is open for the student, and the grade. */
    suspend fun saveStudentExam(studentId: Long, exam: StaffExam): StaffExam
    suspend fun deleteStudentExam(studentId: Long, examId: Long)
    /** Distinct courses of the student, to open an exam for. */
    suspend fun studentCourseOptions(studentId: Long): List<CourseOption>
    suspend fun studentClasses(studentId: Long): List<TimeTableItem>
    suspend fun addStudentClass(studentId: Long, item: TimeTableItem): TimeTableItem
    suspend fun deleteStudentClass(studentId: Long, classId: Long)
    /** Sends an e-mail to the student; it arrives in the student's messages. */
    suspend fun sendEmail(studentId: Long, subject: String, body: String): MailItem
    suspend fun sentEmails(studentId: Long): List<MailItem>
}
