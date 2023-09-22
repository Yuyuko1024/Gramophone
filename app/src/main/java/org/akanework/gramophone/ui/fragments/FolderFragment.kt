package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainer
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialSharedAxis
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.adapters.FolderAdapter
import org.akanework.gramophone.ui.adapters.FolderPopAdapter
import org.akanework.gramophone.ui.adapters.GenreAdapter
import org.akanework.gramophone.ui.adapters.GenreDecorAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

class FolderFragment : BaseFragment(false) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var songAdapter: SongAdapter
    private lateinit var decorAdapter: FolderPopAdapter
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_folder, container, false)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)

        if (libraryViewModel.folderStructure.value!!.folderList.isNotEmpty()) {
            songAdapter = SongAdapter(libraryViewModel.folderStructure.value!!.folderList
                .first().folderList
                .first().folderList
                .first().songList,
                requireActivity() as MainActivity)
            folderAdapter = FolderAdapter(libraryViewModel.folderStructure.value!!.folderList
                .first().folderList
                .first().folderList
                .first().folderList,
                libraryViewModel.folderStructure.value!!.folderList
                    .first().folderList
                    .first().folderList
                    .first(),
                songAdapter)
            decorAdapter = FolderPopAdapter(folderAdapter)
        } else {
            songAdapter = SongAdapter(mutableListOf(), requireActivity() as MainActivity)
            folderAdapter = FolderAdapter(mutableListOf(), libraryViewModel.folderStructure.value!!, songAdapter)
            decorAdapter = FolderPopAdapter(folderAdapter)
        }
        concatAdapter = ConcatAdapter(decorAdapter, folderAdapter, songAdapter)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = concatAdapter
        return rootView
    }

}
