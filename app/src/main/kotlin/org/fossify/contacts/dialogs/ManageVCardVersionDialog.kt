package org.fossify.contacts.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.contacts.R
import org.fossify.contacts.extensions.config
import org.fossify.contacts.helpers.*

class ManageVCardVersionDialog(val activity: BaseSimpleActivity) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_vcard_version, null)
    private val versionRadioButtons = LinkedHashMap<String, Int>()

    init {
        versionRadioButtons.apply {
            put("2.1", R.id.manage_vcard_version_2_1)
            put("3", R.id.manage_vcard_version_3)
            put("4", R.id.manage_vcard_version_4)
        }

        val currentVersion = activity.config.VCardVersion
        versionRadioButtons[currentVersion]?.let { radioButtonId ->
            view.findViewById<android.widget.RadioButton>(radioButtonId).isChecked = true
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val selectedVersion = versionRadioButtons.entries.find { entry ->
            view.findViewById<android.widget.RadioButton>(entry.value).isChecked
        }?.key ?: DEFAULT_VCARD_VERSION

        activity.config.VCardVersion = selectedVersion
    }
}
