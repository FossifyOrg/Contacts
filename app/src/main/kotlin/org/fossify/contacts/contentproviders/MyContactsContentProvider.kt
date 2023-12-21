package org.fossify.contacts.contentproviders

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.google.gson.Gson
import org.fossify.commons.helpers.LocalContactsHelper
import org.fossify.commons.helpers.MyContactsContentProvider
import org.fossify.contacts.extensions.config

class MyContactsContentProvider : ContentProvider() {
    override fun insert(uri: Uri, contentValues: ContentValues?) = null

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (context == null || !context!!.config.showPrivateContacts) {
            return null
        } else {
            val matrixCursor = MatrixCursor(
                arrayOf(
                    MyContactsContentProvider.COL_RAW_ID,
                    MyContactsContentProvider.COL_CONTACT_ID,
                    MyContactsContentProvider.COL_NAME,
                    MyContactsContentProvider.COL_PHOTO_URI,
                    MyContactsContentProvider.COL_PHONE_NUMBERS,
                    MyContactsContentProvider.COL_BIRTHDAYS,
                    MyContactsContentProvider.COL_ANNIVERSARIES
                )
            )

            val favoritesOnly = selectionArgs?.getOrNull(0)?.equals("1") ?: false
            val withPhoneNumbersOnly = selectionArgs?.getOrNull(1)?.equals("1") ?: true

            LocalContactsHelper(context!!).getPrivateSimpleContactsSync(favoritesOnly, withPhoneNumbersOnly).forEach {
                val phoneNumbers = Gson().toJson(it.phoneNumbers)
                val birthdays = Gson().toJson(it.birthdays)
                val anniversaries = Gson().toJson(it.anniversaries)

                matrixCursor.newRow()
                    .add(MyContactsContentProvider.COL_RAW_ID, it.rawId)
                    .add(MyContactsContentProvider.COL_CONTACT_ID, it.contactId)
                    .add(MyContactsContentProvider.COL_NAME, it.name)
                    .add(MyContactsContentProvider.COL_PHOTO_URI, it.photoUri)
                    .add(MyContactsContentProvider.COL_PHONE_NUMBERS, phoneNumbers)
                    .add(MyContactsContentProvider.COL_BIRTHDAYS, birthdays)
                    .add(MyContactsContentProvider.COL_ANNIVERSARIES, anniversaries)
            }

            return matrixCursor
        }
    }

    override fun onCreate() = true

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 1

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri) = ""
}
