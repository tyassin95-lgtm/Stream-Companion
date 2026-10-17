package hu.unideb.studyversity.data.remote

/** Envelope of every REST answer of the Studyversity server (the "Abydos" platform). */
data class RestResponse<T>(
    val data: List<T>? = null,
    val startRow: Int? = null,
    val endRow: Int? = null,
    val totalRows: Int? = null,
    /** "SUCCESS" or "FAILURE" */
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
) {
    val isSuccess get() = status.equals("SUCCESS", ignoreCase = true)

    /** The payload list; throws [ApiException] when the server reported a failure. */
    fun checkedData(): List<T> {
        if (!isSuccess) throw ApiException(errorCode, errorMessage)
        return data.orEmpty()
    }

    fun checkedFirst(): T = checkedData().firstOrNull() ?: throw ApiException(errorCode, errorMessage ?: "Empty response")
}

class ApiException(val code: String?, message: String?) : Exception(message ?: code ?: "Server error")

// ---- request bodies ---------------------------------------------------------------------------------------------

/** One condition of an Abydos / SmartClient AdvancedCriteria, e.g. fieldName=name, operator=iContains, value=Kiss. */
data class Criterion(val fieldName: String, val operator: String, val value: Any?)

/** `{"operator":"and","criteria":[...]}` */
data class AdvancedCriteria(val operator: String = "and", val criteria: List<Criterion> = emptyList())

data class AbydosFetchRequest(val startRow: Int? = null, val endRow: Int? = null, val criteria: AdvancedCriteria? = null)

data class MemberCriteria(val name: String? = null)
data class MemberFetchRequest(val startRow: Int? = null, val endRow: Int? = null, val criteria: MemberCriteria? = null)

data class DeviceRegistrationRequest(
    val application: String? = null,
    val communicationId: String? = null,
    val description: String? = null,
    val deviceId: String? = null,
    /** ANDROID */
    val deviceOs: String? = "ANDROID",
    /** MOBILE */
    val deviceType: String? = "MOBILE",
    val language: String? = null
)

/** Wire format of the preferences row (note the snake_case names). */
data class DevicePreferenceDto(
    val id: Long? = null,
    val deviceId: String? = null,
    val userId: Long? = null,
    val other_notification: Boolean? = null,
    val student_information: Boolean? = null,
    val promotion: Boolean? = null
)
