package hu.unideb.studyversity.data.local

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import hu.unideb.studyversity.data.*
import hu.unideb.studyversity.data.model.*
import java.io.File
import java.text.Normalizer
import java.util.Calendar
import java.util.Date
import java.util.Random

/**
 * Backend stand-in that runs entirely on the device: content comes from [SeedData], everything the student changes
 * (login, moods, mood game, community profile, read messages ...) is persisted as JSON in SharedPreferences, so the
 * app behaves like it talks to a server that remembers things.
 *
 * Rules that the original server enforced and that are reproduced here: login required for personal data, one mood
 * per day, mood game levels of 73 reported days, community suggestions by shared categories.
 */
class LocalBackend(private val context: Context, private val gson: Gson) : Backend {

    // ---- persisted state ---------------------------------------------------------------------------------------
    private class GameState(
        var start: Date, var stage: Int, var stageStart: Date, var reported: MutableList<Date>,
        var completed: MutableList<CompletedMoodGameStage>, var lastReported: Date?
    )

    private class State(
        /** username of the signed-in account, null when signed out */
        var username: String? = null,
        var student: StudentRecord = StudentRecord(),
        var deviceToken: String? = null,
        var promotion: Boolean = true,
        var readMessages: MutableSet<Long> = mutableSetOf(),
        var moods: MutableList<MoodItem> = mutableListOf(),
        var game: GameState? = null,
        var profile: CommunityMember? = null,
        var interests: MutableSet<Long> = mutableSetOf()
    )

    private val sp = context.getSharedPreferences("studyversity_local_backend", Context.MODE_PRIVATE)
    private val lock = Any()
    private var state: State = load()

    private fun load(): State {
        val json = sp.getString(KEY_STATE, null)
        val loaded = json?.let { runCatching { gson.fromJson(it, State::class.java) }.getOrNull() }
        return loaded?.takeIf { it.student.neptun.isNotEmpty() } ?: initialState()
    }

    private fun save() { sp.edit().putString(KEY_STATE, gson.toJson(state)).apply() }

    private fun <T> locked(block: () -> T): T = synchronized(lock) { block() }

    private fun seed() = SeedData(context.resources.configuration.locales[0].language == "hu")

    /** The student already took part in the mood game for three weeks: levels, history and rewards have content. */
    private fun initialState(): State {
        val seed = seed()
        val today = startOfDay(Date())
        val rnd = Random(7)
        val moods = ArrayList<MoodItem>()
        var id = 1L
        for (back in 40 downTo 1) {
            if (back == 21) continue // a missed day
            val v = rnd.nextInt(100)
            moods.add(MoodItem(id = id++, moodDate = addDays(today, -back, 18), moodValue = if (v < 50) "GREAT" else if (v < 85) "NORMAL" else "BAD"))
        }
        val reported = (20 downTo 1).map { addDays(today, -it, 18) }.toMutableList()
        val stage1 = CompletedMoodGameStage(
            id = 1, stageOrder = 1, targetDays = SeedData.STAGE_DAYS, days = SeedData.STAGE_DAYS, daysOfMissed = 22, stageCompleted = 1,
            receiptDate = addDays(today, -22, 12), chosenGift = null, gifts = seed.gameStages()[0].gifts
        )
        val game = GameState(
            start = addDays(today, -95, 12), stage = 2, stageStart = addDays(today, -22, 12), reported = reported,
            completed = mutableListOf(stage1), lastReported = reported.lastOrNull()
        )
        return State(student = seed.newStudentRecord(), moods = moods, game = game)
    }

