package org.fossify.contacts.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.BaseTypes
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getNameLetter
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.insetsController
import org.fossify.commons.extensions.launchSendSMSIntent
import org.fossify.commons.extensions.realScreenSize
import org.fossify.commons.extensions.sendEmailIntent
import org.fossify.commons.extensions.setNavigationBarAppearance
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.letterBackgroundColors
import org.fossify.commons.models.RadioItem
import org.fossify.commons.models.contacts.Contact
import org.fossify.contacts.R
import org.fossify.contacts.extensions.shareContacts

abstract class ContactActivity : SimpleActivity() {
    companion object {
        protected const val PICK_RINGTONE_INTENT_ID = 1500
        protected const val INTENT_SELECT_RINGTONE = 600
    }

    protected var contact: Contact? = null
    protected var originalRingtone: String? = null
    protected var currentContactPhotoPath = ""

    override fun onResume() {
        super.onResume()
        window.insetsController().isAppearanceLightStatusBars = false
        window.setNavigationBarAppearance(getProperBackgroundColor())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_RINGTONE_INTENT_ID && resultCode == RESULT_OK && resultData != null && resultData.dataString != null) {
            customRingtoneSelected(Uri.decode(resultData.dataString!!))
        } else if (requestCode == INTENT_SELECT_RINGTONE && resultCode == Activity.RESULT_OK && resultData != null) {
            val extras = resultData.extras
            if (extras?.containsKey(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) == true) {
                val uri = extras.getParcelable<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                try {
                    systemRingtoneSelected(uri)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    abstract fun customRingtoneSelected(ringtonePath: String)

    abstract fun systemRingtoneSelected(uri: Uri?)

    fun showPhotoPlaceholder(photoView: ImageView) {
        val placeholder = BitmapDrawable(resources, getBigLetterPlaceholder(contact?.getNameToDisplay() ?: "A"))
        photoView.setImageDrawable(placeholder)
        currentContactPhotoPath = ""
        contact?.photo = null
    }

    fun updateContactPhoto(path: String, photoView: ImageView, bottomShadow: ImageView, bitmap: Bitmap? = null) {
        currentContactPhotoPath = path

        if (isDestroyed || isFinishing) {
            return
        }

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()

        val wantedWidth = realScreenSize.x
        val wantedHeight = resources.getDimension(R.dimen.top_contact_image_height).toInt()

        Glide.with(this)
            .load(bitmap ?: path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .override(wantedWidth, wantedHeight)
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    photoView.background = ColorDrawable(0)
                    bottomShadow.beVisible()
                    return false
                }

                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    showPhotoPlaceholder(photoView)
                    bottomShadow.beGone()
                    return true
                }
            }).into(photoView)
    }

    fun deleteContact() {
        ConfirmationDialog(this) {
            if (contact != null) {
                ContactsHelper(this).deleteContact(contact!!, false) {
                    finish()
                }
            }
        }
    }

    fun shareContact(contact: Contact) {
        shareContacts(arrayListOf(contact))
    }

    fun trySendSMS() {
        val numbers = contact!!.phoneNumbers
        if (numbers.size == 1) {
            launchSendSMSIntent(numbers.first().value)
        } else if (numbers.size > 1) {
            val primaryNumber = numbers.find { it.isPrimary }
            if (primaryNumber != null) {
                launchSendSMSIntent(primaryNumber.value)
            } else {
                val items = ArrayList<RadioItem>()
                numbers.forEachIndexed { index, phoneNumber ->
                    items.add(RadioItem(index, phoneNumber.value, phoneNumber.value))
                }

                RadioGroupDialog(this, items) {
                    launchSendSMSIntent(it as String)
                }
            }
        }
    }

    fun trySendEmail() {
        val emails = contact!!.emails
        if (emails.size == 1) {
            sendEmailIntent(emails.first().value)
        } else if (emails.size > 1) {
            val items = ArrayList<RadioItem>()
            emails.forEachIndexed { index, email ->
                items.add(RadioItem(index, email.value, email.value))
            }

            RadioGroupDialog(this, items) {
                sendEmailIntent(it as String)
            }
        }
    }

    fun getEmailTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Email.TYPE_HOME -> org.fossify.commons.R.string.home
                    Email.TYPE_WORK -> org.fossify.commons.R.string.work
                    Email.TYPE_MOBILE -> org.fossify.commons.R.string.mobile
                    else -> org.fossify.commons.R.string.other
                }
            )
        }
    }

    fun getAddressTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    StructuredPostal.TYPE_HOME -> org.fossify.commons.R.string.home
                    StructuredPostal.TYPE_WORK -> org.fossify.commons.R.string.work
                    else -> org.fossify.commons.R.string.other
                }
            )
        }
    }

    fun getIMTypeText(type: Int, label: String): String {
        return if (type == Im.PROTOCOL_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Im.PROTOCOL_AIM -> R.string.aim
                    Im.PROTOCOL_MSN -> R.string.windows_live
                    Im.PROTOCOL_YAHOO -> R.string.yahoo
                    Im.PROTOCOL_SKYPE -> R.string.skype
                    Im.PROTOCOL_QQ -> R.string.qq
                    Im.PROTOCOL_GOOGLE_TALK -> R.string.hangouts
                    Im.PROTOCOL_ICQ -> R.string.icq
                    else -> R.string.jabber
                }
            )
        }
    }

    fun getEventTextId(type: Int) = when (type) {
        Event.TYPE_ANNIVERSARY -> org.fossify.commons.R.string.anniversary
        Event.TYPE_BIRTHDAY -> org.fossify.commons.R.string.birthday
        else -> org.fossify.commons.R.string.other
    }

    private fun getBigLetterPlaceholder(name: String): Bitmap {
        val letter = name.getNameLetter()
        val height = resources.getDimension(R.dimen.top_contact_image_height).toInt()
        val bitmap = Bitmap.createBitmap(realScreenSize.x, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val view = TextView(this)
        view.layout(0, 0, bitmap.width, bitmap.height)

        val circlePaint = Paint().apply {
            color = letterBackgroundColors[Math.abs(name.hashCode()) % letterBackgroundColors.size].toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val wantedTextSize = bitmap.height / 2f
        val textPaint = Paint().apply {
            color = circlePaint.color.getContrastColor()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = wantedTextSize
            style = Paint.Style.FILL
        }

        canvas.drawPaint(circlePaint)

        val xPos = canvas.width / 2f
        val yPos = canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, xPos, yPos, textPaint)
        view.draw(canvas)
        return bitmap
    }

    protected fun getDefaultRingtoneUri() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    protected fun getRingtonePickerIntent(): Intent {
        val defaultRingtoneUri = getDefaultRingtoneUri()
        val currentRingtoneUri = if (contact!!.ringtone != null && contact!!.ringtone!!.isNotEmpty()) {
            Uri.parse(contact!!.ringtone)
        } else if (contact!!.ringtone?.isNotEmpty() == false) {
            null
        } else {
            defaultRingtoneUri
        }

        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultRingtoneUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneUri)
        }
    }
}
