package hu.unideb.studyversity.ui.instructor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.StaffStudent
import kotlinx.coroutines.launch

/** The student the instructor works on, shared by the tabs of the console. */
class InstructorViewModel : ViewModel() {
    private val _student = MutableLiveData<StaffStudent?>()
    val student: LiveData<StaffStudent?> get() = _student
    private var requested = false

    /** Loads the first managed student once; [onError] gets what went wrong (e.g. a signed-out session). */
    fun load(onError: (Throwable) -> Unit) {
        if (requested) return
        requested = true
        viewModelScope.launch {
            try {
                _student.value = ServiceLocator.backend.managedStudents().firstOrNull()
            } catch (e: Exception) {
                requested = false
                onError(e)
            }
        }
    }

    fun update(student: StaffStudent) { _student.value = student }
}
