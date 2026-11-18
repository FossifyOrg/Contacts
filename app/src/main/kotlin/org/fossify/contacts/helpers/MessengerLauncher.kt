package org.fossify.contacts.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import org.fossify.contacts.R

/**
 * Model of a phone-number-based messenger action shown in the Google-style
 * "Connected apps" section on the contact-detail screen.
 *
 * A given messenger can contribute multiple actions (WhatsApp: chat + call +
 * video call). Each action becomes one row when its owning app is installed.
 */
data class MessengerAction(
    val id: String,
    val packageName: String,
    val labelRes: Int,
    val actionKind: Kind,
    val uriBuilder: (String) -> Uri,
) {
    enum class Kind { CHAT, CALL_VOICE, CALL_VIDEO }
}

object MessengerLauncher {

    // Package names for the messengers we can wire to a phone number. Order
    // in ACTIONS controls the display order on the contact-detail screen.
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    private const val TELEGRAM = "org.telegram.messenger"
    private const val TELEGRAM_FOSS = "org.telegram.messenger.web"
    private const val SIGNAL = "org.thoughtcrime.securesms"
    private const val VIBER = "com.viber.voip"
    private const val SKYPE = "com.skype.raider"

    // Everything the launcher can offer if the app is installed.
    val ACTIONS: List<MessengerAction> = listOf(
        MessengerAction(
            id = "whatsapp_chat",
            packageName = WHATSAPP,
            labelRes = R.string.dsremo_whatsapp_chat,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { digits -> Uri.parse("https://api.whatsapp.com/send?phone=$digits") },
        ),
        MessengerAction(
            id = "whatsapp_call",
            packageName = WHATSAPP,
            labelRes = R.string.dsremo_whatsapp_call,
            actionKind = MessengerAction.Kind.CALL_VOICE,
            uriBuilder = { digits -> Uri.parse("https://api.whatsapp.com/send?phone=$digits") },
        ),
        MessengerAction(
            id = "whatsapp_business_chat",
            packageName = WHATSAPP_BUSINESS,
            labelRes = R.string.dsremo_whatsapp_business_chat,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { digits -> Uri.parse("https://api.whatsapp.com/send?phone=$digits") },
        ),
        MessengerAction(
            id = "telegram_chat",
            packageName = TELEGRAM,
            labelRes = R.string.dsremo_telegram_chat,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { digits -> Uri.parse("tg://resolve?phone=$digits") },
        ),
        MessengerAction(
            id = "telegram_foss_chat",
            packageName = TELEGRAM_FOSS,
            labelRes = R.string.dsremo_telegram_foss_chat,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { digits -> Uri.parse("tg://resolve?phone=$digits") },
        ),
        MessengerAction(
            id = "signal_message",
            packageName = SIGNAL,
            labelRes = R.string.dsremo_signal_message,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { plusDigits -> Uri.parse("smsto:$plusDigits") },
        ),
        MessengerAction(
            id = "viber_chat",
            packageName = VIBER,
            labelRes = R.string.dsremo_viber_chat,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { plusDigits -> Uri.parse("viber://chat?number=$plusDigits") },
        ),
        MessengerAction(
            id = "skype_chat",
            packageName = SKYPE,
            labelRes = R.string.dsremo_skype_chat,
            actionKind = MessengerAction.Kind.CHAT,
            uriBuilder = { plusDigits -> Uri.parse("skype:$plusDigits?chat") },
        ),
        MessengerAction(
            id = "skype_call",
            packageName = SKYPE,
            labelRes = R.string.dsremo_skype_call,
            actionKind = MessengerAction.Kind.CALL_VOICE,
            uriBuilder = { plusDigits -> Uri.parse("skype:$plusDigits?call") },
        ),
    )

    /** Actions to render, filtered to the messengers actually installed. */
    fun installedActions(context: Context): List<MessengerAction> {
        return ACTIONS.filter { isInstalled(context, it.packageName) }
    }

    fun appIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun launch(activity: Activity?, action: MessengerAction, rawNumber: String) {
        activity ?: return
        val digits = rawNumber.filter { it == '+' || it.isDigit() }
        if (digits.isEmpty()) return
        val e164 = if (digits.startsWith("+")) digits else "+$digits"
        val digitsNoPlus = e164.removePrefix("+")
        // Signal / Viber / Skype expect the leading `+`. WhatsApp / Telegram
        // wa.me and tg://resolve expect no leading `+`. Route accordingly.
        val payload = when (action.packageName) {
            WHATSAPP, WHATSAPP_BUSINESS, TELEGRAM, TELEGRAM_FOSS -> digitsNoPlus
            else -> e164
        }
        val uri = action.uriBuilder(payload)
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(action.packageName)
        // Signal accepts smsto: via ACTION_SENDTO.
        if (uri.scheme == "smsto") {
            intent.action = Intent.ACTION_SENDTO
            intent.putExtra("sms_body", "")
        }
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.dsremo_app_not_installed, Toast.LENGTH_SHORT).show()
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
