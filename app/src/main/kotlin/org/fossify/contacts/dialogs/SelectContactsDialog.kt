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
) : SelectContactsAdapter.SelectionCallback {
    private var dialog: AlertDialog? = null
    private val binding = DialogSelectContactBinding.inflate(activity.layoutInflater)
    private var initiallySelectedContacts = ArrayList<Contact>()
    private var selectedContacts = ArrayList<Contact>()
    private var allContacts = initialContacts

    init {
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

        allContacts = allContacts.sortedBy { contact -> contact.surname + " " + contact.middleName + " " + contact.firstName }.toMutableList() as ArrayList<Contact>

        binding.apply {
            selectContactList.adapter = SelectContactsAdapter(
                activity, allContacts, allContacts, initiallySelectedContacts, allowSelectMultiple,
                selectContactList, contactClickCallback, "", this@SelectContactsDialog // Pass the callback here
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

        setupOptionsMenu()
    }

    override fun onItemSelectionChanged(isSelected: Boolean, pos: Int, id: Int) {
        if (isSelected) {
            val rightId = allContacts.filter { it.id == id } as ArrayList<Contact>
            selectedContacts.add(rightId[0])
        } else {
            selectedContacts = selectedContacts.filter { it.id != id } as ArrayList<Contact>
        }
    }

    private fun setupOptionsMenu() {
        binding.mainMenu.toggleHideOnScroll(false)
        binding.mainMenu.setupMenu()

        val contactClickCallback: ((Contact) -> Unit)? = if (allowSelectMultiple) {
            null
        } else { contact ->
            callback(arrayListOf(contact), arrayListOf())
            dialog!!.dismiss()
        }

        binding.mainMenu.onSearchTextChangedListener = { text ->
            var filteredContacts = ArrayList(allContacts.filter { contact ->
                (contact.firstName + " " + contact.surname + contact.middleName).contains(text, ignoreCase = true)
            })

            filteredContacts = filteredContacts.sortedBy { contact -> contact.surname + " " + contact.middleName + " " + contact.firstName }.toMutableList() as ArrayList<Contact>

            binding.apply {
                selectContactList.adapter = SelectContactsAdapter(
                    activity, filteredContacts, allContacts, selectedContacts, allowSelectMultiple,
                    selectContactList, contactClickCallback, text, this@SelectContactsDialog
                )

                if (root.context.areSystemAnimationsEnabled) {
                    selectContactList.scheduleLayoutAnimation()
                }

                selectContactList.beVisibleIf(filteredContacts.isNotEmpty())
                selectContactPlaceholder.beVisibleIf(filteredContacts.isEmpty())
            }

            setupFastscroller(filteredContacts)

        }
    }

    private fun dialogConfirmed() {
        ensureBackgroundThread {
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
