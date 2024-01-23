package org.fossify.contacts.helpers

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import androidx.annotation.RequiresApi
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.parameter.ImageType
import ezvcard.property.*
import org.fossify.commons.extensions.getDateTimeFromDateString
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.helpers.VcfExporter.ExportResult.EXPORT_FAIL
import java.io.OutputStream
import java.util.Calendar

class VcfExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var contactsExported = 0
    private var contactsFailed = 0

    @RequiresApi(Build.VERSION_CODES.P)
    fun exportContacts(
        context: Context,
        outputStream: OutputStream?,
        contacts: ArrayList<Contact>,
        showExportingToast: Boolean,
        version: VCardVersion = VCardVersion.V4_0,
        callback: (result: ExportResult) -> Unit
    ) {
        try {
            if (outputStream == null) {
                callback(EXPORT_FAIL)
                return
            }

            if (showExportingToast) {
                context.toast(org.fossify.commons.R.string.exporting)
            }

            val cards = ArrayList<VCard>()
            for (contact in contacts) {
                val card = VCard()

                val formattedName = arrayOf(contact.prefix, contact.firstName, contact.middleName, contact.surname, contact.suffix)
                    .filter { it.isNotEmpty() }
                    .joinToString(separator = " ")
                card.formattedName = FormattedName(formattedName)

                StructuredName().apply {
                    prefixes.add(contact.prefix)
                    given = contact.firstName
                    additionalNames.add(contact.middleName)
                    family = contact.surname
                    suffixes.add(contact.suffix)
                    card.structuredName = this
                }

                if (contact.nickname.isNotEmpty()) {
                    card.setNickname(contact.nickname)
                }

                contact.phoneNumbers.forEach {
                    val phoneNumber = Telephone(it.value)
                    phoneNumber.parameters.addType(getPhoneNumberTypeLabel(it.type, it.label))
                    if (it.isPrimary) {
                        phoneNumber.parameters.addType(getPreferredType(1))
                    }
                    card.addTelephoneNumber(phoneNumber)
                }

                contact.emails.forEach {
                    val email = Email(it.value)
                    email.parameters.addType(getEmailTypeLabel(it.type, it.label))
                    card.addEmail(email)
                }

                contact.events.forEach { event ->
                    if (event.type == Event.TYPE_ANNIVERSARY || event.type == Event.TYPE_BIRTHDAY) {
                        val dateTime = event.value.getDateTimeFromDateString(false)
                        Calendar.getInstance().apply {
                            clear()
                            if (event.value.startsWith("--")) {
                                set(Calendar.YEAR, 1900)
                            } else {
                                set(Calendar.YEAR, dateTime.year)

                            }
                            set(Calendar.MONTH, dateTime.monthOfYear - 1)
                            set(Calendar.DAY_OF_MONTH, dateTime.dayOfMonth)
                            if (event.type == Event.TYPE_BIRTHDAY) {
                                card.birthdays.add(Birthday(time))
                            } else {
                                card.anniversaries.add(Anniversary(time))
                            }
                        }
                    }
                }

                contact.addresses.forEach {
                    val address = Address()
                    if (listOf(it.country, it.region, it.city, it.postcode, it.pobox, it.street, it.neighborhood)
                        .map{it.isNullOrEmpty()}
                        .fold(false){a, b -> a || b}) {
                        address.country = it.country
                        address.region = it.region
                        address.locality = it.city
                        address.postalCode = it.postcode
                        address.poBox = it.pobox
                        address.streetAddress = it.street
                        address.extendedAddress = it.neighborhood
                    } else {
                        address.streetAddress = it.value
                    }
                    address.parameters.addType(getAddressTypeLabel(it.type, it.label))
                    card.addAddress(address)
                }

                contact.IMs.forEach {
                    val impp = when (it.type) {
                        Im.PROTOCOL_AIM -> Impp.aim(it.value)
                        Im.PROTOCOL_YAHOO -> Impp.yahoo(it.value)
                        Im.PROTOCOL_MSN -> Impp.msn(it.value)
                        Im.PROTOCOL_ICQ -> Impp.icq(it.value)
                        Im.PROTOCOL_SKYPE -> Impp.skype(it.value)
                        Im.PROTOCOL_GOOGLE_TALK -> Impp(HANGOUTS, it.value)
                        Im.PROTOCOL_QQ -> Impp(QQ, it.value)
                        Im.PROTOCOL_JABBER -> Impp(JABBER, it.value)
                        else -> Impp(it.label, it.value)
                    }

                    card.addImpp(impp)
                }

                if (contact.notes.isNotEmpty()) {
                    card.addNote(contact.notes)
                }

                if (contact.organization.isNotEmpty()) {
                    val organization = Organization()
                    organization.values.add(contact.organization.company)
                    card.organization = organization
                    card.titles.add(Title(contact.organization.jobPosition))
                }

                contact.websites.forEach {
                    card.addUrl(it)
                }

                try {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(contact.photoUri))

                    if (inputStream != null) {
                        val photoByteArray = inputStream.readBytes()
                        val photo = Photo(photoByteArray, ImageType.JPEG)
                        card.addPhoto(photo)
                        inputStream.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (contact.groups.isNotEmpty()) {
                    val groupList = Categories()
                    contact.groups.forEach {
                        groupList.values.add(it.title)
                    }

                    card.categories = groupList
                }

                cards.add(card)
                contactsExported++
            }

            Ezvcard.write(cards).version(version).go(outputStream)
        } catch (e: Exception) {
            context.showErrorToast(e)
        }

        callback(
            when {
                contactsExported == 0 -> EXPORT_FAIL
                contactsFailed > 0 -> ExportResult.EXPORT_PARTIAL
                else -> ExportResult.EXPORT_OK
            }
        )
    }

    private fun getPhoneNumberTypeLabel(type: Int, label: String) = when (type) {
        Phone.TYPE_MOBILE -> CELL
        Phone.TYPE_HOME -> HOME
        Phone.TYPE_WORK -> WORK
        Phone.TYPE_MAIN -> MAIN
        Phone.TYPE_FAX_WORK -> WORK_FAX
        Phone.TYPE_FAX_HOME -> HOME_FAX
        Phone.TYPE_PAGER -> PAGER
        Phone.TYPE_OTHER -> OTHER
        else -> label
    }

    private fun getEmailTypeLabel(type: Int, label: String) = when (type) {
        Email.TYPE_HOME -> HOME
        Email.TYPE_WORK -> WORK
        Email.TYPE_MOBILE -> MOBILE
        Email.TYPE_OTHER -> OTHER
        else -> label
    }

    private fun getAddressTypeLabel(type: Int, label: String) = when (type) {
        StructuredPostal.TYPE_HOME -> HOME
        StructuredPostal.TYPE_WORK -> WORK
        StructuredPostal.TYPE_OTHER -> OTHER
        else -> label
    }

    private fun getPreferredType(value: Int) = "$PREF=$value"
}
