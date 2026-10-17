package hu.unideb.studyversity.data.remote

import hu.unideb.studyversity.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * Retrofit description of the REST contract the app expects from the Studyversity server. It mirrors the 13 service
 * interfaces of the original app, so an existing deployment can be used as-is, and doubles as the specification for a new
 * server (see studyversity/README.md). All paths are relative to [BackendConfig.API_BASE_URL]; authenticated calls carry
 * `Authorization: Bearer <token>`.
 *
 * Every answer is wrapped in [RestResponse]; list endpoints return the rows in `data`.
 */
interface ApiService {
    // ---- user ---------------------------------------------------------------------------------------------------
    @FormUrlEncoded
    @POST("user/login")
    suspend fun login(@Field("userName") userName: String, @Field("password") password: String): RestResponse<String>

    @GET("user/me")
    suspend fun me(): RestResponse<User>

    // ---- Neptun -------------------------------------------------------------------------------------------------
    @GET("neptun/studentFinancialAccount")
    suspend fun studentFinancialAccount(): RestResponse<StudentFinancialAccountDto>

    @GET("neptun/listStudentTraining")
    suspend fun listStudentTraining(): RestResponse<StudentTrainingItem>

    @GET("neptun/listStudentTimetable")
    suspend fun listStudentTimetable(): RestResponse<TimeTableItem>

    @GET("neptun/listStudentCourse")
    suspend fun listStudentCourse(): RestResponse<StudentCourseItem>

    @GET("neptun/listStudentExam")
    suspend fun listStudentExam(): RestResponse<StudentExamItem>

    @GET("neptun/listStudentModule")
    suspend fun listStudentModule(): RestResponse<StudentModuleItem>

    // ---- documents (academic calendar, scholarships) -------------------------------------------------------------
    /** Documents carrying a tag, e.g. `academic_year` / `scholarship`; rows are [AcademicYearItem] / [StudentScholarShipItem] shaped. */
    @GET("srest/documentListWithTag")
    suspend fun documentsByTag(@Query("tag") tag: String): RestResponse<DocumentVO>

    @GET("document/get/{documentId}")
    suspend fun document(@Path("documentId") id: Long): RestResponse<DocumentVO>

    @POST("upload/file")
    suspend fun uploadFile(@Body file: MultipartBody): RestResponse<DocumentVO>

    // ---- RSS ------------------------------------------------------------------------------------------------------
    /** POSTs the channel ids (e.g. `[119]`); the original posted to a separate RSS host, here it is a path below the API root. */
    @Headers("Content-Type: application/json")
    @POST("rss/list")
    suspend fun rss(@Body channelIds: List<Int>): RestResponse<RssFeedItem>

    // ---- phone book -----------------------------------------------------------------------------------------------
    @GET("phonebook/searchEmployee")
    suspend fun searchEmployee(@Query("startRow") startRow: Int, @Query("endRow") endRow: Int, @Query("name") name: String): RestResponse<PhoneBookSearchItem>

    @GET("phonebook/employeeDetail")
    suspend fun employeeDetail(@Query("id") id: Long): RestResponse<EmployeeDetailItem>

    // ---- map / institutions ---------------------------------------------------------------------------------------
    @GET("category/getCategoryListWithParents")
    suspend fun categoryListWithParents(@Query("parentCodes") parentCodes: List<String>): RestResponse<CategoryItem>

    @POST("map/institution/search")
    suspend fun searchInstitutions(@Body request: AbydosFetchRequest): RestResponse<CachedInstitution>

    @GET("map/institution/get/{id}")
    suspend fun institution(@Path("id") poiId: Long): RestResponse<Institution>

    // ---- library (Corvina) ------------------------------------------------------------------------------------------
    @GET("corvina/loanedBooks")
    suspend fun loanedBooks(): RestResponse<LibraryLoanedContainerItem>

    @GET("corvina/search")
    suspend fun librarySearch(@Query("startRow") startRow: Int, @Query("endRow") endRow: Int, @Query("term") term: String): RestResponse<LibrarySearchItem>

    // ---- messages & preferences -------------------------------------------------------------------------------------
    @POST("studyversityMessage/myMessageList/")
    suspend fun myMessageList(@Body request: AbydosFetchRequest, @Query("sort") sort: String? = "-messageDate"): RestResponse<MessageListItem>

    @GET("studyversityMessage/get/{id}")
    suspend fun message(@Path("id") id: Long, @Query("deviceId") deviceId: String?): RestResponse<MessageDetail>

