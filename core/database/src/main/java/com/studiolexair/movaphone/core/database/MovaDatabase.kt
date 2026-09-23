package com.studiolexair.movaphone.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.studiolexair.movaphone.core.database.dao.AutomationDao
import com.studiolexair.movaphone.core.database.dao.CallRecordDao
import com.studiolexair.movaphone.core.database.dao.ContactDao
import com.studiolexair.movaphone.core.database.dao.EmergencyContactDao
import com.studiolexair.movaphone.core.database.dao.LocationDao
import com.studiolexair.movaphone.core.database.dao.MessageDao
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.core.database.entity.AutomationExecutionEntity
import com.studiolexair.movaphone.core.database.entity.AutomationRuleEntity
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.database.entity.CallRecordEntity
import com.studiolexair.movaphone.core.database.entity.ContactEntity
import com.studiolexair.movaphone.core.database.entity.EmergencyContactEntity
import com.studiolexair.movaphone.core.database.entity.LocationRecordEntity
import com.studiolexair.movaphone.core.database.entity.MessageEntity
import com.studiolexair.movaphone.core.database.entity.SecurityEventEntity
import com.studiolexair.movaphone.core.database.entity.SmsTemplateEntity
import com.studiolexair.movaphone.core.database.entity.SpamRuleEntity
import com.studiolexair.movaphone.core.database.entity.TrustedContactEntity

/**
 * Base de datos local (Local First).
 * Ningún dato personal sale del dispositivo: la sincronización futura se hará a través de
 * SyncEngine (ver docs/ARQUITECTURA.md) sin cambiar el esquema actual.
 */
@Database(
    entities = [
        ContactEntity::class,
        EmergencyContactEntity::class,
        BlockedNumberEntity::class,
        SpamRuleEntity::class,
        CallRecordEntity::class,
        MessageEntity::class,
        SmsTemplateEntity::class,
        AutomationRuleEntity::class,
        AutomationExecutionEntity::class,
        LocationRecordEntity::class,
        SecurityEventEntity::class,
        TrustedContactEntity::class
    ],
    version = MovaDatabase.VERSION,
    exportSchema = true
)
abstract class MovaDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun callRecordDao(): CallRecordDao
    abstract fun messageDao(): MessageDao
    abstract fun securityDao(): SecurityDao
    abstract fun automationDao(): AutomationDao
    abstract fun locationDao(): LocationDao

    companion object {
        const val VERSION = 1
        const val NAME = "mova_phone.db"
    }
}
