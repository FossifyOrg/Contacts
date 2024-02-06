package org.fossify.contacts.interfaces

import org.fossify.commons.models.contacts.Contact

interface RefreshContactsListener {
    fun refreshContacts(refreshTabsMask: Int)

    fun contactClicked(contact: Contact)
}
