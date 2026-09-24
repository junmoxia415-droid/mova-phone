package com.studiolexair.movaphone.services.calls

import com.studiolexair.movaphone.core.database.dao.CallRecordDao
import com.studiolexair.movaphone.core.database.dao.SecurityDao
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.calls.repository.SpamClassifier

/**
 * Puente de dependencias para componentes que Android instancia por sí mismo
 * (servicios y receptores). El contenedor de la aplicación los rellena al arrancar;
 * es el patrón recomendado cuando no se puede usar inyección por constructor.
 */
object CallServiceDependencies {
    @Volatile var securityDao: SecurityDao? = null
    @Volatile var callRecordDao: CallRecordDao? = null
    @Volatile var spamClassifier: SpamClassifier? = null
    @Volatile var spamDetectionEnabled: (() -> Boolean)? = null
    @Volatile var automationEngine: AutomationEngine? = null
    @Volatile var contactNameResolver: ((String?) -> String?)? = null
    @Volatile var onIncomingCall: (suspend (String?) -> Unit)? = null

    /** La app abre su propia pantalla de llamada cuando Telecom entrega una llamada. */
    @Volatile var onShowInCallUi: (() -> Unit)? = null
    @Volatile var onHideInCallUi: (() -> Unit)? = null
}
