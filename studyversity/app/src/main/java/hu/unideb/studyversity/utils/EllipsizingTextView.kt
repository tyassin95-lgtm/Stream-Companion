package hu.unideb.studyversity.utils

import android.content.Context
import android.text.Layout
import android.text.StaticLayout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/** TextView that really ends a multi-line text with "…" even when `maxLines` + `ellipsize=end` misbehave. */
class EllipsizingTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var fullText: CharSequence = ""
    private var stale = true
    private var programmatic = false
    var isEllipsized = false
        private set

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (!programmatic) {
            fullText = text ?: ""
            stale = true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) stale = true
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        if (stale) resetText()
        super.onDraw(canvas)
    }

    private fun resetText() {
        val max = maxLines
        var work: CharSequence = fullText
        var ellipsized = false
        val width = width - compoundPaddingLeft - compoundPaddingRight
        if (max in 1 until Int.MAX_VALUE && width > 0) {
            var layout = StaticLayout.Builder.obtain(work, 0, work.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(lineSpacingExtra, lineSpacingMultiplier).build()
            if (layout.lineCount > max) {
                ellipsized = true
                work = fullText.subSequence(0, layout.getLineEnd(max - 1)).toString().trimEnd()
                while (true) {
                    val candidate = "$work$ELLIPSIS"
                    layout = StaticLayout.Builder.obtain(candidate, 0, candidate.length, paint, width)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(lineSpacingExtra, lineSpacingMultiplier).build()
                    if (layout.lineCount <= max || work.isEmpty()) { work = candidate; break }
                    work = work.subSequence(0, work.length - 1)
                }
            }
        }
        if (work != text) {
            programmatic = true
            try { text = work } finally { programmatic = false }
        }
        stale = false
        isEllipsized = ellipsized
    }

    companion object { private const val ELLIPSIS = "…" }
}
