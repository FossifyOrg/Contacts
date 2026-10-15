package org.fossify.contacts.helpers

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import ezvcard.VCard
import ezvcard.property.Telephone
import ezvcard.property.Email
import ezvcard.property.Birthday
import ezvcard.property.Anniversary
import ezvcard.util.PartialDate
import org.fossify.commons.extensions.getDateTimeFromDateString
import org.fossify.commons.models.contacts.Contact
import java.time.LocalDate

object CardPropertyAdderMain {
    fun addProperties(card: VCard, contact: Contact) {
        addPhoneNumbers(card, contact)
        addEmails(card, contact)
        addEvents(card, contact)
        addAddress(card, contact)
        addIMs(card, contact)
        addNotes(card, contact)
    }

    private fun addPhoneNumbers(card: VCard, contact: Contact) {
        contact.phoneNumbers.forEach {
            val phone = Telephone(it.value).apply {
                parameters.addType(TypeLabelMapper.getPhoneNumberTypeLabel(it.type, it.label))
                if (it.isPrimary) parameters.addType(TypeLabelMapper.getPreferredType(1))
            }
            card.addTelephoneNumber(phone)
        }
    }

    private fun addEmails(card: VCard, contact: Contact) {
        contact.emails.forEach {
            card.addEmail(Email(it.value).apply {
                parameters.addType(TypeLabelMapper.getEmailTypeLabel(it.type, it.label))
            })
        }
    }

    private fun addEvents(card: VCard, contact: Contact) {
        contact.events
            .filter { it.type == Event.TYPE_BIRTHDAY || it.type == Event.TYPE_ANNIVERSARY }
            .forEach { addEventToCard(card, it) }
    }

    private fun addEventToCard(card: VCard, event: org.fossify.commons.models.contacts.Event) {
        val dateTime = event.value.getDateTimeFromDateString(false)
        val isBirthday = event.type == Event.TYPE_BIRTHDAY

        if (event.value.startsWith("--")) {
            val partial = PartialDate.builder()
                .month(dateTime.monthOfYear)
                .date(dateTime.dayOfMonth)
                .build()
            if (isBirthday) card.birthdays.add(Birthday(partial))
            else card.anniversaries.add(Anniversary(partial))
            return
        }

        val date = LocalDate.of(dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth)
        if (isBirthday) card.birthdays.add(Birthday(date))
        else card.anniversaries.add(Anniversary(date))
    }

    private fun addAddress(card: VCard, contact: Contact) {
        contact.addresses.forEach { addr ->
            val address = ezvcard.property.Address().apply {
                if (listOf(
                        addr.country,
                        addr.region,
                        addr.city,
                        addr.postcode,
                        addr.pobox,
                        addr.street,
                        addr.neighborhood)
                        .any { it.isNotEmpty() }) {
                    country = addr.country
                    region = addr.region
                    locality = addr.city
                    postalCode = addr.postcode
                    poBox = addr.pobox
                    streetAddress = addr.street
                    extendedAddress = addr.neighborhood
                } else streetAddress = addr.value
                parameters.addType(TypeLabelMapper.getAddressTypeLabel(addr.type, addr.label))
            }
            card.addAddress(address)
        }
    }

    private fun addIMs(card: VCard, contact: Contact) {
        contact.IMs.forEach {
            val impp = when (it.type) {
                Im.PROTOCOL_AIM -> ezvcard.property.Impp.aim(it.value)
                Im.PROTOCOL_YAHOO -> ezvcard.property.Impp.yahoo(it.value)
                Im.PROTOCOL_MSN -> ezvcard.property.Impp.msn(it.value)
                Im.PROTOCOL_ICQ -> ezvcard.property.Impp.icq(it.value)
                Im.PROTOCOL_SKYPE -> ezvcard.property.Impp.skype(it.value)
                Im.PROTOCOL_GOOGLE_TALK -> ezvcard.property.Impp(HANGOUTS, it.value)
                Im.PROTOCOL_QQ -> ezvcard.property.Impp(QQ, it.value)
                Im.PROTOCOL_JABBER -> ezvcard.property.Impp(JABBER, it.value)
                else -> ezvcard.property.Impp(it.label, it.value)
            }
            card.addImpp(impp)
        }
    }

    private fun addNotes(card: VCard, contact: Contact) {
        if (contact.notes.isNotEmpty()) card.addNote(contact.notes)
    }
}
