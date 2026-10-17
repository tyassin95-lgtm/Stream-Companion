package hu.unideb.studyversity.ui.base

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.AuthRequiredException
import hu.unideb.studyversity.ui.user.login.LoginActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Common behaviour of the screens hosted in MainActivity (title handling, loading overlay, error + login handling). */
abstract class BaseFragment : Fragment() {

    protected val backend get() = ServiceLocator.backend
    protected val session get() = ServiceLocator.session
    protected val prefs get() = ServiceLocator.prefs

    private var loading: LoadingDialog? = null
    private var loginCallback: (() -> Unit)? = null
    private var closeAfterLoginError = false

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == LoginActivity.RESULT_LOGGED_IN) {
            loginCallback?.invoke()
        } else if (closeAfterLoginError) {
            // also covers RESULT_INSTRUCTOR: the console is open, this student screen has nothing to show
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    protected fun toolbarHost(): ToolbarHost? = activity as? ToolbarHost

    protected fun setScreenTitle(@StringRes subTitle: Int, up: Boolean = true) =
        setScreenTitle(getString(subTitle), up)

    protected fun setScreenTitle(subTitle: String?, up: Boolean = true) {
        toolbarHost()?.apply {
            updateTitle(getString(R.string.app_name))
            updateSubTitle(subTitle ?: "")
            showUpButton(up)
        }
    }

    /** Runs [block] in the view's lifecycle; auth and other errors are turned into UI. */
    protected fun load(showProgress: Boolean = true, retry: (() -> Unit)? = null, block: suspend CoroutineScope.() -> Unit) {
        if (showProgress) showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                handleError(e, retry)
            } finally {
                hideLoading()
            }
        }
    }

    protected fun handleError(e: Throwable, retry: (() -> Unit)? = null) {
        if (e is AuthRequiredException) { requireLogin { retry?.invoke() }; return }
        val root = view ?: return
        val msg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: getString(R.string.unknown_error)
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG).apply {
            if (retry != null) setAction(R.string.retry) { retry() }
        }.show()
    }

    /** Opens the login screen; [onLoggedIn] runs when the user signs in, otherwise the fragment closes if [closeOnCancel]. */
    protected fun requireLogin(closeOnCancel: Boolean = true, onLoggedIn: () -> Unit) {
        closeAfterLoginError = closeOnCancel
        loginCallback = onLoggedIn
        loginLauncher.launch(Intent(requireContext(), LoginActivity::class.java))
    }

    protected fun showLoading() {
        if (loading?.isShowing == true) return
        loading = LoadingDialog(requireContext()).also { it.show() }
    }

    protected fun hideLoading() { loading?.dismiss(); loading = null }

    override fun onDestroyView() {
        hideLoading()
        super.onDestroyView()
    }

    protected fun snack(message: String) { view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() } }
    protected fun snack(@StringRes message: Int) = snack(getString(message))
}
