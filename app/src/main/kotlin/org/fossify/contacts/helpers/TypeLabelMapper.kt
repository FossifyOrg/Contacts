package org.fossify.contacts.helpers

object TypeLabelMapper {
    fun getPhoneNumberTypeLabel(type: Int, label: String) = when (type) {
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "CELL"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "MAIN"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "WORK_FAX"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "HOME_FAX"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "PAGER"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "OTHER"
        else -> label
    }

    fun getEmailTypeLabel(type: Int, label: String) = when (type) {
        android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "HOME"
        android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "WORK"
        android.provider.ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> "MOBILE"
        android.provider.ContactsContract.CommonDataKinds.Email.TYPE_OTHER -> "OTHER"
        else -> label
    }

    fun getAddressTypeLabel(type: Int, label: String) = when (type) {
        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> "HOME"
        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "WORK"
        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER -> "OTHER"
        else -> label
    }

    fun getPreferredType(value: Int) = "PREF=$value"
}
