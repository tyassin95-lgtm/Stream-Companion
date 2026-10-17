package hu.unideb.studyversity.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object ConnectivityUtil {
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
