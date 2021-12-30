package com.cryart.sabbathschool.lessons.ui.readings

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.ss.lessons.data.model.SSRead
import app.ss.lessons.data.model.SSReadComments
import app.ss.lessons.data.model.SSReadHighlights
import com.cryart.sabbathschool.core.extensions.context.isDarkTheme
import com.cryart.sabbathschool.core.extensions.view.inflate
import com.cryart.sabbathschool.core.model.SSReadingDisplayOptions
import com.cryart.sabbathschool.lessons.R
import com.cryart.sabbathschool.lessons.databinding.SsReadingViewBinding

interface ReadingOptionsCallback : SSReadingView.ContextMenuCallback, SSReadingView.HighlightsCommentsCallback

class ReadingViewPagerAdapter(
    private val optionsCallback: ReadingOptionsCallback
) : RecyclerView.Adapter<ReadingViewHolder>() {

    var readingOptions: SSReadingDisplayOptions? = null

    private var ssReads: List<SSRead> = emptyList()
    private var ssReadHighlights: List<SSReadHighlights> = emptyList()
    private var ssReadComments: List<SSReadComments> = emptyList()

    fun setContent(
        ssReads: List<SSRead>,
        ssReadHighlights: List<SSReadHighlights>,
        ssReadComments: List<SSReadComments>
    ) {
        this.ssReads = ssReads
        this.ssReadHighlights = ssReadHighlights
        this.ssReadComments = ssReadComments
        this.notifyDataSetChanged()
    }

    fun getReadAt(position: Int): SSRead? = ssReads.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingViewHolder =
        ReadingViewHolder.create(parent)

    override fun onBindViewHolder(holder: ReadingViewHolder, position: Int) {
        holder.bind(
            ssReads[position],
            ssReadHighlights[position],
            ssReadComments[position],
            readingOptions ?: defaultDisplayOptions(holder.itemView.context),
            optionsCallback
        )
        holder.itemView.tag = "ssReadingView_$position"
    }

    private fun defaultDisplayOptions(
        context: Context
    ): SSReadingDisplayOptions = SSReadingDisplayOptions(context.isDarkTheme())

    override fun getItemCount(): Int = ssReads.size
}

class ReadingViewHolder(
    private val binding: SsReadingViewBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        read: SSRead,
        highlights: SSReadHighlights,
        comments: SSReadComments,
        readingOptions: SSReadingDisplayOptions,
        optionsCallback: ReadingOptionsCallback
    ) {
        binding.ssReadingView.apply {
            setContextMenuCallback(optionsCallback)
            setHighlightsCommentsCallback(optionsCallback)
            setReadHighlights(highlights)
            setReadComments(comments)
            loadContent(read.content, readingOptions)
        }
    }

    companion object {
        fun create(parent: ViewGroup): ReadingViewHolder = ReadingViewHolder(
            SsReadingViewBinding.bind(parent.inflate(R.layout.ss_reading_view))
        )
    }
}
