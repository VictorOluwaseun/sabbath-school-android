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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ss.lessons.data.repository.lessons.LessonsRepository
import app.ss.lessons.data.repository.media.MediaRepository
import app.ss.lessons.data.repository.quarterly.QuarterliesRepository
import app.ss.lessons.data.repository.user.UserDataRepository
import app.ss.models.LessonPdf
import app.ss.models.PublishingInfo
import app.ss.models.SSReadComments
import app.ss.models.SSReadHighlights
import com.cryart.sabbathschool.core.extensions.coroutines.DispatcherProvider
import com.cryart.sabbathschool.core.extensions.coroutines.flow.stateIn
import com.cryart.sabbathschool.core.extensions.intent.lessonIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val lessonsRepository: LessonsRepository,
    private val userDataRepository: UserDataRepository,
    private val savedStateHandle: SavedStateHandle,
    quarterliesRepository: QuarterliesRepository,
    dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _audioAvailable = MutableStateFlow(false)
    val audioAvailableFlow: StateFlow<Boolean> get() = _audioAvailable.asStateFlow()

    private val _videoAvailable = MutableStateFlow(false)
    val videoAvailableFlow: StateFlow<Boolean> get() = _videoAvailable.asStateFlow()

    private val _pdfAvailable = MutableStateFlow(false)
    val pdfAvailableFlow: StateFlow<Boolean> get() = _pdfAvailable.asStateFlow()

    private val _lessonPdfs = MutableStateFlow("" to emptyList<LessonPdf>())
    val lessonPdfsFlow: StateFlow<Pair<String, List<LessonPdf>>> = _lessonPdfs

    val publishingInfo: StateFlow<PublishingInfo?> = quarterliesRepository.getPublishingInfo()
        .mapNotNull { it.data }
        .distinctUntilChanged()
        .stateIn(viewModelScope, null)

    val lessonIndex: String? get() = savedStateHandle.lessonIndex

    init {
        lessonIndex?.let { index ->
            viewModelScope.launch(dispatcherProvider.default) {
                val resource = mediaRepository.getAudio(index)
                _audioAvailable.emit(resource.data.isNullOrEmpty().not())
            }
            viewModelScope.launch(dispatcherProvider.default) {
                val videoResource = mediaRepository.getVideo(index)
                _videoAvailable.emit(videoResource.data.isNullOrEmpty().not())
            }
            viewModelScope.launch(dispatcherProvider.default) {
                val lessonResource = lessonsRepository.getLessonInfo(index)
                val pdfs = lessonResource.data?.pdfs ?: emptyList()
                _lessonPdfs.emit(index to pdfs)
                _pdfAvailable.emit(pdfs.isNotEmpty())
            }
        }

        lessonsRepository.checkReaderArtifact()
    }

    internal fun readUserContentFlow(
        readIndex: String,
        defaultContent: ReadUserContent?
    ): StateFlow<ReadUserContent> {
        val initial = defaultContent ?: ReadUserContent(
            readIndex,
            SSReadComments(readIndex, emptyList()),
            SSReadHighlights(readIndex)
        )
        return combine(
            userDataRepository.getComments(readIndex),
            userDataRepository.getHighlights(readIndex)
        ) { comments, highlights ->
            ReadUserContent(
                readIndex,
                comments.getOrNull() ?: initial.comments,
                highlights.getOrNull() ?: initial.highlights
            )
        }.stateIn(viewModelScope, initial)
    }

}
