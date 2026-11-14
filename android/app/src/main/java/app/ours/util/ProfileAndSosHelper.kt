package app.ours.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import java.security.MessageDigest

object ProfileAndSosHelper {

    fun gravatarUrl(email: String?): String? {
        val clean = email?.trim()?.lowercase()?.takeIf { it.contains("@") } ?: return null
        val digest = MessageDigest.getInstance("MD5").digest(clean.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "https://www.gravatar.com/avatar/$hex?d=404&s=160"
    }

    fun resolveAvatarUrl(avatarUrl: String?, email: String?, localPhotoUri: String? = null): String? {
        if (!avatarUrl.isNullOrBlank()) return avatarUrl
        if (!localPhotoUri.isNullOrBlank()) return localPhotoUri
        return gravatarUrl(email)
    }

    fun queryLocalProfilePhotoUri(context: Context): String? = runCatching {
        context.contentResolver.query(
            ContactsContract.Profile.CONTENT_URI,
            arrayOf(ContactsContract.Profile.PHOTO_URI, ContactsContract.Profile.PHOTO_THUMBNAIL_URI),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(1) ?: cursor.getString(0)
            } else {
                null
            }
        }
    }.getOrNull()

    fun launchWhatsAppCall(context: Context, rawPhone: String): Boolean {
        val digits = rawPhone.filter { it.isDigit() }.let { d ->
            if (d.startsWith("0") && d.length >= 9) "62" + d.drop(1) else d
        }
        if (digits.isBlank()) return false

        // Try direct WhatsApp VoIP call MIME if contact ID is resolvable, else wa.me deep link
        val waCallIntent = runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(rawPhone.trim()),
            )
            var dataId: Long? = null
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val contactId = c.getLong(0)
                    context.contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.Data._ID),
                        "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(contactId.toString(), "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"),
                        null,
                    )?.use { dc ->
                        if (dc.moveToFirst()) dataId = dc.getLong(0)
                    }
                }
            }
            dataId?.let { id ->
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$id"),
                        "vnd.android.cursor.item/vnd.com.whatsapp.voip.call",
                    )
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }.getOrNull()

        if (waCallIntent != null && runCatching { context.startActivity(waCallIntent) }.isSuccess) {
            return true
        }

        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(fallback) }.isSuccess
    }
}
