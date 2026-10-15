package org.fossify.contacts.helpers

import android.content.Context
import androidx.core.net.toUri
import ezvcard.VCard
import ezvcard.parameter.ImageType
import ezvcard.property.Organization
import ezvcard.property.Title
import ezvcard.property.Photo
import ezvcard.property.Categories
import org.fossify.commons.models.contacts.Contact

object CardPropertyAdderAdditional {
    fun addProperties(context: Context, card: VCard, contact: Contact) {
        addOrganization(card, contact)
        addWebsites(card, contact)
        addPhoto(context, card, contact)
        addGroups(card, contact)
    }

    private fun addOrganization(card: VCard, contact: Contact) {
        if (contact.organization.isNotEmpty()) {
            val org = Organization().apply { values.add(contact.organization.company) }
            card.organization = org
            card.titles.add(Title(contact.organization.jobPosition))
        }
    }

    private fun addWebsites(card: VCard, contact: Contact) {
        contact.websites.forEach { card.addUrl(it) }
    }

    private fun addPhoto(context: Context, card: VCard, contact: Contact) {
        try {
            context.contentResolver.openInputStream(contact.photoUri.toUri())?.use {
                card.addPhoto(Photo(it.readBytes(), ImageType.JPEG))
            }
        } catch (_: Exception) {}
    }

    private fun addGroups(card: VCard, contact: Contact) {
        if (contact.groups.isNotEmpty()) {
            card.categories = Categories().apply { contact.groups.forEach { values.add(it.title) } }
        }
    }
}
