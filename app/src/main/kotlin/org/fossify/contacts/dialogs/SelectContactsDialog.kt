package org.fossify.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.adapters.SelectContactsAdapter
import org.fossify.contacts.databinding.DialogSelectContactBinding
import java.util.Locale

class SelectContactsDialog(
    val activity: SimpleActivity, initialContacts: ArrayList<Contact>, val allowSelectMultiple: Boolean, val showOnlyContactsWithNumber: Boolean,
    selectContacts: ArrayList<Contact>? = null, val callback: (addedContacts: ArrayList<Contact>, removedContacts: ArrayList<Contact>) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding = DialogSelectContactBinding.inflate(activity.layoutInflater)
    private var initiallySelectedContacts = ArrayList<Contact>()

    init {
        var allContacts = initialContacts
        if (selectContacts == null) {
            val contactSources = activity.getVisibleContactSources()
            allContacts = allContacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>

            if (showOnlyContactsWithNumber) {
                allContacts = allContacts.filter { it.phoneNumbers.isNotEmpty() }.toMutableList() as ArrayList<Contact>
            }

            initiallySelectedContacts = allContacts.filter { it.starred == 1 } as ArrayList<Contact>
        } else {
            initiallySelectedContacts = selectContacts
        }

        // if selecting multiple contacts is disabled, react on first contact click and dismiss the dialog
        val contactClickCallback: ((Contact) -> Unit)? = if (allowSelectMultiple) {
            null
        } else { contact ->
            callback(arrayListOf(contact), arrayListOf())
            dialog!!.dismiss()
        }

        binding.apply {
            selectContactList.adapter = SelectContactsAdapter(
                activity, allContacts, initiallySelectedContacts, allowSelectMultiple,
                selectContactList, contactClickCallback
            )

            if (root.context.areSystemAnimationsEnabled) {
                selectContactList.scheduleLayoutAnimation()
            }

            selectContactList.beVisibleIf(allContacts.isNotEmpty())
            selectContactPlaceholder.beVisibleIf(allContacts.isEmpty())
        }

        setupFastscroller(allContacts)

        val builder = activity.getAlertDialogBuilder()
        if (allowSelectMultiple) {
            builder.setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
        }
        builder.setNegativeButton(org.fossify.commons.R.string.cancel, null)

        builder.apply {
            activity.setupDialogStuff(binding.root, this) { alertDialog ->
                dialog = alertDialog
            }
        }
    }

    private fun dialogConfirmed() {
        ensureBackgroundThread {
            val adapter = binding.selectContactList.adapter as? SelectContactsAdapter
            val selectedContacts = adapter?.getSelectedItemsSet()?.toList() ?: ArrayList()

            val newlySelectedContacts = selectedContacts.filter { !initiallySelectedContacts.contains(it) } as ArrayList
            val unselectedContacts = initiallySelectedContacts.filter { !selectedContacts.contains(it) } as ArrayList
            callback(newlySelectedContacts, unselectedContacts)
        }
    }

    private fun setupFastscroller(allContacts: ArrayList<Contact>) {
        val adjustedPrimaryColor = activity.getProperPrimaryColor()
        binding.apply {
            letterFastscroller.textColor = root.context.getProperTextColor().getColorStateList()
            letterFastscroller.pressedTextColor = adjustedPrimaryColor
            letterFastscrollerThumb.fontSize = root.context.getTextSize()
            letterFastscrollerThumb.textColor = adjustedPrimaryColor.getContrastColor()
            letterFastscrollerThumb.thumbColor = adjustedPrimaryColor.getColorStateList()
            letterFastscrollerThumb.setupWithFastScroller(letterFastscroller)
        }

        binding.letterFastscroller.setupWithRecyclerView(binding.selectContactList, { position ->
            try {
                val name = allContacts[position].getNameToDisplay()
                val character = if (name.isNotEmpty()) name.substring(0, 1) else ""
                FastScrollItemIndicator.Text(character.normalizeString().uppercase(Locale.getDefault()))
            } catch (e: Exception) {
                FastScrollItemIndicator.Text("")
            }
        })
    }
}
