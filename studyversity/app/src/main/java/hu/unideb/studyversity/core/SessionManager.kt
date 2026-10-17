package hu.unideb.studyversity.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.unideb.studyversity.data.Backend
import hu.unideb.studyversity.data.model.User

/** Holds the signed-in user (the original kept it in CurrentUserInfoHolder + Room). */
class SessionManager(private val backend: Backend, private val prefs: AppPreferences) {
    private val _user = MutableLiveData<User?>(null)
    val user: LiveData<User?> get() = _user
    val isLoggedIn get() = _user.value != null
    val currentUser get() = _user.value

    private fun publish(u: User?) {
        // on the main thread the value must be visible right away (a screen opened next reads it)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) _user.value = u else _user.postValue(u)
    }

    suspend fun refresh(): User? {
        val u = runCatching { backend.currentUser() }.getOrNull()
        publish(u)
        return u
    }

    suspend fun login(userName: String, password: String): User {
        val u = backend.login(userName, password)
        publish(u)
        return u
    }

    suspend fun logout() {
        runCatching { backend.logout() }
        prefs.deleteUserPrefs()
        publish(null)
    }
}
