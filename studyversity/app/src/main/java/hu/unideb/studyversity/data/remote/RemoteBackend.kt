package hu.unideb.studyversity.data.remote

import com.google.gson.Gson
import hu.unideb.studyversity.core.AppPreferences
import hu.unideb.studyversity.data.*
import hu.unideb.studyversity.data.model.*
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * [Backend] on top of the REST contract in [ApiService]. NOT exercised against a real server yet (there is none);
 * it is the wiring that makes switching `BackendConfig.USE_REMOTE` a configuration change once the server exists.
 *
 * Authentication: `user/login` returns a token that is sent as a Bearer header. The original app used an OpenID Connect
 * code flow (AppAuth) – replace [login] by it if the new server keeps the identity provider.
 */
class RemoteBackend(private val prefs: AppPreferences, gson: Gson) : Backend {

    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BackendConfig.API_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain ->
                    val token = prefs.getString(KEY_TOKEN)
                    val request = if (token.isNullOrEmpty()) chain.request()
                    else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                    chain.proceed(request)
                }).build()
        )
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    private val dateParam = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Maps HTTP 401/403 to [AuthRequiredException]; server-side failures arrive as [ApiException]. */
    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        if (e.code() == 401 || e.code() == 403) throw AuthRequiredException() else throw e
    }

    // ---- session -----------------------------------------------------------------------------------------------
    override suspend fun login(userName: String, password: String): User = call {
        val token = try { api.login(userName, password).checkedFirst() } catch (e: ApiException) { throw InvalidCredentialsException() }
        prefs.putString(KEY_TOKEN, token)
        currentUser() ?: throw AuthRequiredException()
    }

    override suspend fun logout() { prefs.remove(KEY_TOKEN) }

    override suspend fun currentUser(): User? {
        if (prefs.getString(KEY_TOKEN).isNullOrEmpty()) return null
        return try {
            val user = call { api.me().checkedFirst() }
            val balance = runCatching { api.studentFinancialAccount().checkedFirst() }.getOrNull()
            user.copy(balance = balance?.balance ?: user.balance, currency = balance?.currency ?: user.currency)
        } catch (e: AuthRequiredException) {
            prefs.remove(KEY_TOKEN); null
        }
    }

    // ---- student data ------------------------------------------------------------------------------------------
    override suspend fun trainings() = call { api.listStudentTraining().checkedData() }
    override suspend fun timetable() = call { api.listStudentTimetable().checkedData() }
    override suspend fun courses() = call { api.listStudentCourse().checkedData() }
    override suspend fun exams() = call { api.listStudentExam().checkedData() }
    override suspend fun modules() = call { api.listStudentModule().checkedData() }

    // ---- university content ------------------------------------------------------------------------------------
    override suspend fun academicYears() = call {
        api.documentsByTag("academic_year").checkedData().map { AcademicYearItem(it.id, it.name, it.url) }
    }

    override suspend fun scholarships() = call {
        api.documentsByTag("scholarship").checkedData().map { StudentScholarShipItem(it.id, it.name, it.url) }
    }

    override suspend fun rss(channel: NewsChannel) = call { api.rss(listOf(channel.value)).checkedData() }

    override suspend fun searchEmployees(name: String, startRow: Int, endRow: Int) =
        call { api.searchEmployee(startRow, endRow, name).checkedData() }

    override suspend fun employeeDetail(id: Long) = call { api.employeeDetail(id).checkedData().firstOrNull() }

    override suspend fun mapCategories(parentCodes: List<String>?) = call {
        api.categoryListWithParents(parentCodes ?: listOf("institution_category")).checkedData()
            .map { MapCategoryItem(it.id, it.code, it.name) }
    }

    override suspend fun institutions(categoryCodes: List<String>?, name: String?) = call {
        val conditions = ArrayList<Criterion>()
        if (!categoryCodes.isNullOrEmpty()) conditions.add(Criterion("category", "inSet", categoryCodes))
        if (!name.isNullOrBlank()) conditions.add(Criterion("name", "iContains", name))
        api.searchInstitutions(AbydosFetchRequest(0, 500, AdvancedCriteria("and", conditions))).checkedData()
    }

    override suspend fun institution(poiId: Long) = call { api.institution(poiId).checkedData().firstOrNull() }

    override suspend fun loanedBooks() = call { api.loanedBooks().checkedData() }

    override suspend fun searchLibrary(term: String, startRow: Int, endRow: Int) =
        call { api.librarySearch(startRow, endRow, term).checkedData() }

    // ---- messages & device -------------------------------------------------------------------------------------
    override suspend fun messages() = call { api.myMessageList(AbydosFetchRequest(0, 100, AdvancedCriteria())).checkedData() }

    override suspend fun message(id: Long) = call { api.message(id, prefs.deviceToken.ifEmpty { null }).checkedData().firstOrNull() }

    override suspend fun registerDevice(token: String) {
        call {
            api.registerDevice(DeviceRegistrationRequest(
                application = "hu.unideb.studyversity", communicationId = token, description = android.os.Build.MODEL,
                deviceId = token, language = Locale.getDefault().language
            )).checkedData()
        }
    }

    override suspend fun devicePreferences(): DevicePreference? = call {
        val criteria = AdvancedCriteria("and", listOf(Criterion("deviceId", "equals", prefs.deviceToken)))
        api.searchDevicePreferences(AbydosFetchRequest(0, 1, criteria)).checkedData().firstOrNull()?.let {
            DevicePreference(it.id, it.deviceId, it.promotion, it.userId)
        }
    }

    override suspend fun saveDevicePreferences(p: DevicePreference): DevicePreference = call {
        val dto = DevicePreferenceDto(p.id, p.deviceId, p.userId, true, true, p.promotion)
        val saved = (if (p.id == null) api.addDevicePreferences(dto) else api.updateDevicePreferences(dto)).checkedFirst()
        DevicePreference(saved.id, saved.deviceId, saved.promotion, saved.userId)
    }

    // ---- mood game ---------------------------------------------------------------------------------------------
    override suspend fun randomQuote(): QuoteItem? = call {
        if (BackendConfig.QUOTES_URL.isEmpty()) null else api.quotes(BackendConfig.QUOTES_URL).randomOrNull()
    }

    override suspend fun saveMood(mood: MoodItem) {
        call {
            val answer = api.addMood(mood)
            if (!answer.isSuccess) {
                if (answer.errorCode == ERROR_MOOD_ALREADY_ADDED) throw HappyChallengeTodayAnsweredException(answer.errorMessage.orEmpty())
                throw ApiException(answer.errorCode, answer.errorMessage)
            }
        }
    }

    override suspend fun moods() = call { api.moodList(null, 0, 400, "-moodDate").checkedData() }

    override suspend fun moodSummary(from: Date, to: Date) =
        call { api.moodSummary(dateParam.format(from), dateParam.format(to)).checkedData() }

    override suspend fun moodPoints() = call { api.moodPoints().checkedData().firstOrNull() }

    override suspend fun myCurrentGame(): StudentMoodGame = call {
        api.myCurrentGame().checkedData().firstOrNull() ?: throw NoMoodGameException()
    }

    override suspend fun moodGameStages() = call { api.moodGameStages().checkedData() }

    override suspend fun chooseGift(stageId: Int, giftId: Int) { call { api.chooseGift(stageId, giftId).checkedData() } }

    override suspend fun registerMoodGame() { call { api.registerMoodGame().checkedData() } }

    // ---- community ---------------------------------------------------------------------------------------------
    override suspend fun myCommunityProfile() = call { api.myCommunityProfile().checkedData().firstOrNull() }
    override suspend fun registerCommunityMember(m: CommunityMember) = call { api.addCommunityMember(m).checkedFirst() }
    override suspend fun updateCommunityMember(m: CommunityMember) = call { api.updateCommunityMember(m).checkedFirst() }
    override suspend fun deleteCommunityProfile(id: Long) { call { api.removeCommunityMember(id).checkedData() } }
    override suspend fun communityMember(id: Long) = call { api.communityMember(id).checkedData().firstOrNull() }

    override suspend fun searchMyInterests() = call { api.searchMyInterests(MemberFetchRequest(0, 500, MemberCriteria())).checkedData() }
    override suspend fun searchSuggestions() = call { api.searchMySuggestions(MemberFetchRequest(0, 500, MemberCriteria())).checkedData() }

    override suspend fun addToMyInterests(id: Long) = call { api.addToMyInterests(id).checkedData().firstOrNull() }
    override suspend fun removeFromMyInterests(id: Long) { call { api.removeFromMyInterests(id).checkedData() } }

    override suspend fun categories(parentCodes: List<String>) = call { api.categoryListWithParents(parentCodes).checkedData() }

    override suspend fun uploadProfileImage(bytes: ByteArray): String = call {
        val body = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file", "profile.jpg", okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), bytes))
            .build()
        val doc = api.uploadFile(body).checkedFirst()
        doc.url ?: (BackendConfig.DOCUMENT_BASE_URL + "id/" + doc.id)
    }

    // ---- instructor console ------------------------------------------------------------------------------------
    override suspend fun managedStudents() = call { api.staffStudents().checkedData() }
    override suspend fun updateStudent(update: StudentUpdate) = call { api.staffUpdateStudent(update).checkedFirst() }
    override suspend fun studentExams(studentId: Long) = call { api.staffExams(studentId).checkedData() }
    override suspend fun saveStudentExam(studentId: Long, exam: StaffExam) = call { api.staffSaveExam(studentId, exam).checkedFirst() }
    override suspend fun deleteStudentExam(studentId: Long, examId: Long) { call { api.staffDeleteExam(studentId, examId).checkedData() } }
    override suspend fun studentCourseOptions(studentId: Long) = call { api.staffCourses(studentId).checkedData() }
    override suspend fun studentClasses(studentId: Long) = call { api.staffTimetable(studentId).checkedData() }
    override suspend fun addStudentClass(studentId: Long, item: TimeTableItem) = call { api.staffAddClass(studentId, item).checkedFirst() }
    override suspend fun deleteStudentClass(studentId: Long, classId: Long) { call { api.staffDeleteClass(studentId, classId).checkedData() } }
    override suspend fun sendEmail(studentId: Long, subject: String, body: String) =
        call { api.staffSendEmail(studentId, MailItem(studentId = studentId, subject = subject, body = body)).checkedFirst() }
    override suspend fun sentEmails(studentId: Long) = call { api.staffEmails(studentId).checkedData() }

    private companion object {
        const val KEY_TOKEN = "auth_token"
        /** Error code the server answers when a mood was already added for today. */
        const val ERROR_MOOD_ALREADY_ADDED = "MOOD_ALREADY_ADDED"
    }
}
