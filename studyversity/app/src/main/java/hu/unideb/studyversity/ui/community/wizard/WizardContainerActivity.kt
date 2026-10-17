package hu.unideb.studyversity.ui.community.wizard

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.CategoryItem
import hu.unideb.studyversity.databinding.ActivityNewWizardBinding
import hu.unideb.studyversity.ui.community.ChipSelectionType
import hu.unideb.studyversity.utils.dialog.CustomViewDialog

/** Registration wizard of UD Community: welcome → 4 pages → finish. Result OK once the profile is registered. */
class WizardContainerActivity : AppCompatActivity(), ChipSelectorHost {
    val viewModel: WizardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityNewWizardBinding.inflate(layoutInflater).root)
    }

    private val navController get() = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    fun showAlertDialog(message: String) {
        CustomViewDialog(
            title = getString(R.string.error), message = message, positiveText = getString(R.string.ok),
            mainIcon = R.drawable.ic_exclamation_circle_solid
        ).show(supportFragmentManager, "wizard-alert")
    }

    fun openChipSelector(type: ChipSelectionType) {
        navController.navigate(R.id.chipSelectorFragment, ChipSelectorFragment.args(type, viewModel.selection(type).map { it.id }))
    }

    override fun onChipsChosen(type: ChipSelectionType, items: List<CategoryItem>) {
        viewModel.setSelection(type, items)
        navController.popBackStack()
    }

    fun finishRegistered() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
