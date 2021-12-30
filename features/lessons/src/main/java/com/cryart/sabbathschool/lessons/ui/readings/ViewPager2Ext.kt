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

package com.cryart.sabbathschool.lessons.ui.readings

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.cryart.sabbathschool.core.model.SSReadingDisplayOptions
import com.cryart.sabbathschool.lessons.R

// https://issuetracker.google.com/u/0/issues/143095219
internal fun ViewPager2.wrapContentHeight(position: Int) {
    val view = (get(0) as? RecyclerView)?.layoutManager?.findViewByPosition(position) ?: return
    view.post {
        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(wMeasureSpec, hMeasureSpec)

        if (layoutParams.height != view.measuredHeight) {
            layoutParams = (layoutParams as LinearLayout.LayoutParams)
                .also { lp -> lp.height = view.measuredHeight }
        }
    }
}

internal fun ViewPager2.updateDisplayOptions(options: SSReadingDisplayOptions, position: Int) {
    val view = findViewWithTag<View?>("ssReadingView_$position")
    view?.findViewById<SSReadingView?>(R.id.ss_reading_view)?.updateReadingDisplayOptions(options)
}
