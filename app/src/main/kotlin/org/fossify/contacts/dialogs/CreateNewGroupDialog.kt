package org.fossify.contacts.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.RadioItem
import org.fossify.commons.models.contacts.ContactSource
import org.fossify.commons.models.contacts.Group
import org.fossify.contacts.R
import org.fossify.contacts.databinding.DialogCreateNewGroupBinding

class CreateNewGroupDialog(val activity: BaseSimpleActivity, val callback: (newGroup: Group) -> Unit) {
    init {
        val binding = DialogCreateNewGroupBinding.inflate(activity.layoutInflater)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.create_new_group) { alertDialog ->
                    alertDialog.showKeyboard(binding.groupName)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = binding.groupName.value
                        if (name.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.empty_name)
                            return@OnClickListener
                        }

                        val contactSources = ArrayList<ContactSource>()
                        ContactsHelper(activity).getContactSources {
                            it.mapTo(contactSources) { ContactSource(it.name, it.type, it.publicName) }

                            val items = ArrayList<RadioItem>()
                            contactSources.forEachIndexed { index, contactSource ->
                                items.add(RadioItem(index, contactSource.publicName))
                            }

                            activity.runOnUiThread {
                                if (items.size == 1) {
                                    createGroupUnder(name, contactSources.first(), alertDialog)
                                } else {
                                    RadioGroupDialog(activity, items, titleId = R.string.create_group_under_account) {
                                        val contactSource = contactSources[it as Int]
                                        createGroupUnder(name, contactSource, alertDialog)
                                    }
                                }
                            }
                        }
                    })
                }
            }
    }

    private fun createGroupUnder(name: String, contactSource: ContactSource, dialog: AlertDialog) {
        ensureBackgroundThread {
            val newGroup = ContactsHelper(activity).createNewGroup(name, contactSource.name, contactSource.type)
            activity.runOnUiThread {
                if (newGroup != null) {
                    callback(newGroup)
                }
                dialog.dismiss()
            }
        }
    }
}
