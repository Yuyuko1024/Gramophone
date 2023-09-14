package org.akanework.gramophone.ui.fragments

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.adapters.SongDecorAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel


/**
 * [SongFragment] is the default fragment that will show up
 * when you open the application. It displays information
 * about songs.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class SongFragment : BaseFragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val songList: MutableList<MediaItem> = mutableListOf()
    private lateinit var songAdapter: SongAdapter

    private val itemHelperCallback = object : ItemTouchHelper
        .SimpleCallback(0, ItemTouchHelper.RIGHT) {
        private var originalDx: Float = 0f

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            Log.d("TAG22", "${songList[viewHolder.bindingAdapterPosition]}")
            val player = (requireActivity() as MainActivity).getPlayer()
            player.addMediaItem(player.currentMediaItemIndex + 1,
                songList[viewHolder.bindingAdapterPosition])
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val maxSwipeDistance = viewHolder.itemView.width / 3.0f

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                originalDx = dX
            }

            val limitedDX = dX.coerceIn(-maxSwipeDistance, maxSwipeDistance)

            super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive)

            val background = ColorDrawable(
                MaterialColors.getColor(
                    requireView(),
                    com.google.android.material.R.attr.colorPrimary,
                )
            )

            background.setBounds(
                viewHolder.itemView.left,
                viewHolder.itemView.top,
                (viewHolder.itemView.left + limitedDX).toInt(),
                viewHolder.itemView.bottom
            )
            if (isCurrentlyActive) {
                val text = getString(R.string.play_next)
                val textPaint = Paint().apply {
                    color = MaterialColors.getColor(
                        requireView(),
                        com.google.android.material.R.attr.colorOnPrimary,
                    )
                    textSize = 48f
                }
                val textX = viewHolder.itemView.right - originalDx - textPaint.measureText(text) - 16f
                val textY = viewHolder.itemView.top + viewHolder.itemView.height / 2 + textPaint.textSize / 2

                c.drawText(text, textX, textY, textPaint)
                background.draw(c)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_song, container, false)
        val songRecyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        songRecyclerView.layoutManager = LinearLayoutManager(activity)
        songList.addAll(libraryViewModel.mediaItemList.value!!)
        songAdapter = SongAdapter(songList, requireActivity() as MainActivity)
        val songDecorAdapter =
            SongDecorAdapter(
                requireContext(),
                libraryViewModel.mediaItemList.value!!.size,
                songAdapter,
            )
        val concatAdapter = ConcatAdapter(songDecorAdapter, songAdapter)

        if (!libraryViewModel.mediaItemList.hasActiveObservers()) {
            libraryViewModel.mediaItemList.observe(viewLifecycleOwner) { mediaItems ->
                if (mediaItems.isNotEmpty()) {
                    if (mediaItems.size != songList.size || songDecorAdapter.isCounterEmpty()) {
                        songDecorAdapter.updateSongCounter(mediaItems.size)
                    }
                    songAdapter.updateList(mediaItems)
                }
            }
        }

        songRecyclerView.adapter = concatAdapter

        val itemTouchHelper = ItemTouchHelper(itemHelperCallback)
        itemTouchHelper.attachToRecyclerView(songRecyclerView)

        FastScrollerBuilder(songRecyclerView).build()

        return rootView
    }
}
