package hu.unideb.studyversity.ui.user.login

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.databinding.ActivityLocalLoginBinding
import kotlinx.coroutines.launch

/**
 * Sign-in screen. The original app opened the OIDC provider in a browser (AppAuth) and finished with
 * RESULT_LOGGED_IN / RESULT_CANCELED; this keeps the same result contract so the identity provider can be
 * plugged into [ServiceLocator.session] later without touching the callers.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocalLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)

        binding.cancelButton.setOnClickListener { finish() }
        binding.loginButton.setOnClickListener { submit() }
        binding.passwordInput.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
    }

    private fun submit() {
        val user = binding.neptunCodeInput.text?.toString()?.trim().orEmpty()
        val pass = binding.passwordInput.text?.toString().orEmpty()
        if (user.isEmpty() || pass.isEmpty()) {
            Snackbar.make(binding.root, R.string.login_required, Snackbar.LENGTH_LONG).show()
            return
        }
        binding.fakeLoading.root.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                ServiceLocator.session.login(user, pass)
                setResult(RESULT_LOGGED_IN)
                finish()
            } catch (e: Exception) {
                binding.fakeLoading.root.visibility = View.GONE
                Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.login_was_not_success), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        /** Same value the original LoginActivity returned on success. */
        const val RESULT_LOGGED_IN = 1
    }
}
