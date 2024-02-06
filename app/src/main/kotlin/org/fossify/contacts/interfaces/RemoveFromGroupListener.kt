package org.fossify.contacts.interfaces

import org.fossify.commons.models.contacts.Contact

interface RemoveFromGroupListener {
    fun removeFromGroup(contacts: ArrayList<Contact>)
}
