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

    suspend fun refresh(): User? {
        val u = runCatching { backend.currentUser() }.getOrNull()
        _user.postValue(u)
        return u
    }

    suspend fun login(userName: String, password: String): User {
        val u = backend.login(userName, password)
        _user.postValue(u)
        return u
    }

    suspend fun logout() {
        runCatching { backend.logout() }
        prefs.deleteUserPrefs()
        _user.postValue(null)
    }
}
