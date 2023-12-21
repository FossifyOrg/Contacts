package org.fossify.contacts.adapters

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.databinding.ItemAutocompleteNameNumberBinding

class AutoCompleteTextViewAdapter(
    val activity: SimpleActivity,
    val contacts: ArrayList<Contact>,
    var autoComplete: Boolean = false
) : ArrayAdapter<Contact>(activity, 0, contacts) {
    var resultList = ArrayList<Contact>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = resultList[position]
        var listItem = convertView
        val nameToUse = contact.getNameToDisplay()
        if (listItem == null || listItem.tag != nameToUse.isNotEmpty()) {
            listItem = ItemAutocompleteNameNumberBinding.inflate(activity.layoutInflater, parent, false).root
        }

        val placeholder = BitmapDrawable(activity.resources, SimpleContactsHelper(context).getContactLetterIcon(nameToUse))
        ItemAutocompleteNameNumberBinding.bind(listItem).apply {
            root.setBackgroundColor(context.getProperBackgroundColor())
            itemAutocompleteName.setTextColor(context.getProperTextColor())
            itemAutocompleteNumber.setTextColor(context.getProperTextColor())

            root.tag = nameToUse.isNotEmpty()
            itemAutocompleteName.text = nameToUse
            contact.phoneNumbers.apply {
                val phoneNumber = firstOrNull { it.isPrimary }?.normalizedNumber ?: firstOrNull()?.normalizedNumber
                if (phoneNumber.isNullOrEmpty()) {
                    itemAutocompleteNumber.beGone()
                } else {
                    itemAutocompleteNumber.text = phoneNumber
                }
            }

            val options = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .error(placeholder)
                .centerCrop()

            Glide.with(context)
                .load(contact.photoUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(placeholder)
                .apply(options)
                .apply(RequestOptions.circleCropTransform())
                .into(itemAutocompleteImage)
        }

        return listItem
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            if (constraint != null && autoComplete) {
                val searchString = constraint.toString().normalizeString()
                val results = mutableListOf<Contact>()
                contacts.forEach {
                    if (it.getNameToDisplay().contains(searchString, true)) {
                        results.add(it)
                    }
                }

                results.sortWith(compareBy<Contact>
                { it.name.startsWith(searchString, true) }.thenBy
                { it.name.contains(searchString, true) })
                results.reverse()

                filterResults.values = results
                filterResults.count = results.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                resultList.clear()
                @Suppress("UNCHECKED_CAST")
                resultList.addAll(results.values as List<Contact>)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?) = (resultValue as? Contact)?.name
    }

    override fun getItem(index: Int) = resultList[index]

    override fun getCount() = resultList.size
}
