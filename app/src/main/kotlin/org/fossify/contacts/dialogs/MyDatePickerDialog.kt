package org.fossify.contacts.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.isSPlus
import org.fossify.contacts.databinding.DialogDatePickerBinding
import org.joda.time.DateTime
import java.util.Calendar

class MyDatePickerDialog(val activity: BaseSimpleActivity, val defaultDate: String, val callback: (dateTag: String) -> Unit) {
    private val binding = DialogDatePickerBinding.inflate(activity.layoutInflater)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    val today = Calendar.getInstance()
                    var year = today.get(Calendar.YEAR)
                    var month = today.get(Calendar.MONTH)
                    var day = today.get(Calendar.DAY_OF_MONTH)

                    if (defaultDate.isNotEmpty()) {
                        val ignoreYear = defaultDate.startsWith("-")
                        binding.hideYear.isChecked = ignoreYear

                        if (ignoreYear) {
                            month = defaultDate.substring(2, 4).toInt() - 1
                            day = defaultDate.substring(5, 7).toInt()
                        } else {
                            year = defaultDate.substring(0, 4).toInt()
                            month = defaultDate.substring(5, 7).toInt() - 1
                            day = defaultDate.substring(8, 10).toInt()
                        }
                    }

                    if (activity.isDynamicTheme() && isSPlus()) {
                        val dialogBackgroundColor = activity.getColor(org.fossify.commons.R.color.you_dialog_background_color)
                        binding.dialogHolder.setBackgroundColor(dialogBackgroundColor)
                        binding.datePicker.setBackgroundColor(dialogBackgroundColor)
                    }

                    binding.datePicker.updateDate(year, month, day)
                }
            }
    }

    private fun dialogConfirmed() {
        val year = binding.datePicker.year
        val month = binding.datePicker.month + 1
        val day = binding.datePicker.dayOfMonth
        val date = DateTime().withDate(year, month, day).withTimeAtStartOfDay()

        val tag = if (binding.hideYear.isChecked) {
            date.toString("--MM-dd")
        } else {
            date.toString("yyyy-MM-dd")
        }

        callback(tag)
    }
}
