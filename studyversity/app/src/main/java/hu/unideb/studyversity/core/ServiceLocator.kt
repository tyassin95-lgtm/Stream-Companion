package hu.unideb.studyversity.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import hu.unideb.studyversity.data.Backend
import hu.unideb.studyversity.data.local.LocalBackend
import hu.unideb.studyversity.data.remote.BackendConfig
import hu.unideb.studyversity.data.remote.RemoteBackend

/** Minimal manual DI (the original used Dagger). */
object ServiceLocator {
    lateinit var appContext: Context
        private set
    lateinit var gson: Gson
        private set
    lateinit var prefs: AppPreferences
        private set
    lateinit var backend: Backend
        private set
    lateinit var session: SessionManager
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create()
        prefs = AppPreferences(appContext, gson)
        backend = if (BackendConfig.USE_REMOTE) RemoteBackend(prefs, gson) else LocalBackend(appContext, gson)
        session = SessionManager(backend, prefs)
    }
}
