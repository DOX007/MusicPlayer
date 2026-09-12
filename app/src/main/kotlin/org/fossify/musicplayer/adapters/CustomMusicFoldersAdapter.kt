package org.fossify.musicplayer.adapters

import android.annotation.SuppressLint
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.getPopupMenuTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.humanizePath
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.musicplayer.databinding.ItemExcludedFolderBinding
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.models.Events
import org.greenrobot.eventbus.EventBus

class CustomMusicFoldersAdapter(
    activity: BaseSimpleActivity,
    var folders: ArrayList<String>,
    val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val config = activity.config

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = org.fossify.commons.R.menu.cab_remove_only

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            org.fossify.commons.R.id.cab_remove -> removeSelection()
        }
    }

    override fun getSelectableItemCount() = folders.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = folders.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = folders.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExcludedFolderBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.bindView(folder, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, folder)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = folders.size

    fun updateFolders(folders: ArrayList<String>) {
        this.folders = folders
        notifyDataSetChanged()
    }

    private fun getSelectedItems() = folders.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<String>

    private fun setupView(view: View, folder: String) {
        ItemExcludedFolderBinding.bind(view).apply {
            root.setupViewBackground(activity)
            excludedFolderHolder.isSelected = selectedKeys.contains(folder.hashCode())
            excludedFolderTitle.apply {
                @SuppressLint("SetTextI18n")
                text = context.humanizePath(folder) + "/"
                setTextColor(context.getProperTextColor())
            }

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, folder)
            }
        }
    }

    private fun showPopupMenu(view: View, folder: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val eventTypeId = folder.hashCode()
                when (item.itemId) {
                    org.fossify.commons.R.id.cab_remove -> {
                        executeItemMenuOperation(eventTypeId) {
                            removeSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(eventTypeId: Int, callback: () -> Unit) {
        selectedKeys.clear()
        selectedKeys.add(eventTypeId)
        callback()
    }

    private fun removeSelection() {
        val removeFolders = ArrayList<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            removeFolders.add(it)
            config.removeCustomMusicPath(it)
        }

        folders.removeAll(removeFolders)
        removeSelectedItems(positions)
        EventBus.getDefault().post(Events.RefreshFragments())
        if (folders.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
