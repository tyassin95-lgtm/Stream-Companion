package hu.unideb.studyversity

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import hu.unideb.studyversity.core.ServiceLocator
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import java.time.Duration

/** Helpers shared by the Robolectric tests. */
object TestSupport {
    /** Fresh app state: clears the local "server" and preferences, then rebuilds the service locator. */
    fun resetApp(loggedIn: Boolean) {
        val app = ApplicationProvider.getApplicationContext<StudyversityApp>()
        listOf("studyversity_local_backend", "studyversity_prefs").forEach {
            app.getSharedPreferences(it, 0).edit().clear().commit()
        }
        app.filesDir.listFiles()?.forEach { it.delete() }
        ServiceLocator.init(app)
        if (loggedIn) {
            runBlocking { ServiceLocator.session.login("ABC123", "secret") }
            shadowOf(Looper.getMainLooper()).idle() // LiveData.postValue of the session
        }
    }

    /** Lets posted work (coroutines on the main dispatcher, view animations, delayed runnables) run. */
    fun settle() {
        val looper = shadowOf(Looper.getMainLooper())
        looper.idle()
        ShadowLooper.idleMainLooper(2, java.util.concurrent.TimeUnit.SECONDS)
        looper.idle()
    }
}
