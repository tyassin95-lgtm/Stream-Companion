package hu.unideb.studyversity.utils.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.DialogFragment
import hu.unideb.studyversity.databinding.CustomViewDialogBinding

/**
 * The round-icon alert used all over the original app (no internet, open external app, confirmations ...).
 * Callbacks are kept in the instance, so the dialog is dismissed with the screen on process death.
 */
class CustomViewDialog(
    private val title: String,
    private val message: String,
    private val secondLabel: String? = null,
    private val positiveText: String? = null,
    private val negativeText: String? = null,
    private val onPositive: (() -> Unit)? = null,
    private val onNegative: (() -> Unit)? = null,
    @DrawableRes private val mainIcon: Int? = null,
    @DrawableRes private val positiveIcon: Int? = null,
    private val cancelableDialog: Boolean = true,
    private val onDismissed: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: CustomViewDialogBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(cancelableDialog)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = cancelableDialog
        val b = CustomViewDialogBinding.inflate(inflater, container, false)
        _binding = b
        b.titleText.text = title
        b.messageText.text = message
        b.secondLabel.apply { text = secondLabel; visibility = if (secondLabel.isNullOrEmpty()) View.GONE else View.VISIBLE }
        mainIcon?.let { b.mainIcon.setImageResource(it) }
        b.positiveButton.apply {
            if (positiveText.isNullOrEmpty()) visibility = View.GONE else text = positiveText
            positiveIcon?.let { setIconResource(it) } ?: run { icon = null }
            setOnClickListener { dismiss(); onPositive?.invoke() }
        }
        b.negativeButton.apply {
            if (negativeText.isNullOrEmpty()) visibility = View.GONE else text = negativeText
            setOnClickListener { dismiss(); onNegative?.invoke() }
        }
        return b.root
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissed?.invoke()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
