package org.fossify.contacts.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerThumbView
import com.reddit.indicatorfastscroll.FastScrollerView
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.views.MyFloatingActionButton
import org.fossify.commons.views.MyRecyclerView
import org.fossify.commons.views.MyTextView
import org.fossify.contacts.R
import org.fossify.contacts.activities.GroupContactsActivity
import org.fossify.contacts.activities.InsertOrEditContactActivity
import org.fossify.contacts.activities.MainActivity
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.adapters.ContactsAdapter
import org.fossify.contacts.adapters.GroupsAdapter
import org.fossify.contacts.databinding.FragmentLayoutBinding
import org.fossify.contacts.databinding.FragmentLettersLayoutBinding
import org.fossify.contacts.extensions.config
import org.fossify.contacts.helpers.AVOID_CHANGING_TEXT_TAG
import org.fossify.contacts.helpers.AVOID_CHANGING_VISIBILITY_TAG
import org.fossify.contacts.helpers.Config
import org.fossify.contacts.helpers.GROUP
import org.fossify.contacts.interfaces.RefreshContactsListener
import java.util.Locale

abstract class MyViewPagerFragment<Binding : MyViewPagerFragment.InnerBinding>(context: Context, attributeSet: AttributeSet) :
    CoordinatorLayout(context, attributeSet) {
    protected var activity: SimpleActivity? = null
    protected var allContacts = ArrayList<Contact>()

    private var lastHashCode = 0
    private var contactsIgnoringSearch = listOf<Contact>()
    private var groupsIgnoringSearch = listOf<Group>()
    private lateinit var config: Config
    protected lateinit var innerBinding: Binding

    var skipHashComparing = false
    var forceListRedraw = false

    fun setupFragment(activity: SimpleActivity) {
        config = activity.config
        if (this.activity == null) {
            this.activity = activity
            innerBinding.fragmentFab.beGoneIf(activity is InsertOrEditContactActivity)
            innerBinding.fragmentFab.setOnClickListener {
                fabClicked()
            }

            innerBinding.fragmentPlaceholder2.setOnClickListener {
                placeholderClicked()
            }

            innerBinding.fragmentPlaceholder2.underlineText()

            when (this) {
                is ContactsFragment -> {
                    innerBinding.fragmentFab.contentDescription = activity.getString(org.fossify.commons.R.string.create_new_contact)
                }

                is FavoritesFragment -> {
                    innerBinding.fragmentPlaceholder.text = activity.getString(R.string.no_favorites)
                    innerBinding.fragmentPlaceholder2.text = activity.getString(org.fossify.commons.R.string.add_favorites)
                    innerBinding.fragmentFab.contentDescription = activity.getString(org.fossify.commons.R.string.add_favorites)
                }

                is GroupsFragment -> {
                    innerBinding.fragmentPlaceholder.text = activity.getString(R.string.no_group_created)
                    innerBinding.fragmentPlaceholder2.text = activity.getString(R.string.create_group)
                    innerBinding.fragmentFab.contentDescription = activity.getString(R.string.create_group)
                }
            }
        }
    }

    fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {
        when (this) {
            is GroupsFragment -> (innerBinding.fragmentList.adapter as? GroupsAdapter)?.updateTextColor(textColor)
            else -> (innerBinding.fragmentList.adapter as? ContactsAdapter)?.apply {
                updateTextColor(textColor)
            }
        }

        context.updateTextColors(innerBinding.fragmentWrapper.parent as ViewGroup)
        innerBinding.fragmentFastscroller?.updateColors(adjustedPrimaryColor)
        innerBinding.fragmentPlaceholder2.setTextColor(adjustedPrimaryColor)

        innerBinding.letterFastscroller?.textColor = textColor.getColorStateList()
        innerBinding.letterFastscroller?.pressedTextColor = adjustedPrimaryColor
        innerBinding.letterFastscrollerThumb?.fontSize = context.getTextSize()
        innerBinding.letterFastscrollerThumb?.textColor = adjustedPrimaryColor.getContrastColor()
        innerBinding.letterFastscrollerThumb?.thumbColor = adjustedPrimaryColor.getColorStateList()
    }

    fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
        if (this !is GroupsFragment) {
            (innerBinding.fragmentList.adapter as? ContactsAdapter)?.apply {
                config.sorting = if (startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
                (this@MyViewPagerFragment.activity!! as MainActivity).refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
            }
        }
    }

    fun refreshContacts(contacts: ArrayList<Contact>, placeholderText: String? = null) {
        if ((config.showTabs and TAB_CONTACTS == 0 && this is ContactsFragment && activity !is InsertOrEditContactActivity) ||
            (config.showTabs and TAB_FAVORITES == 0 && this is FavoritesFragment) ||
            (config.showTabs and TAB_GROUPS == 0 && this is GroupsFragment)
        ) {
            return
        }

        if (config.lastUsedContactSource.isEmpty()) {
            val grouped = contacts.groupBy { it.source }.maxWithOrNull(compareBy { it.value.size })
            config.lastUsedContactSource = grouped?.key ?: ""
        }

        allContacts = contacts
        val filtered = when (this) {
            is GroupsFragment -> contacts
            is FavoritesFragment -> {
                val contactSources = activity!!.getVisibleContactSources()
                val favouriteContacts = contacts
                    .filter { it.starred == 1 && contactSources.contains(it.source) }

                if (activity!!.config.isCustomOrderSelected) {
                    sortFavourites(favouriteContacts)
                } else {
                    favouriteContacts
                }
            }

            else -> {
                val contactSources = activity!!.getVisibleContactSources()
                contacts.filter { contactSources.contains(it.source) }
            }
        }

        var currentHash = 0
        filtered.forEach {
            currentHash += it.getHashWithoutPrivatePhoto()
        }

        if (currentHash != lastHashCode || skipHashComparing || filtered.isEmpty()) {
            skipHashComparing = false
            lastHashCode = currentHash
            activity?.runOnUiThread {
                setupContacts(filtered)

                if (placeholderText != null) {
                    innerBinding.fragmentPlaceholder.text = placeholderText
                    innerBinding.fragmentPlaceholder.tag = AVOID_CHANGING_TEXT_TAG
                    innerBinding.fragmentPlaceholder2.beGone()
                    innerBinding.fragmentPlaceholder2.tag = AVOID_CHANGING_VISIBILITY_TAG
                }
            }
        }
    }

    private fun sortFavourites(contacts: List<Contact>): List<Contact> {
        val favoritesOrder = activity?.config?.favoritesContactsOrder
        if (favoritesOrder.isNullOrEmpty()) {
            return contacts
        }

        val orderList = Converters().jsonToStringList(favoritesOrder)
        val map = orderList.withIndex().associate { it.value to it.index }

        return contacts.sortedBy { contact ->
            map[contact.id.toString()]
        }
    }

    private fun setupContacts(contacts: List<Contact>) {
        when (this) {
            is GroupsFragment -> {
                setupGroupsAdapter(contacts) {
                    groupsIgnoringSearch = (innerBinding.fragmentList.adapter as? GroupsAdapter)?.groups ?: ArrayList()
                }
            }

            is FavoritesFragment -> {
                setupContactsFavoritesAdapter(contacts)
                contactsIgnoringSearch = (innerBinding.fragmentList.adapter as? ContactsAdapter)?.contactItems ?: listOf()
                setupLetterFastscroller(contacts)
                innerBinding.letterFastscrollerThumb.setupWithFastScroller(innerBinding.letterFastscroller)
            }

            is ContactsFragment -> {
                setupContactsAdapter(contacts)
                contactsIgnoringSearch = (innerBinding.fragmentList.adapter as? ContactsAdapter)?.contactItems ?: ArrayList()
                setupLetterFastscroller(contacts)
                innerBinding.letterFastscrollerThumb.setupWithFastScroller(innerBinding.letterFastscroller)
            }
        }
    }

    private fun setupGroupsAdapter(contacts: List<Contact>, callback: () -> Unit) {
        ContactsHelper(activity!!).getStoredGroups {
            var storedGroups = it
            contacts.forEach {
                it.groups.forEach {
                    val group = it
                    val storedGroup = storedGroups.firstOrNull { it.id == group.id }
                    storedGroup?.addContact()
                }
            }

            storedGroups = storedGroups.asSequence().sortedWith(compareBy { it.title.lowercase(Locale.getDefault()).normalizeString() })
                .toMutableList() as ArrayList<Group>

            innerBinding.fragmentPlaceholder2.beVisibleIf(storedGroups.isEmpty())
            innerBinding.fragmentPlaceholder.beVisibleIf(storedGroups.isEmpty())
            innerBinding.letterFastscroller?.beVisibleIf(storedGroups.isNotEmpty())

            val currAdapter = innerBinding.fragmentList.adapter
            if (currAdapter == null) {
                GroupsAdapter(activity as SimpleActivity, storedGroups, activity as RefreshContactsListener, innerBinding.fragmentList) {
                    activity?.hideKeyboard()
                    Intent(activity, GroupContactsActivity::class.java).apply {
                        putExtra(GROUP, it as Group)
                        activity!!.startActivity(this)
                    }
                }.apply {
                    innerBinding.fragmentList.adapter = this
                }

                if (context.areSystemAnimationsEnabled) {
                    innerBinding.fragmentList.scheduleLayoutAnimation()
                }
            } else {
                (currAdapter as GroupsAdapter).apply {
                    showContactThumbnails = activity.config.showContactThumbnails
                    updateItems(storedGroups)
                }
            }

            callback()
        }
    }

    fun showContactThumbnailsChanged(showThumbnails: Boolean) {
        if (this is GroupsFragment) {
            (innerBinding.fragmentList.adapter as? GroupsAdapter)?.apply {
                showContactThumbnails = showThumbnails
                notifyDataSetChanged()
            }
        } else {
            (innerBinding.fragmentList.adapter as? ContactsAdapter)?.apply {
                showContactThumbnails = showThumbnails
                notifyDataSetChanged()
            }
        }
    }

    fun setupLetterFastscroller(contacts: List<Contact>) {
        val sorting = context.config.sorting
        innerBinding.letterFastscroller?.setupWithRecyclerView(innerBinding.fragmentList, { position ->
            try {
                val contact = contacts[position]
                var name = when {
                    contact.isABusinessContact() -> contact.getFullCompany()
                    sorting and SORT_BY_SURNAME != 0 && contact.surname.isNotEmpty() -> contact.surname
                    sorting and SORT_BY_MIDDLE_NAME != 0 && contact.middleName.isNotEmpty() -> contact.middleName
                    sorting and SORT_BY_FIRST_NAME != 0 && contact.firstName.isNotEmpty() -> contact.firstName
                    context.config.startNameWithSurname -> contact.surname
                    else -> contact.firstName
                }

                if (name.isEmpty()) {
                    name = contact.getNameToDisplay()
                }

                val character = if (name.isNotEmpty()) name.substring(0, 1) else ""
                FastScrollItemIndicator.Text(character.normalizeString().uppercase(Locale.getDefault()))
            } catch (e: Exception) {
                FastScrollItemIndicator.Text("")
            }
        })
    }

    fun fontSizeChanged() {
        if (this is GroupsFragment) {
            (innerBinding.fragmentList.adapter as? GroupsAdapter)?.apply {
                fontSize = activity.getTextSize()
                notifyDataSetChanged()
            }
        } else {
            (innerBinding.fragmentList.adapter as? ContactsAdapter)?.apply {
                fontSize = activity.getTextSize()
                notifyDataSetChanged()
            }
        }
    }

    fun finishActMode() {
        (innerBinding.fragmentList.adapter as? MyRecyclerViewAdapter)?.finishActMode()
    }

    fun onSearchQueryChanged(text: String) {
        val adapter = innerBinding.fragmentList.adapter
        val fixedText = text.trim().replace("\\s+".toRegex(), " ")
        if (adapter is ContactsAdapter) {
            val shouldNormalize = fixedText.normalizeString() == fixedText
            val filtered = contactsIgnoringSearch.filter {
                getProperText(it.getNameToDisplay(), shouldNormalize).contains(fixedText, true) ||
                    getProperText(it.nickname, shouldNormalize).contains(fixedText, true) ||
                    (fixedText.toLongOrNull() != null && it.phoneNumbers.any {
                        fixedText.normalizePhoneNumber().isNotEmpty() && it.normalizedNumber.contains(fixedText.normalizePhoneNumber(), true)
                    }) ||
                    it.emails.any { it.value.contains(fixedText, true) } ||
                    it.addresses.any { getProperText(it.value, shouldNormalize).contains(fixedText, true) } ||
                    it.IMs.any { it.value.contains(fixedText, true) } ||
                    getProperText(it.notes, shouldNormalize).contains(fixedText, true) ||
                    getProperText(it.organization.company, shouldNormalize).contains(fixedText, true) ||
                    getProperText(it.organization.jobPosition, shouldNormalize).contains(fixedText, true) ||
                    it.websites.any { it.contains(fixedText, true) }
            } as ArrayList

            filtered.sortBy {
                val nameToDisplay = it.getNameToDisplay()
                !getProperText(nameToDisplay, shouldNormalize).startsWith(fixedText, true) && !nameToDisplay.contains(fixedText, true)
            }

            if (filtered.isEmpty() && this@MyViewPagerFragment is FavoritesFragment) {
                if (innerBinding.fragmentPlaceholder.tag != AVOID_CHANGING_TEXT_TAG) {
                    innerBinding.fragmentPlaceholder.text = activity?.getString(org.fossify.commons.R.string.no_contacts_found)
                }
            }

            innerBinding.fragmentPlaceholder.beVisibleIf(filtered.isEmpty())
            adapter.updateItems(filtered, fixedText.normalizeString())
            setupLetterFastscroller(filtered)
        } else if (adapter is GroupsAdapter) {
            val filtered = groupsIgnoringSearch.filter {
                it.title.contains(fixedText, true)
            } as ArrayList

            if (filtered.isEmpty()) {
                innerBinding.fragmentPlaceholder.text = activity?.getString(org.fossify.commons.R.string.no_items_found)
            }

            innerBinding.fragmentPlaceholder.beVisibleIf(filtered.isEmpty())
            adapter.updateItems(filtered, text)
        }
    }

    fun onSearchClosed() {
        if (innerBinding.fragmentList.adapter is ContactsAdapter) {
            (innerBinding.fragmentList.adapter as? ContactsAdapter)?.updateItems(contactsIgnoringSearch)
            setupLetterFastscroller(contactsIgnoringSearch)
            setupViewVisibility(contactsIgnoringSearch.isNotEmpty())
        } else if (innerBinding.fragmentList.adapter is GroupsAdapter) {
            (innerBinding.fragmentList.adapter as? GroupsAdapter)?.updateItems(ArrayList(groupsIgnoringSearch))
            setupViewVisibility(groupsIgnoringSearch.isNotEmpty())
        }

        if (this is FavoritesFragment && innerBinding.fragmentPlaceholder.tag != AVOID_CHANGING_TEXT_TAG) {
            innerBinding.fragmentPlaceholder.text = activity?.getString(R.string.no_favorites)
        }
    }

    fun setupViewVisibility(hasItemsToShow: Boolean) {
        if (innerBinding.fragmentPlaceholder2.tag != AVOID_CHANGING_VISIBILITY_TAG) {
            innerBinding.fragmentPlaceholder2.beVisibleIf(!hasItemsToShow)
        }

        innerBinding.fragmentPlaceholder.beVisibleIf(!hasItemsToShow)
        innerBinding.fragmentList.beVisibleIf(hasItemsToShow)
    }

    abstract fun fabClicked()

    abstract fun placeholderClicked()

    interface InnerBinding {
        val fragmentList: MyRecyclerView
        val fragmentPlaceholder: MyTextView
        val fragmentPlaceholder2: MyTextView
        val fragmentFab: MyFloatingActionButton
        val fragmentWrapper: RelativeLayout
        val letterFastscroller: FastScrollerView?
        val letterFastscrollerThumb: FastScrollerThumbView?
        val fragmentFastscroller: RecyclerViewFastScroller?
    }

    class LetterLayout(val binding: FragmentLettersLayoutBinding) : InnerBinding {
        override val fragmentList: MyRecyclerView = binding.fragmentList
        override val fragmentPlaceholder: MyTextView = binding.fragmentPlaceholder
        override val fragmentPlaceholder2: MyTextView = binding.fragmentPlaceholder2
        override val fragmentFab: MyFloatingActionButton = binding.fragmentFab
        override val fragmentWrapper: RelativeLayout = binding.fragmentWrapper
        override val letterFastscroller: FastScrollerView = binding.letterFastscroller
        override val letterFastscrollerThumb: FastScrollerThumbView = binding.letterFastscrollerThumb
        override val fragmentFastscroller: RecyclerViewFastScroller? = null
    }

    class FragmentLayout(val binding: FragmentLayoutBinding) : InnerBinding {
        override val fragmentList: MyRecyclerView = binding.fragmentList
        override val fragmentPlaceholder: MyTextView = binding.fragmentPlaceholder
        override val fragmentPlaceholder2: MyTextView = binding.fragmentPlaceholder2
        override val fragmentFab: MyFloatingActionButton = binding.fragmentFab
        override val fragmentWrapper: RelativeLayout = binding.fragmentWrapper
        override val letterFastscroller: FastScrollerView? = null
        override val letterFastscrollerThumb: FastScrollerThumbView? = null
        override val fragmentFastscroller: RecyclerViewFastScroller = binding.fragmentFastscroller
    }
}