    // ---- small helpers -----------------------------------------------------------------------------------------
    private fun startOfDay(d: Date): Date = Calendar.getInstance().apply {
        time = d; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    private fun addDays(d: Date, days: Int, hour: Int): Date = Calendar.getInstance().apply {
        time = d; add(Calendar.DAY_OF_YEAR, days); set(Calendar.HOUR_OF_DAY, hour)
    }.time

    private fun sameDay(a: Date, b: Date) = startOfDay(a) == startOfDay(b)

    private fun plain(s: String?) = Normalizer.normalize(s.orEmpty(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

    /** The signed-in account; [AuthRequiredException] when nobody is signed in. */
    private fun account(): Accounts.Account =
        state.username?.let { u -> listOf(Accounts.student, Accounts.instructor).firstOrNull { it.username == u } } ?: throw AuthRequiredException()

    /** Signed in (any role). */
    private fun neptun(): String = account().username

    private fun requireStudent(): StudentRecord {
        if (account().role != ROLE_STUDENT) throw AccessDeniedException()
        return state.student
    }

    private fun requireInstructor() { if (account().role != ROLE_INSTRUCTOR) throw AccessDeniedException() }

    private fun student(id: Long): StudentRecord = state.student.takeIf { it.id == id } ?: throw IllegalArgumentException("Unknown student")

    // ---- session -----------------------------------------------------------------------------------------------
    override suspend fun login(userName: String, password: String): User = locked {
        val account = Accounts.find(userName, password) ?: throw InvalidCredentialsException()
        state.username = account.username
        save()
        userOf(account)
    }

    private fun userOf(a: Accounts.Account): User = if (a.role == ROLE_INSTRUCTOR) seed().instructorUser() else seed().studentUser(state.student)

    override suspend fun logout() = locked { state.username = null; save() }

    override suspend fun currentUser(): User? = locked {
        state.username?.let { u -> listOf(Accounts.student, Accounts.instructor).firstOrNull { it.username == u }?.let(::userOf) }
    }

    // ---- student data ------------------------------------------------------------------------------------------
    override suspend fun trainings() = locked { seed().trainings(requireStudent()) }
    override suspend fun timetable() = locked { seed().timetable(requireStudent()) }
    override suspend fun courses() = locked { requireStudent(); seed().courses() }
    override suspend fun exams() = locked { seed().exams(requireStudent()) }
    override suspend fun modules() = locked { seed().modules(requireStudent()) }

    // ---- university content ------------------------------------------------------------------------------------
    override suspend fun academicYears() = seed().academicYears()
    override suspend fun scholarships() = seed().scholarships()
    override suspend fun rss(channel: NewsChannel) = seed().rss(channel)

    override suspend fun searchEmployees(name: String, startRow: Int, endRow: Int): List<PhoneBookSearchItem> {
        val q = plain(name)
        return seed().phoneBook().filter { q.isEmpty() || plain(it.name).contains(q) || plain(it.formattedDepartmentName).contains(q) }
            .sortedBy { plain(it.name) }.drop(startRow).take((endRow - startRow).coerceAtLeast(0))
    }

    override suspend fun employeeDetail(id: Long) = seed().employee(id)

    override suspend fun mapCategories(parentCodes: List<String>?) = seed().mapCategories()

    override suspend fun institutions(categoryCodes: List<String>?, name: String?): List<CachedInstitution> {
        val q = plain(name)
        return seed().institutions().filter {
            (categoryCodes.isNullOrEmpty() || it.category in categoryCodes) && (q.isEmpty() || plain(it.name).contains(q))
        }.sortedBy { plain(it.name) }
    }

    override suspend fun institution(poiId: Long) = seed().institution(poiId)

    override suspend fun loanedBooks() = locked { requireStudent(); seed().loans() }

    override suspend fun searchLibrary(term: String, startRow: Int, endRow: Int): List<LibrarySearchItem> {
        val q = plain(term)
        return seed().librarySearchAll().filter { q.isEmpty() || plain(it.title).contains(q) || plain(it.author).contains(q) }
            .drop(startRow).take((endRow - startRow).coerceAtLeast(0))
    }

    // ---- messages & device -------------------------------------------------------------------------------------
    /** Seeded notices plus the e-mails instructors sent to the student, newest first. */
    private fun allMessages(): List<MessageDetail> {
        val seed = seed()
        return (seed.messages() + state.student.mails.map { seed.mailAsMessage(it, false) }).sortedByDescending { it.messageDate }
    }

    override suspend fun messages(): List<MessageListItem> = locked {
        requireStudent()
        allMessages().map {
            MessageListItem(
                id = it.id, userMessageId = it.id, messageToUserId = 1, channelType = "PUSH", deviceId = state.deviceToken,
                messageBody = android.text.Html.fromHtml(it.messageBody.orEmpty(), android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                messageDate = it.messageDate, messageHeader = it.messageHeader,
                readed = (it.id in state.readMessages || it.readed == true).toString(), senderType = it.senderType, statusName = it.status
            )
        }
    }

    override suspend fun message(id: Long): MessageDetail? = locked {
        requireStudent()
        val m = allMessages().firstOrNull { it.id == id } ?: return@locked null
        state.readMessages.add(id)
        save()
        m.copy(readed = true)
    }

    override suspend fun registerDevice(token: String) = locked { state.deviceToken = token; save() }

    override suspend fun devicePreferences(): DevicePreference? = locked {
        neptun()
        DevicePreference(id = 1, deviceId = state.deviceToken, promotion = state.promotion, userId = 1)
    }

    override suspend fun saveDevicePreferences(p: DevicePreference): DevicePreference = locked {
        neptun()
        state.promotion = p.promotion ?: state.promotion
        save()
        DevicePreference(id = 1, deviceId = state.deviceToken, promotion = state.promotion, userId = 1)
    }

    // ---- mood game ---------------------------------------------------------------------------------------------
    override suspend fun randomQuote(): QuoteItem? = locked { requireStudent(); seed().quotes().random() }

    override suspend fun saveMood(mood: MoodItem) = locked {
        requireStudent()
        val now = Date()
        if (state.moods.any { it.moodDate?.let { d -> sameDay(d, now) } == true }) throw HappyChallengeTodayAnsweredException(seed().moodAlreadyAnswered())
        state.moods.add(MoodItem(id = (state.moods.maxOfOrNull { it.id ?: 0 } ?: 0) + 1, moodDate = now, moodValue = mood.moodValue))
        state.game?.let { g ->
            if (g.stage < LAST_STAGE) {
                g.reported.add(now)
                g.lastReported = now
                if (g.reported.size >= SeedData.STAGE_DAYS) completeStage(g, now)
            }
        }
        save()
    }

    private fun completeStage(g: GameState, now: Date) {
        val stage = seed().gameStages()[g.stage - 1]
        val missed = (((now.time - g.stageStart.time) / DAY_MS).toInt() + 1 - g.reported.size).coerceAtLeast(0)
        g.completed.add(CompletedMoodGameStage(
            id = stage.id, stageOrder = stage.stageOrder, targetDays = stage.targetDays, days = stage.days, daysOfMissed = missed,
            stageCompleted = 1, receiptDate = now, chosenGift = null, gifts = stage.gifts
        ))
        g.stage += 1
        g.stageStart = now
        g.reported = mutableListOf()
    }

    override suspend fun moods(): List<MoodItem> = locked { requireStudent(); state.moods.sortedByDescending { it.moodDate } }

    override suspend fun moodSummary(from: Date, to: Date): List<MoodCountItem> = locked {
        requireStudent()
        val range = startOfDay(from)..Date(startOfDay(to).time + DAY_MS - 1)
        val counts = state.moods.filter { m -> m.moodDate?.let { it in range } == true }.groupingBy { it.moodValue }.eachCount()
        listOf("GREAT", "NORMAL", "BAD").mapIndexed { i, m -> MoodCountItem(id = i + 1L, mood = m, count = (counts[m] ?: 0).toString()) }
    }

    override suspend fun moodPoints(): PrizeItem? = locked {
        requireStudent()
        val g = state.game ?: return@locked null
        PrizeItem(id = 1, level = g.stage.toString(), point = g.completed.sumOf { it.days ?: 0 } + g.reported.size)
    }

    override suspend fun myCurrentGame(): StudentMoodGame = locked {
        requireStudent()
        val g = state.game ?: throw NoMoodGameException()
        val stages = seed().gameStages()
        StudentMoodGame(
            id = 1, active = g.stage < LAST_STAGE, gameStart = g.start, gameFinish = if (g.stage >= LAST_STAGE) g.stageStart else null,
            currentStageStart = g.stageStart, lastReported = g.lastReported, currentStage = stages[g.stage - 1],
            nextStage = stages.getOrNull(g.stage), completedStages = g.completed.toList(), failedStages = emptyList(),
            reportedDays = g.reported.toList()
        )
    }

    override suspend fun moodGameStages(): List<MoodGameStage> = locked { requireStudent(); seed().gameStages() }

    override suspend fun chooseGift(stageId: Int, giftId: Int) = locked {
        requireStudent()
        val g = state.game ?: throw NoMoodGameException()
        val i = g.completed.indexOfFirst { it.id == stageId }
        if (i < 0) return@locked
        val gift = g.completed[i].gifts.firstOrNull { it.id == giftId } ?: return@locked
        if (g.completed[i].chosenGift == null) g.completed[i] = g.completed[i].copy(chosenGift = gift)
        save()
    }

    override suspend fun registerMoodGame() = locked {
        requireStudent()
        if (state.game == null) {
            val now = Date()
            state.game = GameState(start = now, stage = 1, stageStart = now, reported = mutableListOf(), completed = mutableListOf(), lastReported = null)
            save()
        }
    }

    // ---- community ---------------------------------------------------------------------------------------------
    override suspend fun myCommunityProfile(): CommunityMember? = locked { requireStudent(); state.profile }

    override suspend fun registerCommunityMember(m: CommunityMember): CommunityMember = locked {
        val user = seed().studentUser(requireStudent())
        val saved = m.copy(
            id = MY_PROFILE_ID, userId = user.id, firstName = user.firstName, lastName = user.lastName, modules = user.studentTrainingNameList
        )
        state.profile = saved
        save()
        saved
    }

    override suspend fun updateCommunityMember(m: CommunityMember): CommunityMember = locked {
        val current = state.profile ?: throw IllegalStateException("No community profile")
        val saved = m.copy(id = current.id, userId = current.userId, firstName = current.firstName, lastName = current.lastName, modules = current.modules)
        state.profile = saved
        save()
        saved
    }

    override suspend fun deleteCommunityProfile(id: Long) = locked {
        requireStudent()
        state.profile = null
        state.interests.clear()
        save()
    }

    /** Categories shared with the signed-in student are flagged as significant (shown highlighted). */
    private fun withSignificance(m: CommunityMember): CommunityMember {
        val mine = state.profile ?: return m
        fun mark(list: List<CategoryItem>, own: List<CategoryItem>) = list.map { c -> c.copy(isSignificant = own.any { it.id == c.id }) }
        return m.copy(
            interests = mark(m.interests, mine.interests), faculties = mark(m.faculties, mine.faculties),
            preferredLanguages = mark(m.preferredLanguages, mine.preferredLanguages), potentialFriends = emptyList()
        )
    }

    private fun shared(m: CommunityMember): Int {
        val mine = state.profile ?: return 0
        return m.interests.count { c -> mine.interests.any { it.id == c.id } } * 2 +
            m.faculties.count { c -> mine.faculties.any { it.id == c.id } } +
            m.preferredLanguages.count { c -> mine.preferredLanguages.any { it.id == c.id } }
    }

    override suspend fun communityMember(id: Long): CommunityMember? = locked {
        requireStudent()
        (seed().members().firstOrNull { it.id == id } ?: state.profile?.takeIf { it.id == id })?.let(::withSignificance)
    }

    override suspend fun searchMyInterests(): List<CommunityMember> = locked {
        requireStudent()
        seed().members().filter { it.id in state.interests }.map(::withSignificance)
    }

    override suspend fun searchSuggestions(): List<CommunityMember> = locked {
        requireStudent()
        seed().members().filter { it.id !in state.interests && shared(it) > 0 }
            .sortedWith(compareByDescending<CommunityMember> { shared(it) }.thenBy { plain(it.fullName) })
            .map(::withSignificance)
    }

    override suspend fun addToMyInterests(id: Long): CommunityMember? = locked {
        requireStudent()
        val m = seed().members().firstOrNull { it.id == id } ?: return@locked null
        state.interests.add(id)
        save()
        withSignificance(m)
    }

    override suspend fun removeFromMyInterests(id: Long) = locked { requireStudent(); state.interests.remove(id); save(); Unit }

    override suspend fun categories(parentCodes: List<String>): List<CategoryItem> =
        seed().categories().filter { parentCodes.isEmpty() || it.parentCategoryCode in parentCodes }

    override suspend fun uploadProfileImage(bytes: ByteArray): String = locked {
        requireStudent()
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        // keep only the newest upload
        context.filesDir.listFiles { f -> f.name.startsWith("profile_") && f != file }?.forEach { it.delete() }
        Uri.fromFile(file).toString()
    }

    // ---- instructor console ------------------------------------------------------------------------------------
    override suspend fun managedStudents(): List<StaffStudent> = locked {
        requireInstructor()
        listOf(seed().staffStudent(state.student))
    }

    override suspend fun updateStudent(update: StudentUpdate): StaffStudent = locked {
        requireInstructor()
        val r = student(update.id)
        require(update.neptunCode.isNotBlank()) { "The university ID cannot be empty" }
        require(update.lastName.isNotBlank() || update.firstName.isNotBlank()) { "The name cannot be empty" }
        r.firstName = update.firstName.trim()
        r.lastName = update.lastName.trim()
        r.neptun = update.neptunCode.trim().uppercase()
        r.email = update.email.trim()
        r.balance = update.balance
        r.trainingPlanEnd = update.trainingPlanEnd
        save()
        seed().staffStudent(r)
    }

    override suspend fun studentExams(studentId: Long): List<StaffExam> = locked {
        requireInstructor()
        student(studentId).exams.sortedWith(compareByDescending<StaffExam> { it.semester }.thenByDescending { it.examDate?.time ?: 0L }.thenByDescending { it.id })
    }

    override suspend fun saveStudentExam(studentId: Long, exam: StaffExam): StaffExam = locked {
        requireInstructor()
        val r = student(studentId)
        val old = exam.id?.let { id -> r.exams.firstOrNull { it.id == id } }
        if (exam.id != null && old == null) throw IllegalArgumentException("Unknown exam")
        require(!exam.courseCode.isNullOrBlank() && !exam.subjectName.isNullOrBlank()) { "Choose a course" }
        val grade = exam.grade?.takeIf { it in 1..5 }
        val saved = exam.copy(
            id = exam.id ?: ((r.exams.maxOfOrNull { it.id ?: 0 } ?: 0) + 1),
            grade = grade,
            recordDate = when {
                grade == null -> null
                old?.grade == grade -> old.recordDate ?: Date()
                else -> Date()
            }
        )
        if (old == null) r.exams.add(saved) else r.exams[r.exams.indexOf(old)] = saved
        save()
        saved
    }

    override suspend fun deleteStudentExam(studentId: Long, examId: Long) = locked {
        requireInstructor()
        student(studentId).exams.removeAll { it.id == examId }
        save()
        Unit
    }

    override suspend fun studentCourseOptions(studentId: Long): List<CourseOption> = locked {
        requireInstructor()
        student(studentId)
        DentistryProgram.courses().filter { it.courseType == "L" }
            .map { CourseOption(it.courseCode.orEmpty(), it.subjectName.orEmpty(), it.semester.orEmpty(), it.credit?.toIntOrNull() ?: 0) }
            .sortedWith(compareByDescending<CourseOption> { it.semester }.thenBy { it.subjectName })
    }

    override suspend fun studentClasses(studentId: Long): List<TimeTableItem> = locked {
        requireInstructor()
        seed().timetable(student(studentId)).sortedBy { it.classStart }
    }

    override suspend fun addStudentClass(studentId: Long, item: TimeTableItem): TimeTableItem = locked {
        requireInstructor()
        val r = student(studentId)
        require(!item.subjectName.isNullOrBlank()) { "The subject name cannot be empty" }
        val start = item.classStart
        val end = item.classEnd
        require(start != null && end != null && start < end) { "The class must end after it starts" }
        val saved = item.copy(
            id = (r.classes.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
            instructorName = item.instructorName ?: seed().instructorUser().name,
            semester = item.semester ?: DentistryProgram.CURRENT_SEMESTER,
            latitude = item.latitude ?: DentistryProgram.LAT, longitude = item.longitude ?: DentistryProgram.LON
        )
        r.classes.add(saved)
        save()
        saved
    }

    override suspend fun deleteStudentClass(studentId: Long, classId: Long) = locked {
        requireInstructor()
        student(studentId).classes.removeAll { it.id == classId }
        save()
        Unit
    }

    override suspend fun sendEmail(studentId: Long, subject: String, body: String): MailItem = locked {
        requireInstructor()
        val r = student(studentId)
        require(subject.isNotBlank()) { "The subject cannot be empty" }
        require(body.isNotBlank()) { "The message cannot be empty" }
        val mail = MailItem(
            id = maxOf(MAIL_ID_BASE, r.mails.maxOfOrNull { it.id ?: 0 } ?: 0) + 1, studentId = r.id,
            from = seed().instructorUser().name, to = r.fullName, subject = subject.trim(), body = body.trim(), sentAt = Date()
        )
        r.mails.add(mail)
        save()
        mail
    }

    override suspend fun sentEmails(studentId: Long): List<MailItem> = locked {
        requireInstructor()
        student(studentId).mails.sortedByDescending { it.sentAt }
    }

    private companion object {
        const val KEY_STATE = "state_v2"
        const val LAST_STAGE = 6
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val MY_PROFILE_ID = 1L
        /** E-mails to the student are numbered above the seeded notices so message ids never clash. */
        const val MAIL_ID_BASE = 1000L
    }
}
