package org.fossify.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.contacts.databinding.DialogCustomLabelBinding

class CustomLabelDialog(val activity: BaseSimpleActivity, val callback: (label: String) -> Unit) {
    init {
        val binding = DialogCustomLabelBinding.inflate(activity.layoutInflater)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, org.fossify.commons.R.string.label) { alertDialog ->
                    alertDialog.showKeyboard(binding.customLabelEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val label = binding.customLabelEdittext.value
                        if (label.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.empty_name)
                            return@setOnClickListener
                        }

                        callback(label)
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
