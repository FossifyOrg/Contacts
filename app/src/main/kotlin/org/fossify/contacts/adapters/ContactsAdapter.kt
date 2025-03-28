package org.fossify.contacts.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.telephony.PhoneNumberUtils
import android.util.TypedValue
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.interfaces.ItemMoveCallback
import org.fossify.commons.interfaces.ItemTouchHelperContract
import org.fossify.commons.interfaces.StartReorderDragListener
import org.fossify.commons.models.RadioItem
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.views.MyRecyclerView
import org.fossify.contacts.R
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.activities.ViewContactActivity
import org.fossify.contacts.dialogs.CreateNewGroupDialog
import org.fossify.contacts.extensions.config
import org.fossify.contacts.extensions.editContact
import org.fossify.contacts.extensions.shareContacts
import org.fossify.contacts.helpers.*
import org.fossify.contacts.interfaces.RefreshContactsListener
import org.fossify.contacts.interfaces.RemoveFromGroupListener
import java.util.Collections

class ContactsAdapter(
    activity: SimpleActivity,
    var contactItems: MutableList<Contact>,
    recyclerView: MyRecyclerView,
    highlightText: String = "",
    var viewType: Int = VIEW_TYPE_LIST,
    private val refreshListener: RefreshContactsListener?,
    private val location: Int,
    private val removeListener: RemoveFromGroupListener?,
    private val enableDrag: Boolean = false,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate, ItemTouchHelperContract {

    private val NEW_GROUP_ID = -1

    private var config = activity.config
    private var textToHighlight = highlightText

    var startNameWithSurname = config.startNameWithSurname
    var showContactThumbnails = config.showContactThumbnails
    var showPhoneNumbers = config.showPhoneNumbers
    var fontSize = activity.getTextSize()
    var onDragEndListener: (() -> Unit)? = null

    private var touchHelper: ItemTouchHelper? = null
    private var startReorderDragListener: StartReorderDragListener? = null

    init {
        setupDragListener(true)

        if (enableDrag) {
            touchHelper = ItemTouchHelper(ItemMoveCallback(this, viewType == VIEW_TYPE_GRID))
            touchHelper!!.attachToRecyclerView(recyclerView)

            startReorderDragListener = object : StartReorderDragListener {
                override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper?.startDrag(viewHolder)
                }
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
            findItem(R.id.cab_remove).isVisible = location == LOCATION_FAVORITES_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_add_to_favorites).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_add_to_group).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_FAVORITES_TAB
            findItem(R.id.cab_send_sms_to_contacts).isVisible =
                location == LOCATION_CONTACTS_TAB || location == LOCATION_FAVORITES_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_send_email_to_contacts).isVisible =
                location == LOCATION_CONTACTS_TAB || location == LOCATION_FAVORITES_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_delete).isVisible = location == LOCATION_CONTACTS_TAB || location == LOCATION_GROUP_CONTACTS
            findItem(R.id.cab_create_shortcut).isVisible =
                isOreoPlus() && isOneItemSelected() && (location == LOCATION_FAVORITES_TAB || location == LOCATION_CONTACTS_TAB)

            if (location == LOCATION_GROUP_CONTACTS) {
                findItem(R.id.cab_remove).title = activity.getString(R.string.remove_from_group)
            }
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editContact()
            R.id.cab_select_all -> selectAll()
            R.id.cab_add_to_favorites -> addToFavorites()
            R.id.cab_add_to_group -> addToGroup()
            R.id.cab_share -> shareContacts()
            R.id.cab_send_sms_to_contacts -> sendSMSToContacts()
            R.id.cab_send_email_to_contacts -> sendEmailToContacts()
            R.id.cab_create_shortcut -> createShortcut()
            R.id.cab_remove -> removeContacts()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = contactItems.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = contactItems.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = contactItems.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {
        notifyDataSetChanged()
    }

    override fun onActionModeDestroyed() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_GRID -> {
                if (showPhoneNumbers) org.fossify.commons.R.layout.item_contact_with_number_grid else org.fossify.commons.R.layout.item_contact_without_number_grid
            }

            else -> {
                if (showPhoneNumbers) org.fossify.commons.R.layout.item_contact_with_number else org.fossify.commons.R.layout.item_contact_without_number
            }
        }

        return createViewHolder(layout, parent)
    }

    override fun getItemViewType(position: Int): Int {
        return viewType
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contactItems[position]
        val allowLongClick = location != LOCATION_INSERT_OR_EDIT
        holder.bindView(contact, true, allowLongClick) { itemView, layoutPosition ->
            setupView(itemView, contact, holder)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = contactItems.size

    private fun getItemWithKey(key: Int): Contact? = contactItems.firstOrNull { it.id == key }

    fun updateItems(newItems: List<Contact>, highlightText: String = "") {
        if (newItems.hashCode() != contactItems.hashCode()) {
            contactItems = newItems.toMutableList()
            textToHighlight = highlightText
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun editContact() {
        val contact = getItemWithKey(selectedKeys.first()) ?: return
        activity.editContact(contact, config.mergeDuplicateContacts)
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val items = if (itemsCnt == 1) {
            "\"${getSelectedItems().first().getNameToDisplay()}\""
        } else {
            resources.getQuantityString(org.fossify.commons.R.plurals.delete_contacts, itemsCnt, itemsCnt)
        }

        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            deleteContacts()
        }
    }

    private fun deleteContacts() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val contactsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions()
        contactItems.removeAll(contactsToRemove)

        ContactsHelper(activity).getContacts(true) { allContacts ->
            ensureBackgroundThread {
                ContactsHelper(activity).deleteContacts(contactsToRemove
                    .flatMap { contactToRemove -> allContacts.filter {
                        (config.mergeDuplicateContacts || it.id == contactToRemove.id) && (it.getHashToCompare() == contactToRemove.getHashToCompare())
                    } }
                    .toMutableList() as ArrayList<Contact>)

                activity.runOnUiThread {
                    if (contactItems.isEmpty()) {
                        refreshListener?.refreshContacts(ALL_TABS_MASK)
                        finishActMode()
                    } else {
                        removeSelectedItems(positions)
                        refreshListener?.refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
                    }
                }
            }
        }
    }

    // used for removing contacts from groups or favorites, not deleting actual contacts
    private fun removeContacts() {
        val contactsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions()
        contactItems.removeAll(contactsToRemove)

        if (location == LOCATION_FAVORITES_TAB) {
            ContactsHelper(activity).removeFavorites(contactsToRemove)
            if (contactItems.isEmpty()) {
                refreshListener?.refreshContacts(TAB_FAVORITES)
                finishActMode()
            } else {
                removeSelectedItems(positions)
            }
        } else if (location == LOCATION_GROUP_CONTACTS) {
            removeListener?.removeFromGroup(contactsToRemove)
            removeSelectedItems(positions)
        }
    }

    private fun addToFavorites() {
        ContactsHelper(activity).addFavorites(getSelectedItems())
        refreshListener?.refreshContacts(TAB_FAVORITES)
        finishActMode()
    }

    private fun addToGroup() {
        val items = ArrayList<RadioItem>()
        ContactsHelper(activity).getStoredGroups {
            it.forEach {
                items.add(RadioItem(it.id!!.toInt(), it.title))
            }
            items.add(RadioItem(NEW_GROUP_ID, activity.getString(R.string.create_new_group)))
            showGroupsPicker(items)
        }
    }

    private fun showGroupsPicker(radioItems: ArrayList<RadioItem>) {
        val selectedContacts = getSelectedItems()
        RadioGroupDialog(activity, radioItems, 0) {
            if (it as Int == NEW_GROUP_ID) {
                CreateNewGroupDialog(activity) {
                    ensureBackgroundThread {
                        activity.addContactsToGroup(selectedContacts, it.id!!.toLong())
                        refreshListener?.refreshContacts(TAB_GROUPS)
                    }
                    finishActMode()
                }
            } else {
                ensureBackgroundThread {
                    activity.addContactsToGroup(selectedContacts, it.toLong())
                    refreshListener?.refreshContacts(TAB_GROUPS)
                }
                finishActMode()
            }
        }
    }

    private fun shareContacts() {
        activity.shareContacts(getSelectedItems())
    }

    private fun sendSMSToContacts() {
        activity.sendSMSToContacts(getSelectedItems())
    }

    private fun sendEmailToContacts() {
        activity.sendEmailToContacts(getSelectedItems())
    }

    @SuppressLint("NewApi")
    private fun createShortcut() {
        val manager = activity.getSystemService(ShortcutManager::class.java)
        if (manager.isRequestPinShortcutSupported) {
            val contact = getSelectedItems().first()
            val drawable = resources.getDrawable(R.drawable.shortcut_contact).mutate()
            getShortcutImage(contact, drawable) {
                val intent = Intent(activity, ViewContactActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.putExtra(CONTACT_ID, contact.id)
                intent.putExtra(IS_PRIVATE, contact.isPrivate())
                intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY

                val shortcut = ShortcutInfo.Builder(activity, contact.hashCode().toString())
                    .setShortLabel(contact.getNameToDisplay())
                    .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                    .setIntent(intent)
                    .build()

                manager.requestPinShortcut(shortcut, null)
            }
        }
    }

    private fun getShortcutImage(contact: Contact, drawable: Drawable, callback: () -> Unit) {
        val appIconColor = baseConfig.appIconColor
        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_contact_background).applyColorFilter(appIconColor)
        val placeholderImage = BitmapDrawable(resources, SimpleContactsHelper(activity).getContactLetterIcon(contact.getNameToDisplay()))
        if (contact.photoUri.isEmpty() && contact.photo == null) {
            drawable.setDrawableByLayerId(R.id.shortcut_contact_image, placeholderImage)
            callback()
        } else {
            ensureBackgroundThread {
                val options = RequestOptions()
                    .signature(ObjectKey(contact.getSignatureKey()))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .error(placeholderImage)

                val size = activity.resources.getDimension(org.fossify.commons.R.dimen.shortcut_size).toInt()
                val itemToLoad: Any? = if (contact.photoUri.isNotEmpty()) {
                    contact.photoUri
                } else {
                    contact.photo
                }

                val builder = Glide.with(activity)
                    .asDrawable()
                    .load(itemToLoad)
                    .apply(options)
                    .apply(RequestOptions.circleCropTransform())
                    .into(size, size)

                try {
                    val bitmap = builder.get()
                    drawable.setDrawableByLayerId(R.id.shortcut_contact_image, bitmap)
                } catch (e: Exception) {
                }

                activity.runOnUiThread {
                    callback()
                }
            }
        }
    }

    private fun getSelectedItems() = contactItems.filter { selectedKeys.contains(it.id) } as ArrayList<Contact>

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.findViewById<ImageView>(org.fossify.commons.R.id.item_contact_image))
        }
    }

    private fun setupView(view: View, contact: Contact, holder: ViewHolder) {
        view.apply {
            setupViewBackground(activity)
            findViewById<ConstraintLayout>(org.fossify.commons.R.id.item_contact_frame)?.isSelected = selectedKeys.contains(contact.id)
            val fullName = contact.getNameToDisplay()
            findViewById<TextView>(org.fossify.commons.R.id.item_contact_name).text = if (textToHighlight.isEmpty()) fullName else {
                if (fullName.contains(textToHighlight, true)) {
                    fullName.highlightTextPart(textToHighlight, properPrimaryColor)
                } else {
                    fullName.highlightTextFromNumbers(textToHighlight, properPrimaryColor)
                }
            }

            findViewById<TextView>(org.fossify.commons.R.id.item_contact_name).apply {
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            }

            if (findViewById<TextView>(org.fossify.commons.R.id.item_contact_number) != null) {
                val phoneNumberToUse = if (textToHighlight.isEmpty()) {
                    contact.phoneNumbers.firstOrNull()
                } else {
                    contact.phoneNumbers.firstOrNull { it.value.contains(textToHighlight) } ?: contact.phoneNumbers.firstOrNull()
                }
                val phoneNumberToFormat = phoneNumberToUse?.value ?: ""
                val numberText = if (config.formatPhoneNumbers) {
                    phoneNumberToFormat.formatPhoneNumber()
                } else {
                    phoneNumberToUse?.value ?: ""
                }
                findViewById<TextView>(org.fossify.commons.R.id.item_contact_number).apply {
                    text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, properPrimaryColor, false, true)
                    setTextColor(textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                }
            }

            findViewById<TextView>(org.fossify.commons.R.id.item_contact_image).beVisibleIf(showContactThumbnails)

            if (showContactThumbnails) {
                val placeholderImage = BitmapDrawable(resources, SimpleContactsHelper(context).getContactLetterIcon(fullName))
                if (contact.photoUri.isEmpty() && contact.photo == null) {
                    findViewById<ImageView>(org.fossify.commons.R.id.item_contact_image).setImageDrawable(placeholderImage)
                } else {
                    val options = RequestOptions()
                        .signature(ObjectKey(contact.getSignatureKey()))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(placeholderImage)
                        .centerCrop()

                    val itemToLoad: Any? = if (contact.photoUri.isNotEmpty()) {
                        contact.photoUri
                    } else {
                        contact.photo
                    }

                    Glide.with(activity)
                        .load(itemToLoad)
                        .apply(options)
                        .apply(RequestOptions.circleCropTransform())
                        .into(findViewById(org.fossify.commons.R.id.item_contact_image))
                }
            }

            val dragIcon = findViewById<ImageView>(org.fossify.commons.R.id.drag_handle_icon)
            if (enableDrag && textToHighlight.isEmpty()) {
                dragIcon.apply {
                    beVisibleIf(selectedKeys.isNotEmpty())
                    applyColorFilter(textColor)
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            startReorderDragListener?.requestDrag(holder)
                        }
                        false
                    }
                }
            } else {
                dragIcon.apply {
                    beGone()
                    setOnTouchListener(null)
                }
            }
        }
    }

    override fun onChange(position: Int) = contactItems.getOrNull(position)?.getBubbleText() ?: ""

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        activity.config.isCustomOrderSelected = true

        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(contactItems, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(contactItems, i, i - 1)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {}

    override fun onRowClear(myViewHolder: ViewHolder?) {
        onDragEndListener?.invoke()
    }
}
