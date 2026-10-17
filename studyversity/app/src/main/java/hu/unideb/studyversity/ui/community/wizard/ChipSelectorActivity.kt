package hu.unideb.studyversity.ui.community.wizard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.CategoryItem
import hu.unideb.studyversity.databinding.ActivityChipSelectorBinding
import hu.unideb.studyversity.ui.community.ChipSelectionType

/** Hosts [ChipSelectorFragment] for the profile editor. Extras: `chipSelectionType`, `selectedChips`; result: `chipType`, `selectedChipIds`. */
class ChipSelectorActivity : AppCompatActivity(), ChipSelectorHost {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityChipSelectorBinding.inflate(layoutInflater).root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChipSelectorFragment().apply { arguments = intent.extras })
                .commit()
        }
    }

    override fun onChipsChosen(type: ChipSelectionType, items: List<CategoryItem>) {
        setResult(Activity.RESULT_OK, Intent().putExtra(RESULT_TYPE, type.name).putExtra(RESULT_IDS, items.map { it.id }.toLongArray()))
        finish()
    }

    companion object {
        const val RESULT_TYPE = "chipType"
        const val RESULT_IDS = "selectedChipIds"

        fun intent(context: android.content.Context, type: ChipSelectionType, selectedIds: List<Long>) =
            Intent(context, ChipSelectorActivity::class.java).putExtras(ChipSelectorFragment.args(type, selectedIds))
    }
}
