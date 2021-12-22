/*
 * Copyright (c) 2021. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cryart.sabbathschool.lessons.ui.readings.components

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.lifecycle.LifecycleOwner
import com.cryart.design.color.withAlpha
import com.cryart.design.ext.doOnApplyWindowInsets
import com.cryart.sabbathschool.core.extensions.activity.setLightStatusBar
import com.cryart.sabbathschool.core.extensions.coroutines.flow.collectIn
import com.cryart.sabbathschool.core.extensions.view.tint
import com.cryart.sabbathschool.core.model.SSReadingDisplayOptions
import com.cryart.sabbathschool.core.model.colorTheme
import com.cryart.sabbathschool.core.model.displayTheme
import com.cryart.sabbathschool.core.ui.BaseDataComponent
import com.cryart.sabbathschool.lessons.databinding.SsLessonsToolbarBinding
import kotlinx.coroutines.flow.Flow

class ToolbarComponent(
    lifecycleOwner: LifecycleOwner,
    private val binding: SsLessonsToolbarBinding
) : BaseDataComponent<SSReadingDisplayOptions>(lifecycleOwner) {
    init {
        binding.ssLessonsToolbar.apply {
            fitsSystemWindows = false
            doOnApplyWindowInsets { insetView, windowInsets, _, initialMargins ->
                insetView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    updateMargins(top = initialMargins.top + windowInsets.getInsets(systemBars()).top)
                }
            }
        }
    }

    private var displayOptions: SSReadingDisplayOptions? = null
        set(value) {
            field = value

            val textColor = when (value?.displayTheme(binding.root.context)) {
                SSReadingDisplayOptions.SS_THEME_DARK -> Color.WHITE
                else -> Color.BLACK
            }
            binding.ssLessonsToolbarTitle.setTextColor(textColor)
        }

    private val solidColor: Int? get() = displayOptions?.colorTheme(binding.root.context)

    fun setTitle(title: String) {
        binding.ssLessonsToolbarTitle.text = title
    }

    override fun collect(dataFlow: Flow<SSReadingDisplayOptions>) {
        dataFlow.collectIn(owner) { options -> displayOptions = options }
    }

    fun onContentScroll(scrollY: Int, anchorHeight: Int, activity: Activity) {
        val height = anchorHeight - binding.ssLessonsToolbar.height
        val scrollValue = scrollY.coerceAtLeast(0).toDouble()
        val viewAlpha = (scrollValue / height).coerceAtLeast(0.0)
        val colorAlpha = (viewAlpha * MAX_ALPHA).toInt()
        val isSolid = colorAlpha >= MIN_SOLID_ALPHA

        val backgroundColor = solidColor?.withAlpha(colorAlpha.coerceIn(0, MAX_ALPHA)) ?: return
        with(binding.ssLessonsToolbar) {
            setBackgroundColor(backgroundColor)
            isActivated = isSolid
        }

        binding.ssLessonsToolbarTitle.alpha =
            if (isSolid) {
                viewAlpha.toFloat()
            } else {
                viewAlpha.minus(0.5).toFloat()
            }

        with(activity) {
            window?.statusBarColor = backgroundColor
            val iconTint = if (displayOptions?.displayTheme(this) != SSReadingDisplayOptions.SS_THEME_DARK) {
                setLightStatusBar(isSolid)
                iconInt(viewAlpha)
            } else {
                Color.WHITE
            }

            with(binding.ssLessonsToolbar) {
                navigationIcon?.tint(iconTint)
                overflowIcon?.tint(iconTint)
                menu.forEach { item ->
                    item.icon?.tint(iconTint)
                }
            }
        }
    }

    private fun iconInt(alpha: Double): Int {
        val ratio = alpha.coerceAtMost(1.0).toFloat()
        return ColorUtils.blendARGB(Color.WHITE, Color.BLACK, ratio)
    }

    companion object {
        private const val MAX_ALPHA = 255
        private const val MIN_SOLID_ALPHA = 200
    }
}
