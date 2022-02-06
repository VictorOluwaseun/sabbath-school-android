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

package app.ss.lessons.data.repository.lessons

import app.ss.lessons.data.api.SSLessonsApi
import app.ss.lessons.data.repository.mediator.DataSource
import app.ss.lessons.data.repository.mediator.DataSourceMediator
import app.ss.lessons.data.repository.mediator.LocalDataSource
import app.ss.models.SSLesson
import app.ss.models.SSLessonInfo
import app.ss.storage.db.dao.LessonsDao
import app.ss.storage.db.entity.LessonEntity
import com.cryart.sabbathschool.core.extensions.coroutines.DispatcherProvider
import com.cryart.sabbathschool.core.response.Resource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LessonInfoDataSource @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val lessonsApi: SSLessonsApi,
    private val lessonsDao: LessonsDao,
) : DataSourceMediator<SSLessonInfo, LessonInfoDataSource.Request>(dispatcherProvider = dispatcherProvider) {

    data class Request(val lessonIndex: String)

    override val cache: LocalDataSource<SSLessonInfo, Request> = object : LocalDataSource<SSLessonInfo, Request> {

        override suspend fun getItem(request: Request): Resource<SSLessonInfo> {
            val info = lessonsDao.get(request.lessonIndex) ?: return Resource.loading()
            return Resource.success(info.toInfoModel())
        }

        override fun updateItem(data: SSLessonInfo) {
            lessonsDao.updateInfo(
                data.lesson.index,
                data.days,
                data.pdfs
            )
        }
    }

    override val network: DataSource<SSLessonInfo, Request> = object : DataSource<SSLessonInfo, Request> {
        override suspend fun getItem(request: Request): Resource<SSLessonInfo> = try {
            val error = Resource.error<SSLessonInfo>(Throwable(""))
            val index = request.lessonIndex

            val language = index.substringBefore('-')
            val lessonId = index.substringAfterLast('-')
            val quarterlyId = index.substringAfter('-')
                .substringBeforeLast('-')

            val response = lessonsApi.getLessonInfo(language, quarterlyId, lessonId)

            response.body()?.let { Resource.success(it) } ?: error
        } catch (error: Throwable) {
            Timber.e(error)
            Resource.error(error)
        }
    }

    private fun LessonEntity.toInfoModel(): SSLessonInfo = SSLessonInfo(
        lesson = SSLesson(
            title = title,
            start_date = start_date,
            end_date = end_date,
            cover = cover,
            id = id,
            index = index,
            path = path,
            full_path = full_path,
            pdfOnly = pdfOnly
        ),
        days = days,
        pdfs = pdfs
    )
}
