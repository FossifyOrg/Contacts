package org.fossify.contacts.helpers

import android.content.Context
import android.telephony.PhoneNumberUtils
import org.fossify.commons.helpers.BaseConfig
import org.fossify.commons.helpers.SHOW_CONTACT_THUMBNAILS
import org.fossify.commons.helpers.SHOW_TABS

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showPhoneNumbersFormatting: Boolean
        get() = prefs.getBoolean(ENABLE_FORMATTING_PHONE_NUMBER, true)
        set(showPhoneNumbersFormatting) = prefs.edit().putBoolean(ENABLE_FORMATTING_PHONE_NUMBER, showPhoneNumbersFormatting).apply()

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var autoBackupContactSources: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CONTACT_SOURCES, setOf())!!
        set(autoBackupContactSources) = prefs.edit().remove(AUTO_BACKUP_CONTACT_SOURCES).putStringSet(AUTO_BACKUP_CONTACT_SOURCES, autoBackupContactSources)
            .apply()



}
