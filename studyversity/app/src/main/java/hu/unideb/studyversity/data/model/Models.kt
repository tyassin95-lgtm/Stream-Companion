package hu.unideb.studyversity.data.model

import android.content.Context
import android.text.format.DateFormat
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import java.util.Date

const val ROLE_STUDENT = "STUDENT"
const val ROLE_INSTRUCTOR = "INSTRUCTOR"

/** Models mirror the JSON value objects of the original Studyversity REST API (field names are the wire names). */

data class User(
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
    val profile: String? = null,
    val neptunCode: String? = null,
    val balance: Int? = null,
    val studentTrainingNameList: String? = null,
    val facultyNameList: String? = null,
    val lastName: String? = null,
    val firstName: String? = null,
    val currency: String = "HUF",
    /** [ROLE_STUDENT] or [ROLE_INSTRUCTOR] */
    val role: String = ROLE_STUDENT
) {
    val isInstructor get() = role == ROLE_INSTRUCTOR

    fun formattedBalance(context: Context): String? = balance?.let {
        context.getString(R.string.account_balance) + " " + java.text.NumberFormat.getInstance().format(it) + " " + currency
    }
}

data class StudentTrainingItem(
    val id: Long? = null,
    val activeSemesters: String? = null,
    val facultyName: String? = null,
    val facultyCode: String? = null,
    val financialStatus: String? = null,
    val moduleCode: String? = null,
    val moduleLevel: String? = null,
    val moduleName: String? = null,
    val moduleStatus: String? = null,
    val moduleValidFrom: Date? = null,
    val moduleValidTo: Date? = null,
    val neptunCode: String? = null,
    val passiveSemesters: String? = null,
    val trainingMode: String? = null,
    val trainingStart: Date? = null,
    val trainingPlanEnd: Date? = null
) {
    val isHeader get() = id == null
    fun trainingStart(c: Context) = Formats.date(trainingStart)
    fun trainingPlanEnd(c: Context) = Formats.date(trainingPlanEnd)
    fun moduleValidFrom(c: Context) = Formats.date(moduleValidFrom)
    fun moduleValidTo(c: Context) = Formats.date(moduleValidTo)
}

data class TimeTableItem(
    val id: Long? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val roomName: String? = null,
    val courseType: String? = null,
    val roomCode: String? = null,
    val classStart: Date? = null,
    val classEnd: Date? = null,
    val semester: String? = null,
    val instructorName: String? = null,
    val latitude: String? = null,
    val longitude: String? = null
) {
    val isHeader get() = id == -1L
    val dayFromClassStart: String
        get() = classStart?.let { DateFormat.format("EEEE", it).toString().replaceFirstChar { c -> c.uppercase() } } ?: ""
    val classStartAndEnd: String get() = Formats.hourMinute(classStart) + " - " + Formats.hourMinute(classEnd)
    val location: Pair<Double, Double>?
        get() = runCatching { Pair(latitude!!.toDouble(), longitude!!.toDouble()) }.getOrNull()
}

data class StudentCourseItem(
    val id: Long? = null,
    val subjectName: String? = null,
    val courseCode: String? = null,
    val courseType: String? = null,
    val credit: String? = null,
    val semester: String? = null,
    val moduleName: String? = null
)

data class StudentExamItem(
    val id: Long? = null,
    val subjectName: String? = null,
    val courseCode: String? = null,
    val courseType: String? = null,
    val examType: String? = null,
    val credit: String? = null,
    val moduleValidFrom: Date? = null,
    val moduleValidTo: Date? = null,
    val examDate: Date? = null,
    val examRecordDate: Date? = null,
    val examValue: String? = null,
    val examRecordType: String? = null,
    val semester: String? = null,
    val moduleName: String? = null,
    val subjectRequirement: String? = null
) {
    /** The original treats every value other than "1" (failed) as success. */
    val examSuccess: Boolean get() = examValue != null && !examValue.contains("1")
    fun formattedSubjectName(c: Context) = c.getString(R.string.formatted_subject_name, courseCode, subjectName, courseType)
}

