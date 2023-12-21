package org.fossify.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.ContactSource
import org.fossify.contacts.R
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.adapters.FilterContactSourcesAdapter
import org.fossify.contacts.databinding.DialogManageAutomaticBackupsBinding
import org.fossify.contacts.extensions.config
import java.io.File

class ManageAutoBackupsDialog(private val activity: SimpleActivity, onSuccess: () -> Unit) {
    private val binding = DialogManageAutomaticBackupsBinding.inflate(activity.layoutInflater)
    private val config = activity.config
    private var backupFolder = config.autoBackupFolder
    private var contactSources = mutableListOf<ContactSource>()
    private var selectedContactSources = config.autoBackupContactSources
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        binding.apply {
            backupContactsFolder.setText(activity.humanizePath(backupFolder))
            val filename = config.autoBackupFilename.ifEmpty {
                "${activity.getString(R.string.contacts)}_%Y%M%D_%h%m%s"
            }

            backupContactsFilename.setText(filename)
            backupContactsFilenameHint.setEndIconOnClickListener {
                DateTimePatternInfoDialog(activity)
            }

            backupContactsFilenameHint.setEndIconOnLongClickListener {
                DateTimePatternInfoDialog(activity)
                true
            }

            backupContactsFolder.setOnClickListener {
                selectBackupFolder()
            }

            ContactsHelper(activity).getContactSources { sources ->
                contactSources = sources
                isContactSourcesReady = true
                processDataIfReady(this)
            }

            ContactsHelper(activity).getContacts(getAll = true) { receivedContacts ->
                contacts = receivedContacts
                isContactsReady = true
                processDataIfReady(this)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, org.fossify.commons.R.string.manage_automatic_backups) { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (binding.backupContactSourcesList.adapter == null) {
                            return@setOnClickListener
                        }
                        val filename = binding.backupContactsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(org.fossify.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(backupFolder, "$filename.vcf")
                                if (file.exists() && !file.canWrite()) {
                                    activity.toast(org.fossify.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                val selectedSources = (binding.backupContactSourcesList.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
                                if (selectedSources.isEmpty()) {
                                    activity.toast(org.fossify.commons.R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                config.autoBackupContactSources = selectedSources.map { it.name }.toSet()

                                ensureBackgroundThread {
                                    config.apply {
                                        autoBackupFolder = backupFolder
                                        autoBackupFilename = filename
                                    }

                                    activity.runOnUiThread {
                                        onSuccess()
                                    }

                                    dialog.dismiss()
                                }
                            }

                            else -> activity.toast(org.fossify.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }

    private fun processDataIfReady(binding: DialogManageAutomaticBackupsBinding) {
        if (!isContactSourcesReady || !isContactsReady) {
            return
        }

        if (selectedContactSources.isEmpty()) {
            selectedContactSources = contactSources.map { it.name }.toSet()
        }

        val contactSourcesWithCount = mutableListOf<ContactSource>()
        for (source in contactSources) {
            val count = contacts.count { it.source == source.name }
            contactSourcesWithCount.add(source.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            binding.backupContactSourcesList.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, selectedContactSources.toList())
        }
    }

    private fun selectBackupFolder() {
        activity.hideKeyboard(binding.backupContactsFilename)
        FilePickerDialog(activity, backupFolder, false, showFAB = true) { path ->
            activity.handleSAFDialog(path) { grantedSAF ->
                if (!grantedSAF) {
                    return@handleSAFDialog
                }

                activity.handleSAFDialogSdk30(path) { grantedSAF30 ->
                    if (!grantedSAF30) {
                        return@handleSAFDialogSdk30
                    }

                    backupFolder = path
                    binding.backupContactsFolder.setText(activity.humanizePath(path))
                }
            }
        }
    }
}

