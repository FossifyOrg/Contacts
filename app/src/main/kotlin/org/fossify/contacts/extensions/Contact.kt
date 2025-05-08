package org.fossify.contacts.extensions

import org.fossify.commons.helpers.*
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.ContactSource

val messengerSources = hashSetOf(TELEGRAM_PACKAGE, SIGNAL_PACKAGE, WHATSAPP_PACKAGE, VIBER_PACKAGE, THREEMA_PACKAGE)

fun Contact.isMessengerContact(contactSources: List<ContactSource>): Boolean {
    val sourceData = contactSources.find { it.name == this.source }
    return sourceData != null && messengerSources.contains(sourceData.type)
}
