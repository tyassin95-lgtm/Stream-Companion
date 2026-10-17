package hu.unideb.studyversity.ui.main

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.databinding.ActivityMainBinding
import hu.unideb.studyversity.ui.base.BaseActivity
import kotlinx.coroutines.launch
import java.util.Locale

/** Hosts the main navigation graph (home menu + all fragments) under the Studyversity toolbar. */
class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()

        lifecycleScope.launch { ServiceLocator.session.refresh() }

        // A tapped push notification carries the id of the message to show.
        val messageId = intent.extras?.getLong("messageId", -1L) ?: -1L
        if (messageId != -1L && savedInstanceState == null) {
            val nav = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
            nav.navigate(R.id.messagesFragment, Bundle().apply { putLong("messageId", messageId) })
        }
    }

    override fun onResume() {
        super.onResume()
        // When the UI language changed, locale dependent caches would be dropped here (nothing is cached locally).
        ServiceLocator.prefs.language = Locale.getDefault().language
    }
}
