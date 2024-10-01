package org.fossify.contacts.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.*
import org.fossify.commons.views.MyAppCompatCheckbox
import org.fossify.contacts.R
import org.fossify.contacts.extensions.config

class ManageVisibleFieldsDialog(val activity: BaseSimpleActivity, val callback: (hasSomethingChanged: Boolean) -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_manage_visible_fields, null)
    private val fields = LinkedHashMap<Int, Int>()

    init {
        fields.apply {
            put(SHOW_PREFIX_FIELD, R.id.manage_visible_fields_prefix)
            put(SHOW_FIRST_NAME_FIELD, R.id.manage_visible_fields_first_name)
            put(SHOW_MIDDLE_NAME_FIELD, R.id.manage_visible_fields_middle_name)
            put(SHOW_SURNAME_FIELD, R.id.manage_visible_fields_surname)
            put(SHOW_SUFFIX_FIELD, R.id.manage_visible_fields_suffix)
            put(SHOW_NICKNAME_FIELD, R.id.manage_visible_fields_nickname)
            put(SHOW_PHONE_NUMBERS_FIELD, R.id.manage_visible_fields_phone_numbers)
            put(SHOW_EMAILS_FIELD, R.id.manage_visible_fields_emails)
            put(SHOW_ADDRESSES_FIELD, R.id.manage_visible_fields_addresses)
            put(SHOW_IMS_FIELD, R.id.manage_visible_fields_ims)
            put(SHOW_STRUCTURED_ADDRESSES_FIELD, R.id.manage_visible_fields_structured_addresses)
            put(SHOW_EVENTS_FIELD, R.id.manage_visible_fields_events)
            put(SHOW_NOTES_FIELD, R.id.manage_visible_fields_notes)
            put(SHOW_ORGANIZATION_FIELD, R.id.manage_visible_fields_organization)
            put(SHOW_WEBSITES_FIELD, R.id.manage_visible_fields_websites)
            put(SHOW_GROUPS_FIELD, R.id.manage_visible_fields_groups)
            put(SHOW_CONTACT_SOURCE_FIELD, R.id.manage_visible_fields_contact_source)
            put(SHOW_RINGTONE_FIELD, R.id.manage_ringtone)
        }

        val showContactFields = activity.config.showContactFields
        for ((key, value) in fields) {
            view.findViewById<MyAppCompatCheckbox>(value).isChecked = showContactFields and key != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in fields) {
            if (view.findViewById<MyAppCompatCheckbox>(value).isChecked) {
                result += key
            }
        }

        val hasSomethingChanged = activity.config.showContactFields != result
        activity.config.showContactFields = result

        if (hasSomethingChanged) {
            callback(true)
        }
    }
}
