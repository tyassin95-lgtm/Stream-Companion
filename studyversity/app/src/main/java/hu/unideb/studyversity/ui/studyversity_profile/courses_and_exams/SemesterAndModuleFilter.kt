package hu.unideb.studyversity.ui.studyversity_profile.courses_and_exams

import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import hu.unideb.studyversity.R

/**
 * The two exposed dropdowns (semester + module) at the top of the courses and exams lists.
 * Like the original, the last semester / module is preselected.
 */
class SemesterAndModuleFilter(private val onChange: () -> Unit) {
    var semester = ""
        private set
    var module = ""
        private set

    fun bind(header: View, semesters: List<String>, modules: List<String>) {
        setup(header.findViewById(R.id.my_spinner_dropdown), semesters, semester) { semester = it }
        setup(header.findViewById(R.id.spinnerForModuleName), modules, module) { module = it }
    }

    private fun setup(view: AutoCompleteTextView, values: List<String>, current: String, set: (String) -> Unit) {
        view.setDropDownBackgroundDrawable(ContextCompat.getDrawable(view.context, R.drawable.autocomplete_dropdown))
        view.setAdapter(ArrayAdapter(view.context, R.layout.dropdown_list_item, values))
        val shown = current.takeIf { it in values } ?: values.lastOrNull().orEmpty()
        set(shown)
        view.setText(shown, false)
        view.setOnItemClickListener { _, _, pos, _ -> set(values[pos]); onChange() }
    }

    fun preselect(semesters: List<String>, modules: List<String>) {
        semester = semesters.lastOrNull().orEmpty()
        module = modules.lastOrNull().orEmpty()
    }
}
