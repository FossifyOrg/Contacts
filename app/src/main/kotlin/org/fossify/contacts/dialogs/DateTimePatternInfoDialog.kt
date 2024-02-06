package org.fossify.contacts.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.contacts.R

class DateTimePatternInfoDialog(activity: BaseSimpleActivity) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.datetime_pattern_info_layout, null)
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ -> { } }
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }
}
