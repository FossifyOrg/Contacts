package org.fossify.contacts.adapters

import android.graphics.drawable.BitmapDrawable
import android.util.SparseArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.views.MyAppCompatCheckbox
import org.fossify.commons.views.MyRecyclerView
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.databinding.ItemAddFavoriteWithNumberBinding
import org.fossify.contacts.databinding.ItemAddFavoriteWithoutNumberBinding
import org.fossify.contacts.extensions.config

class SelectContactsAdapter(
    val activity: SimpleActivity, var contacts: ArrayList<Contact>, private val selectedContacts: ArrayList<Contact>, private val allowPickMultiple: Boolean,
    recyclerView: MyRecyclerView, private val itemClick: ((Contact) -> Unit)? = null
) :
    RecyclerView.Adapter<SelectContactsAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()
    private val config = activity.config
    private val adjustedPrimaryColor = activity.getProperPrimaryColor()
    private val fontSize = activity.getTextSize()

    private val showContactThumbnails = config.showContactThumbnails
    private val showPhoneNumbers = config.showPhoneNumbers
    private val itemBindingClass = if (showPhoneNumbers) Binding.WithNumber else Binding.WithoutNumber
    private var textToHighlight = ""

    init {
        contacts.forEachIndexed { index, contact ->
            if (selectedContacts.asSequence().map { it.id }.contains(contact.id)) {
                selectedPositions.add(index)
            }
        }

        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemBindingClass.bind(itemViews[pos]).contactCheckbox.isChecked = select
    }

    fun getSelectedItemsSet(): HashSet<Contact> {
        val selectedItemsSet = HashSet<Contact>(selectedPositions.size)
        selectedPositions.forEach { selectedItemsSet.add(contacts[it]) }
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = itemBindingClass.inflate(activity.layoutInflater, parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        itemViews.put(position, holder.bindView(contact))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = contacts.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(itemBindingClass.bind(holder.itemView).contactTmb)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(contact: Contact): View {
            itemBindingClass.bind(itemView).apply {
                contactCheckbox.beVisibleIf(allowPickMultiple)
                contactCheckbox.setColors(root.context.getProperTextColor(), root.context.getProperPrimaryColor(), root.context.getProperBackgroundColor())
                val textColor = root.context.getProperTextColor()

                val fullName = contact.getNameToDisplay()
                contactName.text = if (textToHighlight.isEmpty()) fullName else {
                    if (fullName.contains(textToHighlight, true)) {
                        fullName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                    } else {
                        fullName.highlightTextFromNumbers(textToHighlight, adjustedPrimaryColor)
                    }
                }

                contactName.setTextColor(textColor)
                contactName.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                contactNumber?.apply {
                    val phoneNumberToUse = if (textToHighlight.isEmpty()) {
                        contact.phoneNumbers.firstOrNull()
                    } else {
                        contact.phoneNumbers.firstOrNull { it.value.contains(textToHighlight) } ?: contact.phoneNumbers.firstOrNull()
                    }

                    val numberText = phoneNumberToUse?.value ?: ""
                    text = if (textToHighlight.isEmpty()) numberText else numberText.highlightTextPart(textToHighlight, adjustedPrimaryColor, false, true)
                    setTextColor(textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                }

                root.setOnClickListener {
                    if (itemClick != null) {
                        itemClick.invoke(contact)
                    } else {
                        viewClicked(!contactCheckbox.isChecked)
                    }
                }

                contactTmb.beVisibleIf(showContactThumbnails)

                if (showContactThumbnails) {
                    val avatarName = when {
                        contact.isABusinessContact() -> contact.getFullCompany()
                        config.startNameWithSurname -> contact.surname
                        else -> contact.firstName
                    }

                    val placeholderImage = BitmapDrawable(root.resources, SimpleContactsHelper(root.context).getContactLetterIcon(avatarName))

                    if (contact.photoUri.isEmpty() && contact.photo == null) {
                        contactTmb.setImageDrawable(placeholderImage)
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
                            .into(contactTmb)
                    }
                }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean) {
            toggleItemSelection(select, adapterPosition)
        }
    }

    private sealed interface Binding {

        fun inflate(layoutInflater: LayoutInflater, viewGroup: ViewGroup, attachToRoot: Boolean): ItemAddFavoriteBinding
        fun bind(view: View): ItemAddFavoriteBinding


        data object WithNumber : Binding {
            override fun inflate(layoutInflater: LayoutInflater, viewGroup: ViewGroup, attachToRoot: Boolean): ItemAddFavoriteBinding {
                return ItemAddFavoriteWithNumberBindingAdapter(ItemAddFavoriteWithNumberBinding.inflate(layoutInflater, viewGroup, attachToRoot))
            }

            override fun bind(view: View): ItemAddFavoriteBinding {
                return ItemAddFavoriteWithNumberBindingAdapter(ItemAddFavoriteWithNumberBinding.bind(view))
            }
        }

        data object WithoutNumber : Binding {
            override fun inflate(layoutInflater: LayoutInflater, viewGroup: ViewGroup, attachToRoot: Boolean): ItemAddFavoriteBinding {
                return ItemAddFavoriteWithoutNumberBindingAdapter(ItemAddFavoriteWithoutNumberBinding.inflate(layoutInflater, viewGroup, attachToRoot))
            }

            override fun bind(view: View): ItemAddFavoriteBinding {
                return ItemAddFavoriteWithoutNumberBindingAdapter(ItemAddFavoriteWithoutNumberBinding.bind(view))
            }
        }
    }

    private interface ItemAddFavoriteBinding : ViewBinding {
        val contactName: TextView
        val contactNumber: TextView?
        val contactTmb: ImageView
        val contactCheckbox: MyAppCompatCheckbox
    }

    private class ItemAddFavoriteWithoutNumberBindingAdapter(val binding: ItemAddFavoriteWithoutNumberBinding) : ItemAddFavoriteBinding {
        override val contactName: TextView = binding.contactName
        override val contactNumber: TextView? = null
        override val contactTmb: ImageView = binding.contactTmb
        override val contactCheckbox: MyAppCompatCheckbox = binding.contactCheckbox
        override fun getRoot(): View = binding.root
    }

    private class ItemAddFavoriteWithNumberBindingAdapter(val binding: ItemAddFavoriteWithNumberBinding) : ItemAddFavoriteBinding {
        override val contactName: TextView = binding.contactName
        override val contactNumber: TextView = binding.contactNumber
        override val contactTmb: ImageView = binding.contactTmb
        override val contactCheckbox: MyAppCompatCheckbox = binding.contactCheckbox
        override fun getRoot(): View = binding.root
    }
}
