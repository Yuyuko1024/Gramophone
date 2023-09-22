package org.akanework.gramophone.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.R

class FolderPopAdapter(private val folderAdapter: FolderAdapter)
    : RecyclerView.Adapter<FolderPopAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderPopAdapter.ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.adapter_folder_popup, parent, false),
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            if (folderAdapter.parentNode.size >= 1) {
                val parentNode = folderAdapter.parentNode.first()
                folderAdapter.parentNode.pop()
                Log.d("PARENTNODE", "$parentNode")
                folderAdapter.updateList(parentNode.folderList)
                folderAdapter.songAdapter.updateList(parentNode.songList)
            }
            Log.d("STRUCT", "${folderAdapter.parentNode}")
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view)

}