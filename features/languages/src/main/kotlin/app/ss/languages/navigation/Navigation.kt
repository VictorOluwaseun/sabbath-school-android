/*
 * Copyright (c) 2022. Adventech <info@adventech.io>
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

package app.ss.languages.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import app.ss.design.compose.transitions.scaleInEnterTransition
import app.ss.design.compose.transitions.scaleInPopEnterTransition
import app.ss.design.compose.transitions.scaleOutExitTransition
import app.ss.design.compose.transitions.scaleOutPopExitTransition
import app.ss.languages.LanguagesRoute
import com.google.accompanist.navigation.animation.composable

private const val languagesRoute = "languages_route"

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.languagesScreen(
    mainPadding: PaddingValues,
    onBackClick: () -> Unit,
) {
    composable(
        route = languagesRoute,
        enterTransition = { scaleInEnterTransition() },
        exitTransition = { scaleOutExitTransition() },
        popEnterTransition = { scaleInPopEnterTransition() },
        popExitTransition = { scaleOutPopExitTransition() }
    ) {
        LanguagesRoute(
            mainPadding = mainPadding,
            onNavBack = onBackClick
        )
    }
}

fun NavController.navigateToLanguages() = navigate(languagesRoute)

