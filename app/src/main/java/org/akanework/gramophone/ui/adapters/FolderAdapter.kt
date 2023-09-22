package org.akanework.gramophone.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import java.util.Stack

class FolderAdapter(private val folderList: MutableList<MediaStoreUtils.FileNode>,
                    private val parent: MediaStoreUtils.FileNode,
                    val songAdapter: SongAdapter)
    : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    var parentNode: Stack<MediaStoreUtils.FileNode> = Stack()
    var currentNode: MediaStoreUtils.FileNode = parent

    init {
        parentNode.push(parent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderAdapter.ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_folder_card, parent, false),
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.folderName.text = folderList[holder.bindingAdapterPosition].folderName
        holder.itemView.setOnClickListener {
            val currentNode = folderList[holder.bindingAdapterPosition]
            parentNode.push(currentNode)
            Log.d("STRUCT1", "${parentNode}")
            updateList(currentNode.folderList)
            songAdapter.updateList(currentNode.songList)
        }
    }

    override fun getItemCount(): Int = folderList.size

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val folderName: TextView = view.findViewById(R.id.title)
    }

    fun updateList(newList: MutableList<MediaStoreUtils.FileNode>) {
        val diffResult = DiffUtil.calculateDiff(SongDiffCallback(folderList, newList))
        folderList.clear()
        folderList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class SongDiffCallback(
        private val oldList: MutableList<MediaStoreUtils.FileNode>,
        private val newList: MutableList<MediaStoreUtils.FileNode>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition].folderName == newList[newItemPosition].folderName

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}