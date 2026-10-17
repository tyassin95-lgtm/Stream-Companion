package hu.unideb.studyversity

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import java.util.Locale

/** Helpers that read app resources and assets in tests. */
object StringScan {
    /** `name=text` of every string resource (English and Hungarian) matching [regex]; the app-id placeholders are skipped. */
    fun hits(regex: Regex): List<String> {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val out = ArrayList<String>()
        for (lang in listOf("en", "hu")) {
            val c = Configuration(base.resources.configuration).apply { setLocale(Locale(lang)) }
            val ctx = base.createConfigurationContext(c)
            for (f in R.string::class.java.fields) {
                val text = runCatching { ctx.getString(f.getInt(null)) }.getOrNull() ?: continue
                if (regex.containsMatchIn(text)) out.add("$lang/${f.name}=${text.take(60)}")
            }
        }
        return out
    }
}

object ApplicationProviderAssets {
    fun read(path: String): String =
        ApplicationProvider.getApplicationContext<Context>().assets.open(path).bufferedReader().use { it.readText() }
}
