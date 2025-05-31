package org.fossify.contacts.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getPublicContactSourceSync
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.ContactSource
import org.fossify.contacts.R
import org.fossify.contacts.databinding.DialogChooseContactSourceBinding
import org.fossify.contacts.databinding.ItemRadioButtonBinding

class ChooseContactSourceDialog(val activity: Activity, contacts: ArrayList<Contact>, contactSources: ArrayList<ContactSource>, val callback: (contact: Contact) -> Unit) {
    private lateinit var dialog: AlertDialog

    init {
        val binding = DialogChooseContactSourceBinding.inflate(activity.layoutInflater)
        val sourceContactPairs = ArrayList(contacts.map { contact ->
            var source = activity.getPublicContactSourceSync(contact.source, contactSources)
            if (source == "") {
                source = activity.getString(R.string.phone_storage)
            }
            Pair(source, contact)
        })
        sourceContactPairs.sortBy { it.first }
        sourceContactPairs.forEach { (source, contact) ->
            val item = ItemRadioButtonBinding.inflate(activity.layoutInflater).apply {
                itemRadioButton.text = source
                itemRadioButton.setOnClickListener {
                    callback(contact)
                    dialog.dismiss()
                }
            }

            binding.dialogChooseContactSource.addView(item.root, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val builder = activity.getAlertDialogBuilder()

        builder.apply {
            activity.setupDialogStuff(binding.root, this) { alertDialog ->
                dialog = alertDialog
            }
        }
    }
}
