package org.fossify.musicplayer.activities

import android.os.Bundle
import android.view.Menu
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.musicplayer.R
import org.fossify.musicplayer.adapters.CustomMusicFoldersAdapter
import org.fossify.musicplayer.databinding.ActivityCustomMusicFoldersBinding
import org.fossify.musicplayer.extensions.config

class CustomMusicFoldersActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityCustomMusicFoldersBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.customMusicFoldersList))
        setupMaterialScrollListener(binding.customMusicFoldersList, binding.customMusicFoldersAppbar)
        updateFolders()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.customMusicFoldersAppbar, NavigationIcon.Arrow)
        binding.customMusicFoldersToolbar.menu.apply {
            clear()
            add(Menu.NONE, ADD_FOLDER_MENU_ID, Menu.NONE, R.string.add_folder)
        }
        binding.customMusicFoldersToolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == ADD_FOLDER_MENU_ID) {
                addFolder()
                true
            } else {
                false
            }
        }
    }

    private fun addFolder() {
        FilePickerDialog(this, pickFile = false, enforceStorageRestrictions = false) { path ->
            config.addCustomMusicPath(path)
            updateFolders()
        }
    }

    private fun updateFolders() {
        val folders = config.customMusicPaths.toMutableList() as ArrayList<String>
        binding.customMusicFoldersPlaceholder.apply {
            beVisibleIf(folders.isEmpty())
            setTextColor(getProperTextColor())
        }

        val adapter = CustomMusicFoldersAdapter(this, folders, this, binding.customMusicFoldersList) {}
        binding.customMusicFoldersList.adapter = adapter
    }

    override fun refreshItems() {
        updateFolders()
    }

    companion object {
        private const val ADD_FOLDER_MENU_ID = 1
    }
}
