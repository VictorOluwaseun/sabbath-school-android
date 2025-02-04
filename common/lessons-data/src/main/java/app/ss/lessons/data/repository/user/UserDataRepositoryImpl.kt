/*
 * Copyright (c) 2023. Adventech <info@adventech.io>
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

package app.ss.lessons.data.repository.user

import app.ss.lessons.data.api.SSLessonsApi
import app.ss.lessons.data.model.api.request.UploadPdfAnnotationsRequest
import app.ss.models.PdfAnnotations
import app.ss.models.SSReadComments
import app.ss.models.SSReadHighlights
import app.ss.network.NetworkResource
import app.ss.network.safeApiCall
import app.ss.storage.db.dao.PdfAnnotationsDao
import app.ss.storage.db.dao.ReadCommentsDao
import app.ss.storage.db.dao.ReadHighlightsDao
import app.ss.storage.db.entity.PdfAnnotationsEntity
import app.ss.storage.db.entity.ReadCommentsEntity
import app.ss.storage.db.entity.ReadHighlightsEntity
import com.cryart.sabbathschool.core.extensions.connectivity.ConnectivityHelper
import com.cryart.sabbathschool.core.extensions.coroutines.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ss.misc.DeviceHelper
import ss.prefs.api.SSPrefs
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserDataRepositoryImpl @Inject constructor(
    private val lessonsApi: SSLessonsApi,
    private val readHighlightsDao: ReadHighlightsDao,
    private val readCommentsDao: ReadCommentsDao,
    private val pdfAnnotationsDao: PdfAnnotationsDao,
    private val ssPrefs: SSPrefs,
    private val dispatcherProvider: DispatcherProvider,
    private val connectivityHelper: ConnectivityHelper,
    private val deviceHelper: DeviceHelper
) : UserDataRepository, CoroutineScope by CoroutineScope(dispatcherProvider.default) {

    override fun getHighlights(readIndex: String): Flow<Result<SSReadHighlights>> = readHighlightsDao
        .getFlow(readIndex)
        .map { Result.success(SSReadHighlights(readIndex, it?.highlights ?: "")) }
        .onStart { syncHighlights(readIndex) }
        .flowOn(dispatcherProvider.io)
        .catch {
            Timber.e(it)
            emit(Result.failure(it))
        }

    private fun syncHighlights(readIndex: String) = launch {
        val response = safeApiCall(connectivityHelper) { lessonsApi.getHighlights(readIndex) }
        if (response !is NetworkResource.Success) return@launch

        val remote = response.value.body()
        val cached = withContext(dispatcherProvider.io) { readHighlightsDao.get(readIndex) }

        remote?.let {
            if (it.timestamp.isAfter(cached?.timestamp)) {
                withContext(dispatcherProvider.io) {
                    readHighlightsDao.insertItem(
                        ReadHighlightsEntity(readIndex, it.highlights, it.timestamp)
                    )
                }
            }
        }

        cached?.let {
            if (it.timestamp.isAfter(remote?.timestamp)) {
                safeApiCall(connectivityHelper) {
                    lessonsApi.uploadHighlights(SSReadHighlights(readIndex, it.highlights))
                }
            }
        }
    }

    override fun saveHighlights(highlights: SSReadHighlights) {
        launch {
            withContext(dispatcherProvider.io) {
                readHighlightsDao.insertItem(
                    ReadHighlightsEntity(highlights.readIndex, highlights.highlights, deviceHelper.nowEpochMilli())
                )
            }

            safeApiCall(connectivityHelper) {
                lessonsApi.uploadHighlights(highlights)
            }
        }
    }

    override fun getComments(readIndex: String): Flow<Result<SSReadComments>> = readCommentsDao
        .getFlow(readIndex)
        .map { Result.success(SSReadComments(readIndex, it?.comments ?: emptyList())) }
        .onStart { syncComments(readIndex) }
        .flowOn(dispatcherProvider.io)
        .catch {
            Timber.e(it)
            emit(Result.failure(it))
        }

    private fun syncComments(readIndex: String) = launch {
        val response = safeApiCall(connectivityHelper) { lessonsApi.getComments(readIndex) }
        if (response !is NetworkResource.Success) return@launch

        val remote = response.value.body()
        val cached = withContext(dispatcherProvider.io) { readCommentsDao.get(readIndex) }

        remote?.let {
            if (it.timestamp.isAfter(cached?.timestamp)) {
                withContext(dispatcherProvider.io) {
                    readCommentsDao.insertItem(ReadCommentsEntity(readIndex, it.comments, it.timestamp))
                }
            }
        }

        cached?.let {
            if (it.timestamp.isAfter(remote?.timestamp)) {
                safeApiCall(connectivityHelper) {
                    lessonsApi.uploadComments(SSReadComments(readIndex, it.comments))
                }
            }
        }
    }

    override fun saveComments(comments: SSReadComments) {
        launch {
            withContext(dispatcherProvider.io) {
                readCommentsDao.insertItem(
                    ReadCommentsEntity(comments.readIndex, comments.comments, deviceHelper.nowEpochMilli())
                )
            }

            safeApiCall(connectivityHelper) {
                lessonsApi.uploadComments(comments)
            }
        }
    }

    override fun getAnnotations(
        lessonIndex: String,
        pdfId: String
    ): Flow<Result<List<PdfAnnotations>>> = pdfAnnotationsDao
        .getFlow("$lessonIndex-$pdfId")
        .map { entities -> entities.map { PdfAnnotations(pageIndex = it.pageIndex, annotations = it.annotations) } }
        .map { Result.success(it) }
        .onStart { syncAnnotations(lessonIndex, pdfId) }
        .flowOn(dispatcherProvider.io)
        .catch {
            Timber.e(it)
            emit(Result.failure(it))
        }

    private fun syncAnnotations(lessonIndex: String, pdfId: String) = launch {
        val response = safeApiCall(connectivityHelper) { lessonsApi.getPdfAnnotations(lessonIndex, pdfId) }
        if (response !is NetworkResource.Success) return@launch

        val pdfIndex = "$lessonIndex-$pdfId"
        val remote = response.value.body()
        val cached = withContext(dispatcherProvider.io) { pdfAnnotationsDao.get(pdfIndex) }

        remote?.forEach { pdf ->
            val cache = cached.find { it.index == "$pdfIndex-${pdf.pageIndex}" }
            if (cache == null || pdf.timestamp.isAfter(cache.timestamp)) {
                withContext(dispatcherProvider.io) {
                    val entities = pdf.annotations.map {
                        PdfAnnotationsEntity(
                            index = "$pdfIndex-${pdf.pageIndex}",
                            pdfIndex = pdfIndex,
                            pageIndex = pdf.pageIndex,
                            annotations = pdf.annotations,
                            timestamp = pdf.timestamp
                        )
                    }
                    pdfAnnotationsDao.insertAll(entities)
                }
            }
        }
    }

    override fun saveAnnotations(lessonIndex: String, pdfId: String, annotations: List<PdfAnnotations>) {
        launch {
            withContext(dispatcherProvider.io) {
                val pdfIndex = "$lessonIndex-$pdfId"
                val entities = annotations.map {
                    PdfAnnotationsEntity(
                        index = "$pdfIndex-${it.pageIndex}",
                        pdfIndex = pdfIndex,
                        pageIndex = it.pageIndex,
                        annotations = it.annotations,
                        timestamp = deviceHelper.nowEpochMilli()
                    )
                }
                pdfAnnotationsDao.insertAll(entities)
            }

            safeApiCall(connectivityHelper) {
                lessonsApi.uploadAnnotations(
                    lessonIndex,
                    pdfId,
                    UploadPdfAnnotationsRequest(annotations)
                )
            }
        }
    }

    override suspend fun clear() {
        withContext(dispatcherProvider.io) {
            readHighlightsDao.clear()
            readCommentsDao.clear()
            pdfAnnotationsDao.clear()
            ssPrefs.clear()
        }
    }

    private fun Long.isAfter(other: Long?): Boolean {
        other ?: return true
        return Instant.ofEpochMilli(this).isAfter(Instant.ofEpochMilli(other))
    }
}