    @POST("studyversityMessage/registerDevice")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): RestResponse<String>

    @POST("studyversityPreferences/search")
    suspend fun searchDevicePreferences(@Body request: AbydosFetchRequest): RestResponse<DevicePreferenceDto>

    @POST("studyversityPreferences/add")
    suspend fun addDevicePreferences(@Body body: DevicePreferenceDto): RestResponse<DevicePreferenceDto>

    @POST("studyversityPreferences/update")
    suspend fun updateDevicePreferences(@Body body: DevicePreferenceDto): RestResponse<DevicePreferenceDto>

    // ---- mood game --------------------------------------------------------------------------------------------------
    @POST("mood/add")
    suspend fun addMood(@Body mood: MoodItem, @Query("newMode") newMode: Boolean = true): RestResponse<Any>

    @GET("mood/list")
    suspend fun moodList(
        @Query("criteria") criteria: String?, @Query("startRow") startRow: Int, @Query("endRow") endRow: Int,
        @Query("orderBy") orderBy: String?
    ): RestResponse<MoodItem>

    @GET("mood/getSummaryForInterval")
    suspend fun moodSummary(@Query("fromDate") from: String, @Query("toDate") to: String): RestResponse<MoodCountItem>

    @GET("mood/getPoints")
    suspend fun moodPoints(): RestResponse<PrizeItem>

    @GET("studentMoodGame/getMyCurrentGame")
    suspend fun myCurrentGame(): RestResponse<StudentMoodGame>

    @GET("moodGameStage/list")
    suspend fun moodGameStages(): RestResponse<MoodGameStage>

    /** Extension of the original contract (the original chose gifts on a web page). */
    @POST("studentMoodGame/chooseGift")
    suspend fun chooseGift(@Query("stageId") stageId: Int, @Query("giftId") giftId: Int): RestResponse<Any>

    /** Extension: joins the mood game (the original used a web registration page). */
    @POST("studentMoodGame/register")
    suspend fun registerMoodGame(): RestResponse<Any>

    /** Motivational quotes: a plain JSON array at [BackendConfig.QUOTES_URL] (not wrapped in a RestResponse). */
    @GET
    suspend fun quotes(@Url url: String): List<QuoteItem>

    // ---- community ----------------------------------------------------------------------------------------------------
    @GET("studyversityCommunity/getMyProfile")
    suspend fun myCommunityProfile(): RestResponse<CommunityMember>

    @POST("studyversityCommunity/add")
    suspend fun addCommunityMember(@Body m: CommunityMember): RestResponse<CommunityMember>

    @POST("studyversityCommunity/update/")
    suspend fun updateCommunityMember(@Body m: CommunityMember): RestResponse<CommunityMember>

    @POST("studyversityCommunity/remove/{id}")
    suspend fun removeCommunityMember(@Path("id") id: Long): RestResponse<CommunityMember>

    @GET("studyversityCommunity/get/{id}")
    suspend fun communityMember(@Path("id") id: Long): RestResponse<CommunityMember>

    @POST("studyversityCommunity/searchMyInterest")
    suspend fun searchMyInterests(@Body request: MemberFetchRequest): RestResponse<CommunityMember>

    @POST("studyversityCommunity/searchMySuggestions")
    suspend fun searchMySuggestions(@Body request: MemberFetchRequest): RestResponse<CommunityMember>

    @POST("studyversityCommunity/addToMyInterest/{id}")
    suspend fun addToMyInterests(@Path("id") id: Long): RestResponse<CommunityMember>

    @POST("studyversityCommunity/removeFromMyInterests/{id}")
    suspend fun removeFromMyInterests(@Path("id") id: Long): RestResponse<Any>

    // ---- instructor console (extension of the original contract; role INSTRUCTOR) ------------------------------------
    @GET("staff/student/list")
    suspend fun staffStudents(): RestResponse<StaffStudent>

    @POST("staff/student/update")
    suspend fun staffUpdateStudent(@Body update: StudentUpdate): RestResponse<StaffStudent>

    @GET("staff/student/{id}/exams")
    suspend fun staffExams(@Path("id") studentId: Long): RestResponse<StaffExam>

    @POST("staff/student/{id}/exam/save")
    suspend fun staffSaveExam(@Path("id") studentId: Long, @Body exam: StaffExam): RestResponse<StaffExam>

    @POST("staff/student/{id}/exam/delete/{examId}")
    suspend fun staffDeleteExam(@Path("id") studentId: Long, @Path("examId") examId: Long): RestResponse<Any>

    @GET("staff/student/{id}/courses")
    suspend fun staffCourses(@Path("id") studentId: Long): RestResponse<CourseOption>

    @GET("staff/student/{id}/timetable")
    suspend fun staffTimetable(@Path("id") studentId: Long): RestResponse<TimeTableItem>

    @POST("staff/student/{id}/timetable/add")
    suspend fun staffAddClass(@Path("id") studentId: Long, @Body item: TimeTableItem): RestResponse<TimeTableItem>

    @POST("staff/student/{id}/timetable/delete/{classId}")
    suspend fun staffDeleteClass(@Path("id") studentId: Long, @Path("classId") classId: Long): RestResponse<Any>

    @POST("staff/student/{id}/email")
    suspend fun staffSendEmail(@Path("id") studentId: Long, @Body mail: MailItem): RestResponse<MailItem>

    @GET("staff/student/{id}/emails")
    suspend fun staffEmails(@Path("id") studentId: Long): RestResponse<MailItem>
}

/** `neptun/studentFinancialAccount` row. */
data class StudentFinancialAccountDto(val balance: Int? = null, val currency: String? = null)
