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

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryart.design.theme.LabelSmall
import com.cryart.design.theme.SSTheme
import com.cryart.design.theme.SecondaryLighter
import com.cryart.design.theme.Spacing4
import com.cryart.design.theme.Title
import com.cryart.design.theme.parse
import com.cryart.design.widgets.CoilImage
import com.cryart.sabbathschool.core.extensions.coroutines.flow.rememberFlowWithLifecycle
import com.cryart.sabbathschool.core.model.SSReadingDisplayOptions
import com.cryart.sabbathschool.core.model.themeColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class LessonInfoComponent(
    composeView: ComposeView,
    private val infoFlow: Flow<LessonInfoModel>,
    private val optionsFlow: Flow<SSReadingDisplayOptions>
) {
    init {
        composeView.setContent {
            SSTheme {
                LessonInfoView(
                    infoFlow = infoFlow,
                    optionsFlow = optionsFlow
                )
            }
        }
    }
}

data class LessonInfoModel(
    val cover: String? = null,
    val title: String = "",
    val subTitle: String = "",
    val scrollY: Int = 0,
)

@Composable
fun LessonInfoView(
    modifier: Modifier = Modifier,
    infoFlow: Flow<LessonInfoModel> = emptyFlow(),
    optionsFlow: Flow<SSReadingDisplayOptions> = emptyFlow()
) {
    val model by rememberFlowWithLifecycle(flow = infoFlow)
        .collectAsState(initial = LessonInfoModel())
    val options by rememberFlowWithLifecycle(flow = optionsFlow)
        .collectAsState(initial = SSReadingDisplayOptions(isSystemInDarkTheme()))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.parse(options.themeColor(LocalContext.current)))
    ) {
        CoilImage(
            data = model.cover,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val viewAlpha = (1 - (model.scrollY / 800f))
                    alpha = viewAlpha.coerceAtLeast(0f)
                    translationY = model.scrollY * 0.5f
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .graphicsLayer {
                    val viewAlpha = (1 - (model.scrollY / 700f))
                    alpha = viewAlpha.coerceAtLeast(0f)
                },
            verticalArrangement = Arrangement.spacedBy(Spacing4)
        ) {
            Text(
                text = model.subTitle.uppercase(),
                style = LabelSmall.copy(
                    color = SecondaryLighter,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.title,
                style = Title.copy(
                    color = Color.White,
                    fontSize = 34.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
