package org.fossify.contacts.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import ezvcard.VCardVersion
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.RadioItem
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.BuildConfig
import org.fossify.contacts.R
import org.fossify.contacts.activities.EditContactActivity
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.activities.ViewContactActivity
import org.fossify.contacts.dialogs.ImportContactsDialog
import org.fossify.contacts.helpers.DEFAULT_FILE_NAME
import org.fossify.contacts.helpers.VcfExporter
import java.io.FileOutputStream

fun SimpleActivity.startCallIntent(recipient: String) {
    handlePermission(PERMISSION_CALL_PHONE) {
        val action = if (it) Intent.ACTION_CALL else Intent.ACTION_DIAL
        Intent(action).apply {
            data = Uri.fromParts("tel", recipient, null)
            launchActivityIntent(this)
        }
    }
}

fun SimpleActivity.showContactSourcePicker(currentSource: String, callback: (newSource: String) -> Unit) {
    ContactsHelper(this).getSaveableContactSources { sources ->
        val items = ArrayList<RadioItem>()
        var sourceNames = sources.map { it.name }
        var currentSourceIndex = sourceNames.indexOfFirst { it == currentSource }
        sourceNames = sources.map { it.publicName }

        sourceNames.forEachIndexed { index, account ->
            items.add(RadioItem(index, account))
            if (currentSource == SMT_PRIVATE && account == getString(R.string.phone_storage_hidden)) {
                currentSourceIndex = index
            }
        }

        runOnUiThread {
            RadioGroupDialog(this, items, currentSourceIndex) {
                callback(sources[it as Int].name)
            }
        }
    }
}

fun BaseSimpleActivity.shareContacts(contacts: ArrayList<Contact>) {
    val filename = if (contacts.size == 1) {
        "${contacts.first().getNameToDisplay()}.vcf"
    } else {
        DEFAULT_FILE_NAME
    }

    val file = getTempFile(filename)
    if (file == null) {
        toast(org.fossify.commons.R.string.unknown_error_occurred)
        return
    }

    getFileOutputStream(file.toFileDirItem(this), true) {
        VcfExporter().exportContacts(
            context = this,
            outputStream = it,
            contacts = contacts,
            showExportingToast = false
        ) {
            if (it == VcfExporter.ExportResult.EXPORT_OK) {
                sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
            } else {
                showErrorToast("$it")
            }
        }
    }
}

fun SimpleActivity.handleGenericContactClick(contact: Contact) {
    when (config.onContactClick) {
        ON_CLICK_CALL_CONTACT -> callContact(contact)
        ON_CLICK_VIEW_CONTACT -> viewContact(contact)
        ON_CLICK_EDIT_CONTACT -> editContact(contact, config.mergeDuplicateContacts)
    }
}

fun SimpleActivity.callContact(contact: Contact) {
    hideKeyboard()
    if (contact.phoneNumbers.isNotEmpty()) {
        tryInitiateCall(contact) { startCallIntent(it) }
    } else {
        toast(org.fossify.commons.R.string.no_phone_number_found)
    }
}

fun Activity.viewContact(contact: Contact) {
    hideKeyboard()
    Intent(applicationContext, ViewContactActivity::class.java).apply {
        putExtra(CONTACT_ID, contact.id)
        putExtra(IS_PRIVATE, contact.isPrivate())
        startActivity(this)
    }
}

fun Activity.editContact(contact: Contact, isMergedDuplicate: Boolean) {
    if (!isMergedDuplicate) {
        editContact(contact)
    } else {
        ContactsHelper(this).getContactSources { contactSources ->
            getDuplicateContacts(contact, true) { contacts ->
                if (contacts.size == 1) {
                    runOnUiThread {
                        editContact(contacts.first())
                    }
                } else {
                    val items = ArrayList(contacts.mapIndexed { index, contact ->
                        var source = getPublicContactSourceSync(contact.source, contactSources)
                        if (source == "") {
                            source = getString(R.string.phone_storage)
                        }
                        RadioItem(index, source, contact)
                    }.sortedBy { it.title })

                    runOnUiThread {
                        RadioGroupDialog(
                            activity = this,
                            items = items,
                            titleId = R.string.select_account,
                        ) {
                            editContact(it as Contact)
                        }
                    }
                }
            }
        }
    }
}

fun Activity.getDuplicateContacts(contact: Contact, includeCurrent: Boolean, callback: (duplicateContacts: ArrayList<Contact>) -> Unit) {
    val duplicateContacts = ArrayList<Contact>()
    if (includeCurrent) {
        duplicateContacts.add(contact)
    }
    ContactsHelper(this).getDuplicatesOfContact(contact, false) { contacts ->
        ensureBackgroundThread {
            val displayContactSources = getVisibleContactSources()
            contacts.filter { displayContactSources.contains(it.source) }.forEach {
                val duplicate = ContactsHelper(this).getContactWithId(it.id, it.isPrivate())
                if (duplicate != null) {
                    duplicateContacts.add(duplicate)
                }
            }
            callback(duplicateContacts)
        }
    }
}

fun Activity.editContact(contact: Contact) {
    hideKeyboard()
    Intent(applicationContext, EditContactActivity::class.java).apply {
        putExtra(CONTACT_ID, contact.id)
        putExtra(IS_PRIVATE, contact.isPrivate())
        startActivity(this)
    }
}

fun SimpleActivity.tryImportContactsFromFile(uri: Uri, callback: (Boolean) -> Unit) {
    when (uri.scheme) {
        "file" -> showImportContactsDialog(uri.path!!, callback)
        "content" -> {
            val tempFile = getTempFile()
            if (tempFile == null) {
                toast(org.fossify.commons.R.string.unknown_error_occurred)
                return
            }

            try {
                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream!!.copyTo(out)
                showImportContactsDialog(tempFile.absolutePath, callback)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

        else -> toast(org.fossify.commons.R.string.invalid_file_format)
    }
}

fun SimpleActivity.showImportContactsDialog(path: String, callback: (Boolean) -> Unit) {
    ImportContactsDialog(this, path, callback)
}
