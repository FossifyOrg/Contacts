package org.fossify.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getVisibleContactSources
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.SMT_PRIVATE
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.ContactSource
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.adapters.FilterContactSourcesAdapter
import org.fossify.contacts.databinding.DialogFilterContactSourcesBinding
import org.fossify.contacts.extensions.config

class FilterContactSourcesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding = DialogFilterContactSourcesBinding.inflate(activity.layoutInflater)
    private var contactSources = ArrayList<ContactSource>()
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        ContactsHelper(activity).getContactSources { contactSources ->
            contactSources.mapTo(this@FilterContactSourcesDialog.contactSources) { it.copy() }
            isContactSourcesReady = true
            processDataIfReady()
        }

        ContactsHelper(activity).getContacts(getAll = true) { contacts ->
            contacts.mapTo(this@FilterContactSourcesDialog.contacts) { it.copy() }
            isContactsReady = true
            processDataIfReady()
        }
    }

    private fun processDataIfReady() {
        if (!isContactSourcesReady) {
            return
        }

        val contactSourcesWithCount = ArrayList<ContactSource>()
        for (contactSource in contactSources) {
            val count = if (isContactsReady) {
                contacts.filter { it.source == contactSource.name }.count()
            } else {
                -1
            }
            contactSourcesWithCount.add(contactSource.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            val selectedSources = activity.getVisibleContactSources()
            binding.filterContactSourcesList.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, selectedSources)

            if (dialog == null) {
                activity.getAlertDialogBuilder()
                    .setPositiveButton(org.fossify.commons.R.string.ok) { dialogInterface, i -> confirmContactSources() }
                    .setNegativeButton(org.fossify.commons.R.string.cancel, null)
                    .apply {
                        activity.setupDialogStuff(binding.root, this) { alertDialog ->
                            dialog = alertDialog
                        }
                    }
            }
        }
    }

    private fun confirmContactSources() {
        val selectedContactSources = (binding.filterContactSourcesList.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
        val ignoredContactSources = contactSources.filter { !selectedContactSources.contains(it) }.map {
            if (it.type == SMT_PRIVATE) SMT_PRIVATE else it.getFullIdentifier()
        }.toHashSet()

        if (activity.getVisibleContactSources() != ignoredContactSources) {
            activity.config.ignoredContactSources = ignoredContactSources
            callback()
        }
        dialog?.dismiss()
    }
}
