package hu.unideb.studyversity.ui.community

/** The three category groups a community profile is described with. [code] is the parent category code in the API. */
enum class ChipSelectionType(val code: String) {
    INTERESTS("interests"), FACULTY_NAMES("faculty_names"), LANGUAGES("languages")
}

enum class MemberListMode { MY_INTERESTS, SUGGESTIONS }
