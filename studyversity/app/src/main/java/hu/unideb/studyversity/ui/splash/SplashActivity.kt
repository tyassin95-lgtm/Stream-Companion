package hu.unideb.studyversity.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hu.unideb.studyversity.ui.main.MainActivity

/** Shows the splash background (theme) and forwards to MainActivity, passing a push `messageId` through. */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val messageId = intent?.extras?.getString("messageId", "")?.toLongOrNull() ?: -1L
        val next = Intent(this, MainActivity::class.java)
        if (messageId != -1L) next.putExtra("messageId", messageId)
        startActivity(next)
        finish()
    }
}
