package org.fossify.contacts.extensions

import android.telephony.PhoneNumberUtils
import java.util.Locale

fun String.formatPhoneNumber(minimumLength: Int = 4): String {
    val country = Locale.getDefault().country
    return if (this.length >= minimumLength) {
        PhoneNumberUtils.formatNumber(this, country).toString()
    } else {
        this
    }
}
