package hu.unideb.studyversity.ui.mood.gameEnded

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hu.unideb.studyversity.R

class ActivityMoodGameEnded : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_game_ended)
    }
    override fun finish() { super.finish(); overridePendingTransition(R.anim.fake_anim, R.anim.slide_down) }
}
