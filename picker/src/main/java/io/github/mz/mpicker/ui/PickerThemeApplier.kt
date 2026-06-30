package io.github.mz.mpicker.ui

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.TextView
import androidx.annotation.ColorInt
import io.github.mz.mpicker.api.PickerTheme

internal object PickerThemeApplier {
    fun buttonBackground(theme: PickerTheme, radiusDp: Float = 8f): StateListDrawable {
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), rounded(theme.primaryPressed, radiusDp))
            addState(intArrayOf(-android.R.attr.state_enabled), rounded(theme.primaryDisabled, radiusDp))
            addState(intArrayOf(), rounded(theme.primary, radiusDp))
        }
    }

    fun chipBackground(
        @ColorInt color: Int,
        @ColorInt strokeColor: Int,
        radiusDp: Float = 9f,
    ): GradientDrawable = rounded(color, radiusDp).apply {
        setStroke(dp(radiusDp / 9f).toInt().coerceAtLeast(1), strokeColor)
    }

    fun textButton(view: TextView, @ColorInt color: Int, bold: Boolean = false) {
        view.setTextColor(color)
        view.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    fun textColorState(@ColorInt normal: Int, @ColorInt disabled: Int): ColorStateList =
        ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(disabled, normal),
        )

    fun rounded(@ColorInt color: Int, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp)
        setColor(color)
    }

    fun dp(value: Float): Float = value * android.content.res.Resources.getSystem().displayMetrics.density
}