data class StudentModuleItem(val id: Long = 0, val moduleCode: String? = null, val moduleName: String? = null, val userId: Long? = null)

data class AcademicYearItem(val id: Long = 0, val name: String? = null, val url: String? = null)
data class StudentScholarShipItem(val id: Long = 0, val name: String? = null, val url: String? = null)

data class MapCategoryItem(val id: Long = 0, val code: String? = null, val name: String? = null)

/** Generic id / code / name / value tuple the original API uses for dynamic values, business hours, slides ... */
data class IdCodeName(
    val id: String? = null, val code: String? = null, val name: String? = null,
    val value: String? = null, val icon: String? = null
)

data class CachedInstitution(
    val id: Long = 0,
    val poiId: Long = 0,
    val name: String? = null,
    val category: String? = null,
    val categoryName: String? = null,
    val address: String? = null,
    val detail: String? = null,
    val latitude: String? = null,
    val longitude: String? = null
) {
    val location: Pair<Double, Double>?
        get() = runCatching { Pair(latitude!!.toDouble(), longitude!!.toDouble()) }.getOrNull()
}

/** Institutions are described by "dynamic values" (code → value), exactly like the original REST payload. */
data class Slide(val slide: List<IdCodeName> = emptyList())

data class EconomicUnit(val title: String, val subItems: ArrayList<String>) {
    val formattedSubItems: String get() = subItems.joinToString("") { it + "\n" }
}

data class Institution(
    val id: Long = 0,
    val name: String? = null,
    val campus: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val categories: List<IdCodeName> = emptyList(),
    val businessHoursTitle: String? = null,
    val businessHours: List<IdCodeName> = emptyList(),
    val slideshowTitle: String? = null,
    val slideshow: List<Slide> = emptyList(),
    val imageIds: List<Long> = emptyList(),
    /** Absolute image urls; when empty they are derived from [imageIds] and the document store. */
    val imageUrls: List<String> = emptyList(),
    val preDynamicValues: List<IdCodeName> = emptyList(),
    val postDynamicValues: List<IdCodeName> = emptyList()
) {
    private fun pre(code: String) = preDynamicValues.firstOrNull { it.code == code }?.value
    private fun post(code: String) = postDynamicValues.firstOrNull { it.code == code }?.value?.replace(";", "; ")

    val address: String? get() = pre("institution_address")
    val directPhone: String? get() = post("direct_phone")
    val centralDispatcherPhone: String? get() = post("central_dispatcher_phone")
    val webPage: String? get() = post("webpage")

    val location: Pair<Double, Double>?
        get() = runCatching { Pair(latitude!!.toDouble(), longitude!!.toDouble()) }.getOrNull()

    /** Hours rows that actually carry data ("N/A" entries are dropped). */
    val managedBusinessHours: List<IdCodeName> get() = businessHours.filterNot { it.value.equals("N/A", true) }
    val businessHoursIsEmpty: Boolean get() = managedBusinessHours.isEmpty()

    /** Organisations in the building: slides are (title, item) pairs grouped by title. */
    val economicUnits: List<EconomicUnit>
        get() {
            val out = ArrayList<EconomicUnit>()
            slideshow.forEach { sl ->
                val title = sl.slide.getOrNull(0)?.value
                val item = sl.slide.getOrNull(1)?.value
                if (title != null && item != null) {
                    val existing = out.firstOrNull { it.title == title }
                    if (existing != null) existing.subItems.add(item) else out.add(EconomicUnit(title, arrayListOf(item)))
                }
            }
            return out
        }

    val isEmpty: Boolean
        get() = name.isNullOrEmpty() && campus.isNullOrEmpty() && latitude.isNullOrEmpty() && longitude.isNullOrEmpty() &&
            phone.isNullOrEmpty() && website.isNullOrEmpty() && imageIds.isEmpty() && imageUrls.isEmpty() &&
            slideshowTitle.isNullOrEmpty() && businessHoursTitle.isNullOrEmpty() && businessHours.isEmpty() &&
            slideshow.isEmpty() && categories.isEmpty() && preDynamicValues.isEmpty() && postDynamicValues.isEmpty()
}

