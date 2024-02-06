package org.fossify.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.views.MyAppCompatCheckbox
import org.fossify.contacts.R
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.databinding.DialogSelectGroupsBinding
import org.fossify.contacts.databinding.ItemCheckboxBinding
import org.fossify.contacts.databinding.ItemTextviewBinding

class SelectGroupsDialog(val activity: SimpleActivity, val selectedGroups: ArrayList<Group>, val callback: (newGroups: ArrayList<Group>) -> Unit) {
    private val binding = DialogSelectGroupsBinding.inflate(activity.layoutInflater)
    private val checkboxes = ArrayList<MyAppCompatCheckbox>()
    private var groups = ArrayList<Group>()
    private var dialog: AlertDialog? = null

    init {
        ContactsHelper(activity).getStoredGroups {
            groups = it
            activity.runOnUiThread {
                initDialog()
            }
        }
    }

    private fun initDialog() {
        groups.sortedBy { it.title }.forEach {
            addGroupCheckbox(it)
        }

        addCreateNewGroupButton()

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun addGroupCheckbox(group: Group) {
        ItemCheckboxBinding.inflate(activity.layoutInflater, null, false).apply {
            checkboxes.add(itemCheckbox)
            itemCheckboxHolder.setOnClickListener {
                itemCheckbox.toggle()
            }

            itemCheckbox.apply {
                isChecked = selectedGroups.contains(group)
                text = group.title
                tag = group.id
                setColors(activity.getProperTextColor(), activity.getProperPrimaryColor(), activity.getProperBackgroundColor())
            }
            binding.dialogGroupsHolder.addView(this.root)
        }
    }

    private fun addCreateNewGroupButton() {
        val newGroup = Group(0, activity.getString(R.string.create_new_group))
        ItemTextviewBinding.inflate(activity.layoutInflater, null, false).itemTextview.apply {
            text = newGroup.title
            tag = newGroup.id
            setTextColor(activity.getProperTextColor())
            binding.dialogGroupsHolder.addView(this)
            setOnClickListener {
                CreateNewGroupDialog(activity) {
                    selectedGroups.add(it)
                    groups.add(it)
                    binding.dialogGroupsHolder.removeViewAt(binding.dialogGroupsHolder.childCount - 1)
                    addGroupCheckbox(it)
                    addCreateNewGroupButton()
                }
            }
        }
    }

    private fun dialogConfirmed() {
        val selectedGroups = ArrayList<Group>()
        checkboxes.filter { it.isChecked }.forEach {
            val groupId = it.tag as Long
            groups.firstOrNull { it.id == groupId }?.apply {
                selectedGroups.add(this)
            }
        }

        callback(selectedGroups)
    }
}
