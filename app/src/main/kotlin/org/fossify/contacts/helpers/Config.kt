package org.fossify.contacts.helpers

import android.content.Context
import org.fossify.commons.helpers.BaseConfig
import org.fossify.commons.helpers.SHOW_TABS

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var autoBackupContactSources: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CONTACT_SOURCES, setOf())!!
        set(autoBackupContactSources) = prefs.edit().remove(AUTO_BACKUP_CONTACT_SOURCES).putStringSet(AUTO_BACKUP_CONTACT_SOURCES, autoBackupContactSources)
            .apply()

    var showCompanyInList: Boolean
        get() = prefs.getBoolean(SHOW_COMPANY_IN_LIST, false)
        set(showCompanyInList) = prefs.edit().putBoolean(SHOW_COMPANY_IN_LIST, showCompanyInList).apply()

    var showGroupsInList: Boolean
        get() = prefs.getBoolean(SHOW_GROUPS_IN_LIST, false)
        set(showGroupsInList) = prefs.edit().putBoolean(SHOW_GROUPS_IN_LIST, showGroupsInList).apply()
}