data class PhoneBookSearchItem(
    val id: Long = 0,
    val name: String? = null,
    val formattedDepartmentName: String? = null,
    val contactWithDepartmentName: String? = null
) {
    /** "<b>First line</b> rest" – the department name's first segment is bold. */
    val formattedDepartmentNameWithHtml: String
        get() {
            val parts = formattedDepartmentName?.split("<br/>") ?: return "<b>"
            val sb = StringBuilder("<b>")
            parts.forEachIndexed { i, s -> sb.append(s); if (i == 0) sb.append("</b> ") }
            return sb.toString()
        }
}

data class EmployeeDetailItem(
    val id: Long = 0,
    val clientContactId: Long = 0,
    val name: String? = null,
    val departmentName: String? = null,
    val jobPosition: String? = null,
    val centralPhoneWithExtension: String? = null,
    val email: String? = null,
    val address: String? = null,
    val addressInBuilding: String? = null,
    val tudosterUrl: String? = null,
    val odtProfilUrl: String? = null,
    val website: String? = null,
    val ownWebUrl: String? = null
)

enum class NewsChannel(val value: Int) { unidebGeneralNews(119), unidebEvents(120), sportNews(121), sportEvents(122) }

data class RssFeedItem(
    val id: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val link: String? = null,
    val pubDate: Date? = null,
    val channelIDs: String? = null
) {
    val formattedDescription: String
        get() = android.text.Html.fromHtml(
            (description ?: "").replace(Regex("<img[^>]*>"), ""), android.text.Html.FROM_HTML_MODE_LEGACY
        ).toString().replace("\n\n", "\n")
}

// ---- library -------------------------------------------------------------------------------------------------
data class LanguageValue(val language: String? = null, val value: String? = null)
data class HoldingLocation(val locationID: String? = null, val name: List<LanguageValue> = emptyList()) {
    fun displayName(lang: String): String =
        name.firstOrNull { it.language.equals(lang, true) }?.value ?: name.firstOrNull()?.value ?: ""
}
data class LibraryBookHoldings(val uniqueID: String? = null, val issueDate: String? = null, val callNumber: String? = null, val location: HoldingLocation? = null)
data class LibraryLoanedItem(
    val uniqueID: String? = null, val author: String? = null, val title: String? = null, val publication: String? = null,
    val catalogURL: String? = null, val coverImageURL: String? = null, val library: List<HoldingLocation> = emptyList()
)
data class LibraryLoanedContainerItem(val bibRecord: LibraryLoanedItem? = null, val library: HoldingLocation? = null, val dueDate: Date? = null, var isExpanded: Boolean = false)
data class LibrarySearchItem(
    val uniqueID: String? = null, val author: String? = null, val title: String? = null, val publication: String? = null,
    val catalogURL: String? = null, val coverImageURL: String? = null, val availableItemCount: String? = null,
    val holdings: List<LibraryBookHoldings> = emptyList(), var isExpanded: Boolean = false
)

// ---- documents / messages -------------------------------------------------------------------------------------
data class DocumentVO(
    val id: Long = 0, val name: String? = null, val description: String? = null, val directoryPath: String? = null,
    val format: String? = null, val parentId: Long? = null, val size: Int? = null, val type: String? = null,
    val uploadDate: String? = null, val url: String? = null, val version: Int? = null
)

data class MessageListItem(
    val id: Long? = null, val userMessageId: Long? = null, val messageToUserId: Long? = null,
    val channelType: String? = null, val deviceId: String? = null, val messageBody: String? = null,
    val messageDate: Date? = null, val messageHeader: String? = null, var readed: String? = null,
    val senderType: String? = null, val statusName: String? = null
) {
    val isRead get() = readed == "true" || readed == "1" || readed == "Y"
}

