package org.fossify.contacts.helpers

import android.content.Context
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.property.FormattedName
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.models.contacts.Contact
import java.io.OutputStream

class VcfExporter {
    enum class ExportResult { EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL }

    private var contactsExported = 0
    private var contactsFailed = 0

    fun exportContacts(
        context: Context,
        outputStream: OutputStream?,
        contacts: ArrayList<Contact>,
        showExportingToast: Boolean,
        callback: (result: ExportResult) -> Unit,
    ) {
        if (outputStream == null) {
            callback(ExportResult.EXPORT_FAIL)
            return
        }

        if (showExportingToast) {
            context.toast(org.fossify.commons.R.string.exporting)
        }

        val version = getVCardVersion(context)
        val cards = contacts.map { contact ->
            val card = VCard()
            card.formattedName = FormattedName(getFormattedName(contact))
            CardPropertyAdderMain.addProperties(card, contact)
            CardPropertyAdderAdditional.addProperties(context, card, contact)
            contactsExported++
            card
        }

        try {
            Ezvcard.write(cards).version(version).go(outputStream)
        } catch (e: Exception) {
            context.showErrorToast(e)
        }

        callback(
            when {
                contactsExported == 0 -> ExportResult.EXPORT_FAIL
                contactsFailed > 0 -> ExportResult.EXPORT_PARTIAL
                else -> ExportResult.EXPORT_OK
            }
        )
    }

    private fun getVCardVersion(context: Context): VCardVersion {
        val versionString = Config.newInstance(context).vCardVersion
        return when (versionString) {
            "2.1" -> VCardVersion.V2_1
            "3" -> VCardVersion.V3_0
            "4" -> VCardVersion.V4_0
            else -> VCardVersion.V4_0
        }
    }

    private fun getFormattedName(contact: Contact): String =
        arrayOf(contact.prefix, contact.firstName, contact.middleName, contact.surname, contact.suffix)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
