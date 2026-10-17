package hu.unideb.studyversity.ui.community.wizard

import hu.unideb.studyversity.data.model.CategoryItem
import hu.unideb.studyversity.ui.community.ChipSelectionType

/** Implemented by the screens that can host the chip selector (registration wizard, chip selector activity). */
interface ChipSelectorHost {
    fun onChipsChosen(type: ChipSelectionType, items: List<CategoryItem>)
}