data class MessageDetail(
    val id: Long? = null, val messageBody: String? = null, val messageDate: Date? = null,
    val messageHeader: String? = null, val readed: Boolean? = null, val senderType: String? = null,
    val status: String? = null, val format: String? = null, val attachedDocuments: List<DocumentVO> = emptyList()
)

data class DevicePreference(
    val id: Long? = null, val deviceId: String? = null, val promotion: Boolean? = null, val userId: Long? = null
)

// ---- mood / happy challenge ------------------------------------------------------------------------------------
data class MoodItem(val id: Long? = null, val moodDate: Date? = null, val moodValue: String? = null) {
    val isHeader get() = (id ?: 0) < 0
    val weekNumber: Int? get() = moodDate?.let { java.util.Calendar.getInstance().apply { time = it }.get(java.util.Calendar.WEEK_OF_YEAR) }
    val onlyDayName: String? get() = moodDate?.let { java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(it) }
    fun shortDate(c: Context): String? = moodDate?.let {
        java.text.SimpleDateFormat(if (java.util.Locale.getDefault().language == "hu") "MM.dd" else "dd.MM", java.util.Locale.getDefault()).format(it)
    }
}
data class MoodCountItem(val id: Long? = null, val mood: String? = null, val count: String? = null)
data class PrizeItem(val id: Long? = null, val level: String? = null, val point: Int? = null)
data class QuoteItem(val id: Long = 0, val quote: String? = null, val author: String? = null, val video: String? = null)

data class MoodGameGift(val id: Int? = null, val name: String? = null, val description: String? = null, val photo: Int? = null)
data class MoodGameStage(
    val id: Int? = null, val stageOrder: Int? = null, val targetDays: Int? = null, val days: Int? = null,
    val daysOfMissed: Int? = null, val gifts: List<MoodGameGift> = emptyList()
)
data class CompletedMoodGameStage(
    val id: Int? = null, val stageOrder: Int? = null, val targetDays: Int? = null, val days: Int? = null,
    val daysOfMissed: Int? = null, val stageCompleted: Int? = null, val receiptDate: Date? = null,
    val chosenGift: MoodGameGift? = null, val gifts: List<MoodGameGift> = emptyList()
)
data class StudentMoodGame(
    val id: Int? = null, val active: Boolean? = null, val gameStart: Date? = null, val gameFinish: Date? = null,
    val currentStageStart: Date? = null, val lastReported: Date? = null, val currentStage: MoodGameStage? = null,
    val nextStage: MoodGameStage? = null, val completedStages: List<CompletedMoodGameStage> = emptyList(),
    val failedStages: List<MoodGameStage> = emptyList(), val reportedDays: List<Date> = emptyList()
)

// ---- community -------------------------------------------------------------------------------------------------
data class CategoryItem(
    val id: Long = 0, val code: String? = null, val name: String? = null, val description: String? = null,
    val parentCategory: Long? = null, val parentCategoryCode: String? = null, val parentCategoryName: String? = null,
    val isSignificant: Boolean = false, var selected: Boolean = false
)

data class CommunityMember(
    val id: Long? = null, val userId: Long? = null, val firstName: String? = null, val lastName: String? = null,
    val description: String? = null, val photoUrl: String? = null, val modules: String? = null,
    val facebookLink: String? = null, val instagramLink: String? = null, val linkedInLink: String? = null,
    val tiktokLink: String? = null, val faculties: List<CategoryItem> = emptyList(),
    val interests: List<CategoryItem> = emptyList(), val preferredLanguages: List<CategoryItem> = emptyList(),
    val potentialFriends: List<Long> = emptyList(), val suggestedInterests: List<String> = emptyList()
) {
    val facultiesListAsString get() = faculties.joinToString(", ") { it.name ?: "" }
    val fullName get() = listOfNotNull(lastName, firstName).joinToString(" ")
}
