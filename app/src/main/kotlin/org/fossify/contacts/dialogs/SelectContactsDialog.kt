package org.fossify.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.R
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

        binding.apply {
            selectContactList.adapter = SelectContactsAdapter(
                activity, allContacts, allContacts, initiallySelectedContacts, allowSelectMultiple,
                selectContactList, contactClickCallback, ""
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

    private fun setupOptionsMenu() {
        binding.mainMenu.getToolbar().inflateMenu(R.menu.menu)
        binding.mainMenu.toggleHideOnScroll(false)
        binding.mainMenu.setupMenu()

        val contactClickCallback: ((Contact) -> Unit)? = if (allowSelectMultiple) {
            null
        } else { contact ->
            callback(arrayListOf(contact), arrayListOf())
            dialog!!.dismiss()
        }

        binding.mainMenu.onSearchTextChangedListener = { text ->
            val filteredContacts = ArrayList(allContacts.filter { contact ->
                (contact.firstName + " " + contact.surname + contact.middleName).contains(text, ignoreCase = true)
            })

            binding.apply {
                val adapter = binding.selectContactList.adapter as? SelectContactsAdapter

                if (adapter != null) {
                    selectedContacts.addAll(
                        adapter.getSelectedItemsSet()?.toList()?.filterNot {
                            it in selectedContacts
                        }.orEmpty()
                    )
                }

                selectContactList.adapter = SelectContactsAdapter(
                    activity, filteredContacts, allContacts, selectedContacts, allowSelectMultiple,
                    selectContactList, contactClickCallback, text
                )


                if (root.context.areSystemAnimationsEnabled) {
                    selectContactList.scheduleLayoutAnimation()
                }

                selectContactList.beVisibleIf(filteredContacts.isNotEmpty())
                selectContactPlaceholder.beVisibleIf(filteredContacts.isEmpty())
            }

            setupFastscroller(filteredContacts)

        }

        binding.mainMenu.getToolbar().menu.apply {
            findItem(R.id.sort).isVisible = true
            findItem(R.id.filter).isVisible = true
            findItem(R.id.dialpad).isVisible = false
            findItem(R.id.settings).isVisible = false
            findItem(R.id.change_view_type).isVisible = false
            findItem(R.id.column_count).isVisible = false
            findItem(R.id.more_apps_from_us).isVisible = false
            findItem(R.id.about).isVisible = false
        }

        binding.mainMenu.getToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog(showCustomSorting = false)
                R.id.filter -> showFilterDialog()
//                R.id.dialpad -> launchDialpad()
//                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
//                R.id.change_view_type -> changeViewType()
//                R.id.column_count -> changeColumnCount()
//                R.id.settings -> launchSettings()
//                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun showSortingDialog(showCustomSorting: Boolean) {
        ChangeSortingDialog(activity, showCustomSorting) {
            refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
        }
        var dialog: ChangeSortingDialog? = null
        dialog = ChangeSortingDialog(activity, showCustomSorting = showCustomSorting) {

            dialog?.getCurrSorting()?.let { currSorting ->
                println("Current sorting value: $currSorting")
                var sorting = dialog?.getCurrSorting()
                if (sorting == SORT_BY_FIRST_NAME) {
                    val sortedContacts = allContacts.sortedWith(compareBy({ it.firstName == "" }, { it.firstName?.firstOrNull()?.isLetter() == false }, { it.firstName }))
                    refreshContacts(ArrayList(sortedContacts))

//                println("Prénom")
//                for (contact in sortedContacts) {
//                    println("${contact.firstName} : ${contact.surname}")
//                }

                }
                if (sorting == SORT_BY_MIDDLE_NAME) {
                    val sortedContacts = allContacts.sortedWith(compareBy({ it.middleName == "" }, { it.middleName?.firstOrNull()?.isLetter() == false }, { it.middleName }))
                    refreshContacts(ArrayList(sortedContacts))
//                println("Nom")
//                for (contact in sortedContacts) {
//                    println("${contact.firstName} : ${contact.middleName}")
//                }
                }
                if (sorting == SORT_BY_SURNAME) {
                    val sortedContacts = allContacts.sortedWith(compareBy({ it.surname == "" }, { it.surname?.firstOrNull()?.isLetter() == false }, { it.surname }))
                    refreshContacts(ArrayList(sortedContacts))
//                println("Surnom")
                }
                if (sorting == SORT_BY_FULL_NAME) {
                    var sortedContacts = allContacts.sortedBy { it.firstName }
                }
//            println(sorting)
            }
        }
        println("Current sorting value: ${dialog!!.getCurrSorting()}")
        println("changed 2")

 */
        ChangeSortingDialog(activity, showCustomSorting, {}) { updatedSorting ->
            println("Updated sorting: $updatedSorting")
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this.activity) {
            refreshContacts(allContacts)
        }
    }
    fun refreshContacts(refreshTabsMask: ArrayList<Contact>) {
        val contactClickCallback: ((Contact) -> Unit)? = if (allowSelectMultiple) {
            null
        } else { contact ->
            callback(arrayListOf(contact), arrayListOf())
            dialog!!.dismiss()
        }

        binding.apply {
            val adapter = binding.selectContactList.adapter as? SelectContactsAdapter

            if (adapter != null) {
                selectedContacts.addAll(
                    adapter.getSelectedItemsSet()?.toList()?.filterNot {
                        it in selectedContacts
                    }.orEmpty()
                )
            }

            selectContactList.adapter = SelectContactsAdapter(
                activity, refreshTabsMask, allContacts, selectedContacts, allowSelectMultiple,
                selectContactList, contactClickCallback, ""
            )


            if (root.context.areSystemAnimationsEnabled) {
                selectContactList.scheduleLayoutAnimation()
            }

            selectContactList.beVisibleIf(refreshTabsMask.isNotEmpty())
            selectContactPlaceholder.beVisibleIf(refreshTabsMask.isEmpty())
        }

        setupFastscroller(refreshTabsMask)
    }

    private fun dialogConfirmed() {
        ensureBackgroundThread {
            val adapter = binding.selectContactList.adapter as? SelectContactsAdapter

            if (adapter != null) {
                selectedContacts.addAll(
                    adapter.getSelectedItemsSet()?.toList()?.filterNot {
                        it in selectedContacts
                    }.orEmpty()
                )
            }

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
