package org.fossify.contacts.adapters

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getTextSize
import org.fossify.commons.extensions.groupsDB
import org.fossify.commons.extensions.highlightTextPart
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.TAB_GROUPS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.views.MyRecyclerView
import org.fossify.contacts.R
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.databinding.ItemGroupBinding
import org.fossify.contacts.dialogs.RenameGroupDialog
import org.fossify.contacts.extensions.config
import org.fossify.contacts.interfaces.RefreshContactsListener

class GroupsAdapter(
    activity: SimpleActivity, var groups: ArrayList<Group>, val refreshListener: RefreshContactsListener?, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    private var textToHighlight = ""
    var showContactThumbnails = activity.config.showContactThumbnails
    var fontSize = activity.getTextSize()

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_groups

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_rename -> renameGroup()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = groups.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = groups.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = groups.indexOfFirst { it.id!!.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemGroupBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        holder.bindView(group, true, true) { itemView, layoutPosition ->
            setupView(itemView, group)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = groups.size

    private fun getItemWithKey(key: Int): Group? = groups.firstOrNull { it.id!!.toInt() == key }

    fun updateItems(newItems: ArrayList<Group>, highlightText: String = "") {
        if (newItems.hashCode() != groups.hashCode()) {
            groups = newItems
            textToHighlight = highlightText
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun renameGroup() {
        val group = getItemWithKey(selectedKeys.first()) ?: return
        RenameGroupDialog(activity, group) {
            finishActMode()
            refreshListener?.refreshContacts(TAB_GROUPS)
        }
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val firstItem = getSelectedItems().first()
        val items = if (itemsCnt == 1) {
            "\"${firstItem.title}\""
        } else {
            resources.getQuantityString(R.plurals.delete_groups, itemsCnt, itemsCnt)
        }

        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            ensureBackgroundThread {
                deleteGroups()
            }
        }
    }

    private fun deleteGroups() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val groupsToRemove = groups.filter { selectedKeys.contains(it.id!!.toInt()) } as ArrayList<Group>
        val positions = getSelectedItemPositions()
        groupsToRemove.forEach {
            if (it.isPrivateSecretGroup()) {
                activity.groupsDB.deleteGroupId(it.id!!)
            } else {
                ContactsHelper(activity).deleteGroup(it.id!!)
            }
        }
        groups.removeAll(groupsToRemove)

        activity.runOnUiThread {
            if (groups.isEmpty()) {
                refreshListener?.refreshContacts(TAB_GROUPS)
                finishActMode()
            } else {
                removeSelectedItems(positions)
            }
        }
    }

    private fun getSelectedItems() = groups.filter { selectedKeys.contains(it.id?.toInt()) } as ArrayList<Group>

    private fun setupView(view: View, group: Group) {
        ItemGroupBinding.bind(view).apply {
            groupFrame.isSelected = selectedKeys.contains(group.id!!.toInt())
            val titleWithCnt = "${group.title} (${group.contactsCount})"
            val groupTitle = if (textToHighlight.isEmpty()) {
                titleWithCnt
            } else {
                titleWithCnt.highlightTextPart(textToHighlight, properPrimaryColor)
            }

            groupName.apply {
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                text = groupTitle
            }

            groupTmb.beVisibleIf(showContactThumbnails)
            if (showContactThumbnails) {
                groupTmb.setImageDrawable(SimpleContactsHelper(activity).getColoredGroupIcon(group.title))
            }
        }
    }

    override fun onChange(position: Int) = groups.getOrNull(position)?.getBubbleText() ?: ""
}
