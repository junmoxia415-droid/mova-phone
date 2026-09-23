package com.studiolexair.movaphone.data.contacts.source

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.entity.ContactEntity
import com.studiolexair.movaphone.core.logging.MovaLog

/**
 * Lectura de la agenda del sistema mediante ContactsContract (API oficial, sólo lectura).
 * MOVA no modifica la agenda del usuario sin una acción explícita.
 */
class DeviceContactsDataSource(private val context: Context) {

    fun readAll(): List<ContactEntity> {
        val resolver: ContentResolver = context.contentResolver
        val now = System.currentTimeMillis()
        val results = mutableListOf<ContactEntity>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(projection[0])
                val nameIndex = cursor.getColumnIndexOrThrow(projection[1])
                val numberIndex = cursor.getColumnIndexOrThrow(projection[2])
                val photoIndex = cursor.getColumnIndexOrThrow(projection[3])
                val starredIndex = cursor.getColumnIndexOrThrow(projection[4])

                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIndex) ?: continue
                    val digits = PhoneNumbers.digitsOnly(number)
                    if (digits.isEmpty()) continue
                    results += ContactEntity(
                        deviceContactId = cursor.getString(idIndex),
                        displayName = cursor.getString(nameIndex) ?: PhoneNumbers.pretty(number),
                        phoneNumber = number,
                        normalizedNumber = PhoneNumbers.normalize(number),
                        photoUri = cursor.getString(photoIndex),
                        isFavorite = cursor.getInt(starredIndex) == 1,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
        } catch (security: SecurityException) {
            MovaLog.w(TAG, "Sin permiso para leer contactos del sistema")
            throw security
        } catch (t: Throwable) {
            MovaLog.e(TAG, "Fallo leyendo la agenda del sistema", t)
        }
        return results
    }

    private companion object {
        const val TAG = "DeviceContacts"
    }
}
