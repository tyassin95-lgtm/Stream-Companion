package hu.unideb.studyversity.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.utils.dialog.CustomViewDialog

object Utils {
    /** Launches an installed app or opens its Play Store page. */
    fun startApplicationFromPlayStore(context: Context, packageName: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) context.startActivity(launch)
        else context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Asks "open <app>?" first when a [fragmentManager] is available. */
    fun startApplication(context: Context, fragmentManager: FragmentManager?, packageName: String, appName: String) {
        if (fragmentManager == null) { startApplicationFromPlayStore(context, packageName); return }
        CustomViewDialog(
            title = appName,
            message = context.getString(R.string.app_open_text, appName),
            positiveText = context.getString(R.string.ok),
            negativeText = context.getString(R.string.cancel),
            onPositive = { startApplicationFromPlayStore(context, packageName) },
            mainIcon = R.drawable.ic_info_outline
        ).show(fragmentManager, "open_app")
    }

    fun languageCode(context: Context): String =
        context.resources.configuration.locales[0].language
}
