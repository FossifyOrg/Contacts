package org.fossify.contacts.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.viewpager.widget.ViewPager
import org.fossify.commons.databinding.BottomTablayoutItemBinding
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.R
import org.fossify.contacts.adapters.ViewPagerAdapter
import org.fossify.contacts.databinding.ActivityInsertEditContactBinding
import org.fossify.contacts.dialogs.ChangeSortingDialog
import org.fossify.contacts.dialogs.FilterContactSourcesDialog
import org.fossify.contacts.extensions.config
import org.fossify.contacts.fragments.MyViewPagerFragment
import org.fossify.contacts.helpers.ADD_NEW_CONTACT_NUMBER
import org.fossify.contacts.helpers.KEY_EMAIL
import org.fossify.contacts.helpers.KEY_NAME
import org.fossify.contacts.interfaces.RefreshContactsListener

class InsertOrEditContactActivity : SimpleActivity(), RefreshContactsListener {
    companion object {
        private const val START_INSERT_ACTIVITY = 1
        private const val START_EDIT_ACTIVITY = 2
    }

    private var isSelectContactIntent = false
    private var specialMimeType: String? = null

    override var isSearchBarEnabled = true

    private val binding by viewBinding(ActivityInsertEditContactBinding::inflate)

    private val contactsFavoritesList = arrayListOf(
        TAB_CONTACTS,
        TAB_FAVORITES
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        isSelectContactIntent = intent.action == Intent.ACTION_PICK
        setupEdgeToEdge(
            padBottomImeAndSystem = listOf(binding.insertEditTabsHolder),
        )

        if (isSelectContactIntent) {
            specialMimeType = when (intent.data) {
                Email.CONTENT_URI -> Email.CONTENT_ITEM_TYPE
                Phone.CONTENT_URI -> Phone.CONTENT_ITEM_TYPE
                else -> null
            }
        }

        binding.newContactHolder.beGoneIf(isSelectContactIntent)
        binding.selectContactLabel.beGoneIf(isSelectContactIntent)

        if (checkAppSideloading()) {
            return
        }

        setupTabs()

        // we do not really care about the permission request result. Even if it was denied, load private contacts
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    handlePermission(PERMISSION_GET_ACCOUNTS) {
                        initFragments()
                    }
                }
            } else {
                initFragments()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateMenuColors()
        setupTabColors()
    }

    private fun setupOptionsMenu() {
        binding.insertEditMenu.requireToolbar().inflateMenu(R.menu.menu_insert_or_edit)
        binding.insertEditMenu.toggleHideOnScroll(false)
        binding.insertEditMenu.setupMenu()

        binding.insertEditMenu.onSearchClosedListener = {
            getAllFragments().forEach {
                it?.onSearchClosed()
            }
        }

        binding.insertEditMenu.onSearchTextChangedListener = { text ->
            getCurrentFragment()?.onSearchQueryChanged(text)
        }

        binding.insertEditMenu.requireToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog()
                R.id.filter -> showFilterDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateMenuColors() {
        binding.insertEditMenu.updateColors()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            hideKeyboard()
            finish()
        }
    }

    override fun onBackPressedCompat(): Boolean {
        return if (binding.insertEditMenu.isSearchOpen) {
            binding.insertEditMenu.closeSearch()
            true
        } else {
            false
        }
    }

    private fun initFragments() {
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                binding.insertEditTabsHolder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
            }
        })

        binding.viewPager.onGlobalLayout {
            refreshContacts(getTabsMask())
        }

        binding.selectContactLabel.setTextColor(getProperPrimaryColor())
        binding.newContactTmb.setImageDrawable(
            resources.getColoredDrawableWithColor(
                org.fossify.commons.R.drawable.ic_add_person_vector,
                getProperTextColor()
            )
        )
        binding.newContactName.setTextColor(getProperTextColor())
        binding.newContactHolder.setOnClickListener {
            createNewContact()
        }
    }

    private fun setupTabs() {
        binding.insertEditTabsHolder.removeAllTabs()
        contactsFavoritesList.forEachIndexed { index, value ->
            if (config.showTabs and value != 0) {
                binding.insertEditTabsHolder.newTab().setCustomView(org.fossify.commons.R.layout.bottom_tablayout_item).apply tab@{
                    customView?.let {
                        BottomTablayoutItemBinding.bind(it)
                    }?.apply {
                        tabItemIcon.setImageDrawable(getTabIcon(index))
                        tabItemLabel.text = getTabLabel(index)
                        binding.insertEditTabsHolder.addTab(this@tab)
                    }
                }
            }
        }

        binding.insertEditTabsHolder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false, getDeselectedTabDrawableIds()[it.position])
            },
            tabSelectedAction = {
                binding.insertEditMenu.closeSearch()
                binding.viewPager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true, getSelectedTabDrawableIds()[it.position])
            }
        )

        binding.insertEditTabsHolder.beGoneIf(binding.insertEditTabsHolder.tabCount == 1)
    }

    private fun getCurrentFragment(): MyViewPagerFragment<*>? {
        return if (binding.viewPager.currentItem == 0) {
            findViewById(R.id.contacts_fragment)
        } else {
            findViewById(R.id.favorites_fragment)
        }
    }

    private fun getAllFragments(): ArrayList<MyViewPagerFragment<*>?> {
        return arrayListOf<MyViewPagerFragment<*>?>(
            findViewById(R.id.contacts_fragment),
            findViewById(R.id.favorites_fragment)
        )
    }

    private fun setupTabColors() {
        val activeView = binding.insertEditTabsHolder.getTabAt(binding.viewPager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true, getSelectedTabDrawableIds()[binding.viewPager.currentItem])

        getInactiveTabIndexes(binding.viewPager.currentItem).forEach { index ->
            val inactiveView = binding.insertEditTabsHolder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false, getDeselectedTabDrawableIds()[index])
        }

        val bottomBarColor = getBottomNavigationBackgroundColor()
        binding.insertEditTabsHolder.setBackgroundColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until binding.insertEditTabsHolder.tabCount).filter { it != activeIndex }

    private fun getSelectedTabDrawableIds() = arrayOf(
        org.fossify.commons.R.drawable.ic_person_vector,
        org.fossify.commons.R.drawable.ic_star_vector
    )

    private fun getDeselectedTabDrawableIds() = arrayOf(
        org.fossify.commons.R.drawable.ic_person_outline_vector,
        org.fossify.commons.R.drawable.ic_star_outline_vector
    )

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isFinishing) {
            return
        }

        if (binding.viewPager.adapter == null) {
            binding.viewPager.adapter = ViewPagerAdapter(this, contactsFavoritesList, getTabsMask())
        }

        ContactsHelper(this).getContacts {
            if (isDestroyed || isFinishing) {
                return@getContacts
            }

            val contacts = it.filter {
                if (specialMimeType != null) {
                    val hasRequiredValues = when (specialMimeType) {
                        Email.CONTENT_ITEM_TYPE -> it.emails.isNotEmpty()
                        Phone.CONTENT_ITEM_TYPE -> it.phoneNumbers.isNotEmpty()
                        else -> true
                    }
                    !it.isPrivate() && hasRequiredValues
                } else {
                    true
                }
            } as ArrayList<Contact>

            val placeholderText = when (specialMimeType) {
                Email.CONTENT_ITEM_TYPE -> getString(R.string.no_contacts_with_emails)
                Phone.CONTENT_ITEM_TYPE -> getString(R.string.no_contacts_with_phone_numbers)
                else -> null
            }

            if (refreshTabsMask and TAB_CONTACTS != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.apply {
                    skipHashComparing = true
                    refreshContacts(contacts, placeholderText)
                }
            }

            if (refreshTabsMask and TAB_FAVORITES != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.favorites_fragment)?.apply {
                    skipHashComparing = true
                    refreshContacts(contacts, placeholderText)
                }
            }
        }
    }

    override fun contactClicked(contact: Contact) {
        hideKeyboard()
        if (isSelectContactIntent) {
            Intent().apply {
                data = getResultUri(contact)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setResult(RESULT_OK, this)
            }
            finish()
        } else {
            val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
            val email = getEmailFromIntent(intent) ?: ""
            Intent(applicationContext, EditContactActivity::class.java).apply {
                data = getContactPublicUri(contact)
                action = ADD_NEW_CONTACT_NUMBER

                if (phoneNumber.isNotEmpty()) {
                    putExtra(KEY_PHONE, phoneNumber)
                }

                if (email.isNotEmpty()) {
                    putExtra(KEY_EMAIL, email)
                }

                putExtra(IS_PRIVATE, contact.isPrivate())
                startActivityForResult(this, START_EDIT_ACTIVITY)
            }
        }
    }

    private fun getResultUri(contact: Contact): Uri {
        return when {
            specialMimeType != null -> {
                val contactId = ContactsHelper(this).getContactMimeTypeId(contact.id.toString(), specialMimeType!!)
                Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, contactId)
            }

            else -> getContactPublicUri(contact)
        }
    }

    private fun createNewContact() {
        val name = intent.getStringExtra(KEY_NAME) ?: ""
        val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
        val email = getEmailFromIntent(intent) ?: ""

        Intent().apply {
            action = Intent.ACTION_INSERT
            data = ContactsContract.Contacts.CONTENT_URI

            if (phoneNumber.isNotEmpty()) {
                putExtra(KEY_PHONE, phoneNumber)
            }

            if (name.isNotEmpty()) {
                putExtra(KEY_NAME, name)
            }

            if (email.isNotEmpty()) {
                putExtra(KEY_EMAIL, email)
            }

            try {
                startActivityForResult(this, START_INSERT_ACTIVITY)
            } catch (e: ActivityNotFoundException) {
                toast(org.fossify.commons.R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            refreshContacts(getTabsMask())
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.forceListRedraw = true
            refreshContacts(getTabsMask())
        }
    }

    private fun getTabsMask(): Int {
        var mask = TAB_CONTACTS
        if (config.showTabs and TAB_FAVORITES != 0) {
            mask += TAB_FAVORITES
        }
        return mask
    }
}
