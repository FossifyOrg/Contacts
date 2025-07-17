package org.fossify.contacts.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.widget.Toast
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.util.PartialDate
import org.fossify.commons.extensions.getCachePhoto
import org.fossify.commons.extensions.groupsDB
import org.fossify.commons.extensions.normalizePhoneNumber
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.DEFAULT_MIMETYPE
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.contacts.Address
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Email
import org.fossify.commons.models.contacts.Event
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.models.contacts.IM
import org.fossify.commons.models.contacts.Organization
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.extensions.getCachePhotoUri
import org.fossify.contacts.helpers.VcfImporter.ImportResult.IMPORT_FAIL
import org.fossify.contacts.helpers.VcfImporter.ImportResult.IMPORT_OK
import org.fossify.contacts.helpers.VcfImporter.ImportResult.IMPORT_PARTIAL
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.time.LocalDate
import java.util.Locale

class VcfImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private var contactsImported = 0
    private var contactsFailed = 0

    fun importContacts(path: String, targetContactSource: String): ImportResult {
        try {
            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                activity.assets.open(path)
            }

            val ezContacts = Ezvcard.parse(inputStream).all()
            for (ezContact in ezContacts) {
                val structuredName = ezContact.structuredName
                val prefix = structuredName?.prefixes?.firstOrNull() ?: ""
                val firstName = structuredName?.given ?: ""
                val middleName = structuredName?.additionalNames?.firstOrNull() ?: ""
                val surname = structuredName?.family ?: ""
                val suffix = structuredName?.suffixes?.firstOrNull() ?: ""
                val nickname = ezContact.nickname?.values?.firstOrNull() ?: ""
                var photoUri = ""

                val phoneNumbers = ArrayList<PhoneNumber>()
                ezContact.telephoneNumbers.forEach {
                    val number = it.text
                    val type = getPhoneNumberTypeId(
                        type = it.types.firstOrNull()?.value ?: MOBILE,
                        subtype = it.types.getOrNull(1)?.value
                    )
                    val label = if (type == Phone.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }

                    val preferred = getPreferredValue(it.types.lastOrNull()?.value) == 1
                    phoneNumbers.add(
                        PhoneNumber(
                            value = number,
                            type = type,
                            label = label,
                            normalizedNumber = number.normalizePhoneNumber(),
                            isPrimary = preferred
                        )
                    )
                }

                val emails = ArrayList<Email>()
                ezContact.emails.forEach {
                    val email = it.value
                    val type = getEmailTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val label = if (type == CommonDataKinds.Email.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }

                    if (email.isNotEmpty()) {
                        emails.add(Email(email, type, label))
                    }
                }

                val addresses = ArrayList<Address>()
                ezContact.addresses.forEach {
                    var address = it.streetAddress ?: ""
                    val type = getAddressTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val label = if (type == StructuredPostal.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }
                    val country = it.country ?: ""
                    val region = it.region ?: ""
                    val city = it.locality ?: ""
                    val postcode = it.postalCode ?: ""
                    val pobox = it.poBox ?: ""
                    val street = it.streetAddress ?: ""
                    val neighborhood = it.extendedAddress ?: ""

                    if (it.locality?.isNotEmpty() == true) {
                        address += " ${it.locality} "
                    }

                    if (it.region?.isNotEmpty() == true) {
                        if (address.isNotEmpty()) {
                            address = "${address.trim()}, "
                        }
                        address += "${it.region} "
                    }

                    if (it.postalCode?.isNotEmpty() == true) {
                        address += "${it.postalCode} "
                    }

                    if (it.country?.isNotEmpty() == true) {
                        address += "${it.country} "
                    }

                    address = address.trim()
                    if (address.isNotEmpty()) {
                        addresses.add(
                            Address(
                                value = address,
                                type = type,
                                label = label,
                                country = country,
                                region = region,
                                city = city,
                                postcode = postcode,
                                pobox = pobox,
                                street = street,
                                neighborhood = neighborhood
                            )
                        )
                    }
                }

                val events = ArrayList<Event>()
                ezContact.anniversaries.forEach { anniversary ->
                    val event = if (anniversary.date != null) {
                        Event(
                            formatDateToDayCode(LocalDate.from(anniversary.date)),
                            CommonDataKinds.Event.TYPE_ANNIVERSARY
                        )
                    } else {
                        Event(
                            formatPartialDateToDayCode(anniversary.partialDate),
                            CommonDataKinds.Event.TYPE_ANNIVERSARY
                        )
                    }
                    events.add(event)
                }

                ezContact.birthdays.forEach { birthday ->
                    val event = if (birthday.date != null) {
                        Event(
                            formatDateToDayCode(LocalDate.from(birthday.date)),
                            CommonDataKinds.Event.TYPE_BIRTHDAY
                        )
                    } else {
                        Event(
                            formatPartialDateToDayCode(birthday.partialDate),
                            CommonDataKinds.Event.TYPE_BIRTHDAY
                        )
                    }
                    events.add(event)
                }

                val starred = 0
                val contactId = 0
                val notes = ezContact.notes.firstOrNull()?.value ?: ""
                val groups = getContactGroups(ezContact)
                val company = ezContact.organization?.values?.firstOrNull() ?: ""
                val jobPosition = ezContact.titles?.firstOrNull()?.value ?: ""
                val organization = Organization(company, jobPosition)
                val websites = ezContact.urls.map { it.value } as ArrayList<String>
                val photoData = ezContact.photos.firstOrNull()?.data
                val photo = if (photoData != null) {
                    BitmapFactory.decodeByteArray(photoData, 0, photoData.size)
                } else {
                    null
                }

                val thumbnailUri = savePhoto(photoData)
                if (thumbnailUri.isNotEmpty()) {
                    photoUri = thumbnailUri
                }

                val ringtone = null

                val IMs = ArrayList<IM>()
                ezContact.impps.forEach {
                    val typeString = it.uri.scheme
                    val value = URLDecoder.decode(
                        it.uri.toString().substring(it.uri.scheme.length + 1),
                        "UTF-8"
                    )
                    val type = when {
                        it.isAim -> Im.PROTOCOL_AIM
                        it.isYahoo -> Im.PROTOCOL_YAHOO
                        it.isMsn -> Im.PROTOCOL_MSN
                        it.isIcq -> Im.PROTOCOL_ICQ
                        it.isSkype -> Im.PROTOCOL_SKYPE
                        typeString == HANGOUTS -> Im.PROTOCOL_GOOGLE_TALK
                        typeString == QQ -> Im.PROTOCOL_QQ
                        typeString == JABBER -> Im.PROTOCOL_JABBER
                        else -> Im.PROTOCOL_CUSTOM
                    }

                    val label = if (type == Im.PROTOCOL_CUSTOM) {
                        URLDecoder.decode(
                            typeString,
                            "UTF-8"
                        )
                    } else {
                        ""
                    }
                    val IM = IM(value, type, label)
                    IMs.add(IM)
                }

                val contact = Contact(
                    id = 0,
                    prefix = prefix,
                    firstName = firstName,
                    middleName = middleName,
                    surname = surname,
                    suffix = suffix,
                    nickname = nickname,
                    photoUri = photoUri,
                    phoneNumbers = phoneNumbers,
                    emails = emails,
                    addresses = addresses,
                    events = events,
                    source = targetContactSource,
                    starred = starred,
                    contactId = contactId,
                    thumbnailUri = thumbnailUri,
                    photo = photo,
                    notes = notes,
                    groups = groups,
                    organization = organization,
                    websites = websites,
                    IMs = IMs,
                    mimetype = DEFAULT_MIMETYPE,
                    ringtone = ringtone
                )

                // if there is no N and ORG fields at the given contact, only FN, treat it as an organization
                if (
                    contact.getNameToDisplay().isEmpty()
                    && contact.organization.isEmpty()
                    && ezContact.formattedName?.value?.isNotEmpty() == true
                ) {
                    contact.organization.company = ezContact.formattedName.value
                    contact.mimetype = CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                }

                if (contact.isABusinessContact()) {
                    contact.mimetype = CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                }

                if (ContactsHelper(activity).insertContact(contact)) {
                    contactsImported++
                }
            }
        } catch (e: Exception) {
            activity.showErrorToast(e, Toast.LENGTH_LONG)
            contactsFailed++
        }

        return when {
            contactsImported == 0 -> IMPORT_FAIL
            contactsFailed > 0 -> IMPORT_PARTIAL
            else -> IMPORT_OK
        }
    }

    private fun formatDateToDayCode(date: LocalDate): String {
        if (date.year == 1900) {
            // for backward compatibility with old exports
            return "--%02d-%02d".format(date.monthValue, date.dayOfMonth)
        }

        return "%04d-%02d-%02d".format(
            date.year, date.monthValue, date.dayOfMonth
        )
    }

    private fun formatPartialDateToDayCode(partialDate: PartialDate): String {
        return "--%02d-%02d".format(
            partialDate.month, partialDate.date
        )
    }

    private fun getContactGroups(ezContact: VCard): ArrayList<Group> {
        val groups = ArrayList<Group>()
        if (ezContact.categories != null) {
            val groupNames = ezContact.categories.values

            if (groupNames != null) {
                val storedGroups = ContactsHelper(activity).getStoredGroupsSync()

                groupNames.forEach {
                    val groupName = it
                    val storedGroup = storedGroups.firstOrNull { it.title == groupName }

                    if (storedGroup != null) {
                        groups.add(storedGroup)
                    } else {
                        val newGroup = Group(null, groupName)
                        val id = activity.groupsDB.insertOrUpdate(newGroup)
                        newGroup.id = id
                        groups.add(newGroup)
                    }
                }
            }
        }
        return groups
    }

    private fun getPhoneNumberTypeId(type: String, subtype: String?) =
        when (type.uppercase(Locale.getDefault())) {
            CELL -> Phone.TYPE_MOBILE
            HOME -> {
                if (subtype?.uppercase(Locale.getDefault()) == FAX) {
                    Phone.TYPE_FAX_HOME
                } else {
                    Phone.TYPE_HOME
                }
            }

            WORK -> {
                if (subtype?.uppercase(Locale.getDefault()) == FAX) {
                    Phone.TYPE_FAX_WORK
                } else {
                    Phone.TYPE_WORK
                }
            }

            MAIN -> Phone.TYPE_MAIN
            WORK_FAX -> Phone.TYPE_FAX_WORK
            HOME_FAX -> Phone.TYPE_FAX_HOME
            FAX -> Phone.TYPE_FAX_WORK
            PAGER -> Phone.TYPE_PAGER
            OTHER -> Phone.TYPE_OTHER
            else -> Phone.TYPE_CUSTOM
        }

    private fun getEmailTypeId(type: String) = when (type.uppercase(Locale.getDefault())) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        MOBILE -> CommonDataKinds.Email.TYPE_MOBILE
        OTHER -> CommonDataKinds.Email.TYPE_OTHER
        else -> CommonDataKinds.Email.TYPE_CUSTOM
    }

    private fun getAddressTypeId(type: String) = when (type.uppercase(Locale.getDefault())) {
        HOME -> StructuredPostal.TYPE_HOME
        WORK -> StructuredPostal.TYPE_WORK
        OTHER -> StructuredPostal.TYPE_OTHER
        else -> StructuredPostal.TYPE_CUSTOM
    }

    private fun savePhoto(byteArray: ByteArray?): String {
        if (byteArray == null) {
            return ""
        }

        val file = activity.getCachePhoto()
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        } finally {
            fileOutputStream?.close()
        }

        return activity.getCachePhotoUri(file).toString()
    }

    private fun getPreferredValue(type: String?): Int {
        if (type != null) {
            if (type.startsWith("$PREF=".lowercase())) {
                return type.split("=")[1].toInt()
            }
        }

        return -1
    }
}
