package hu.unideb.studyversity.ui.community.wizard

import android.view.LayoutInflater
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.CategoryItem

object ChipGroups {
    /** Fills [group] with the selected categories (rounded dark chips); an "add" chip is appended when [onAdd] is given. */
    fun fill(group: ChipGroup, items: List<CategoryItem>, onAdd: (() -> Unit)?) {
        group.removeAllViews()
        val inflater = LayoutInflater.from(group.context)
        items.forEach { c ->
            val chip = inflater.inflate(R.layout.chip_significant_item, group, false) as Chip
            chip.text = c.name
            group.addView(chip)
        }
        if (onAdd != null) {
            val add = inflater.inflate(R.layout.chip_add_item, group, false) as Chip
            add.setText(R.string.add_chip)
            add.setOnClickListener { onAdd() }
            group.addView(add)
        }
    }
}
