package hu.unideb.studyversity.ui.mood.levelUpgrade

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hu.unideb.studyversity.R

/** Transparent host for [MoodLevelUpgradeFragment] (declared in the layout). */
class ActivityMoodLevelUpgrade : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_level_upgrade)
    }
    override fun finish() { super.finish(); overridePendingTransition(R.anim.fake_anim, R.anim.slide_down) }
}
