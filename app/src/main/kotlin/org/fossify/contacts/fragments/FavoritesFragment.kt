package org.fossify.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.google.gson.Gson
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.helpers.CONTACTS_GRID_MAX_COLUMNS_COUNT
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.TAB_FAVORITES
import org.fossify.commons.helpers.VIEW_TYPE_GRID
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.commons.views.MyLinearLayoutManager
import org.fossify.commons.views.MyRecyclerView
import org.fossify.contacts.activities.MainActivity
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.adapters.ContactsAdapter
import org.fossify.contacts.databinding.FragmentFavoritesBinding
import org.fossify.contacts.databinding.FragmentLettersLayoutBinding
import org.fossify.contacts.dialogs.SelectContactsDialog
import org.fossify.contacts.extensions.config
import org.fossify.contacts.helpers.LOCATION_FAVORITES_TAB
import org.fossify.contacts.interfaces.RefreshContactsListener

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.LetterLayout>(context, attributeSet) {
    private var favouriteContacts = listOf<Contact>()
    private var zoomListener: MyRecyclerView.MyZoomListener? = null
    private lateinit var binding: FragmentFavoritesBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentFavoritesBinding.bind(this)
        innerBinding = LetterLayout(FragmentLettersLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        finishActMode()
        showAddFavoritesDialog()
    }

    override fun placeholderClicked() {
        showAddFavoritesDialog()
    }

    private fun getRecyclerAdapter() = innerBinding.fragmentList.adapter as? ContactsAdapter

    private fun showAddFavoritesDialog() {
        SelectContactsDialog(activity!!, allContacts, true, false) { addedContacts, removedContacts ->
            ContactsHelper(activity as SimpleActivity).apply {
                addFavorites(addedContacts)
                removeFavorites(removedContacts)
            }

            (activity as? MainActivity)?.refreshContacts(TAB_FAVORITES)
        }
    }

    fun setupContactsFavoritesAdapter(contacts: List<Contact>) {
        favouriteContacts = contacts
        setupViewVisibility(favouriteContacts.isNotEmpty())
        val currAdapter = getRecyclerAdapter()

        val viewType = context.config.viewType
        setFavoritesViewType(viewType)
        initZoomListener(viewType)

        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = LOCATION_FAVORITES_TAB

            ContactsAdapter(
                activity = activity as SimpleActivity,
                contactItems = favouriteContacts.toMutableList(),
                refreshListener = activity as RefreshContactsListener,
                location = location,
                viewType = viewType,
                removeListener = null,
                recyclerView = innerBinding.fragmentList,
                enableDrag = true,
            ) {
                (activity as RefreshContactsListener).contactClicked(it as Contact)
            }.apply {
                innerBinding.fragmentList.adapter = this
                setupZoomListener(zoomListener)
                onDragEndListener = {
                    val adapter = innerBinding.fragmentList.adapter
                    if (adapter is ContactsAdapter) {
                        val items = adapter.contactItems
                        saveCustomOrderToPrefs(items)
                        setupLetterFastscroller(items)
                    }
                }
            }

            if (context.areSystemAnimationsEnabled) {
                innerBinding.fragmentList.scheduleLayoutAnimation()
            }
        } else {
            currAdapter.apply {
                startNameWithSurname = context.config.startNameWithSurname
                showPhoneNumbers = context.config.showPhoneNumbers
                showContactThumbnails = context.config.showContactThumbnails
                this.viewType = viewType
                updateItems(favouriteContacts)
            }
        }
    }

    fun updateFavouritesAdapter() {
        setupContactsFavoritesAdapter(favouriteContacts)
    }

    private fun setFavoritesViewType(viewType: Int) {
        val spanCount = context.config.contactsGridColumnCount

        if (viewType == VIEW_TYPE_GRID) {
            innerBinding.letterFastscroller.beGone()
            innerBinding.fragmentList.layoutManager = MyGridLayoutManager(context, spanCount)
        } else {
            innerBinding.letterFastscroller.beVisible()
            innerBinding.fragmentList.layoutManager = MyLinearLayoutManager(context)
        }
    }

    private fun initZoomListener(viewType: Int) {
        if (viewType == VIEW_TYPE_GRID) {
            val layoutManager = innerBinding.fragmentList.layoutManager as MyGridLayoutManager
            zoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < CONTACTS_GRID_MAX_COLUMNS_COUNT) {
                        increaseColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }
            }
        } else {
            zoomListener = null
        }
    }

    private fun increaseColumnCount() {
        if (context.config.viewType == VIEW_TYPE_GRID) {
            context!!.config.contactsGridColumnCount += 1
            columnCountChanged()
        }
    }

    private fun reduceColumnCount() {
        if (context.config.viewType == VIEW_TYPE_GRID) {
            context!!.config.contactsGridColumnCount -= 1
            columnCountChanged()
        }
    }

    fun columnCountChanged() {
        (innerBinding.fragmentList.layoutManager as? MyGridLayoutManager)?.spanCount = context!!.config.contactsGridColumnCount
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, favouriteContacts.size)
        }
    }

    private fun saveCustomOrderToPrefs(items: List<Contact>) {
        activity?.apply {
            val orderIds = items.map { it.id }
            val orderGsonString = Gson().toJson(orderIds)
            config.favoritesContactsOrder = orderGsonString
        }
    }

}
