package hu.unideb.studyversity.ui.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R

/** Base for all screens that carry the standard toolbar (view id `toolbar`). */
abstract class BaseActivity : AppCompatActivity(), ToolbarHost {
    protected var toolbar: Toolbar? = null

    /** Call after setContentView. */
    protected fun initToolbar(upEnabled: Boolean = false) {
        toolbar = findViewById(R.id.toolbar)
        toolbar?.let { setSupportActionBar(it) }
        showUpButton(upEnabled)
    }

    override fun updateTitle(title: String?) { toolbar?.title = title }
    override fun updateSubTitle(subTitle: String?) { if (subTitle != null) toolbar?.subtitle = subTitle }
    override fun showUpButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
        supportActionBar?.setDisplayShowHomeEnabled(show)
    }
    override val snackbarAnchor: View? get() = findViewById(android.R.id.content)

    fun showSnackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
        snackbarAnchor?.let { Snackbar.make(it, message, length).show() }
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressedDispatcher.onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
