package com.studiolexair.movaphone.di

import android.content.Context
import com.studiolexair.movaphone.core.common.util.DeviceCapabilitiesReader
import com.studiolexair.movaphone.core.common.util.PhoneNumbers
import com.studiolexair.movaphone.core.database.DatabaseFactory
import com.studiolexair.movaphone.core.database.entity.BlockedNumberEntity
import com.studiolexair.movaphone.core.logging.MovaLog
import com.studiolexair.movaphone.core.permissions.PermissionChecker
import com.studiolexair.movaphone.core.security.biometric.BiometricAuthManager
import com.studiolexair.movaphone.core.security.crypto.CryptoManager
import com.studiolexair.movaphone.core.security.event.SecurityEventLogger
import com.studiolexair.movaphone.core.security.event.SecurityEventType
import com.studiolexair.movaphone.core.security.lock.AppLockController
import com.studiolexair.movaphone.core.security.pin.PinManager
import com.studiolexair.movaphone.core.security.settings.MovaSettingsStore
import com.studiolexair.movaphone.data.automation.engine.AutomationActionExecutor
import com.studiolexair.movaphone.data.automation.engine.AutomationConditionEvaluator
import com.studiolexair.movaphone.data.automation.engine.AutomationEngineImpl
import com.studiolexair.movaphone.data.automation.receiver.AutomationEventBridge
import com.studiolexair.movaphone.data.automation.receiver.SystemEventsRegistrar
import com.studiolexair.movaphone.data.automation.repository.AutomationRepositoryImpl
import com.studiolexair.movaphone.data.automation.worker.AutomationWorker
import com.studiolexair.movaphone.data.automation.worker.AutomationWorkerDependencies
import com.studiolexair.movaphone.data.calls.call.CallLauncherImpl
import com.studiolexair.movaphone.data.calls.repository.CallsRepositoryImpl
import com.studiolexair.movaphone.data.calls.source.DeviceCallLogDataSource
import com.studiolexair.movaphone.data.calls.spam.SpamClassifierImpl
import com.studiolexair.movaphone.data.contacts.repository.ContactsRepositoryImpl
import com.studiolexair.movaphone.data.contacts.source.DeviceContactsDataSource
import com.studiolexair.movaphone.data.emergency.orchestrator.SosOrchestratorImpl
import com.studiolexair.movaphone.data.emergency.repository.EmergencyRepositoryImpl
import com.studiolexair.movaphone.data.location.repository.LocationRepositoryImpl
import com.studiolexair.movaphone.data.location.source.AndroidLocationDataSource
import com.studiolexair.movaphone.data.messages.provider.SmsMessageProvider
import com.studiolexair.movaphone.data.messages.repository.MessageRepositoryImpl
import com.studiolexair.movaphone.data.messages.repository.SmsTemplateRepositoryImpl
import com.studiolexair.movaphone.data.messages.source.DeviceSmsDataSource
import com.studiolexair.movaphone.domain.automation.repository.AutomationEngine
import com.studiolexair.movaphone.domain.calls.usecase.MarkSpamUseCase
import com.studiolexair.movaphone.domain.calls.usecase.PlaceCallUseCase
import com.studiolexair.movaphone.domain.contacts.usecase.DeleteContactUseCase
import com.studiolexair.movaphone.domain.contacts.usecase.SaveContactUseCase
import com.studiolexair.movaphone.domain.contacts.usecase.ToggleFavoriteUseCase
import com.studiolexair.movaphone.domain.emergency.repository.SosOrchestrator
import com.studiolexair.movaphone.domain.emergency.usecase.DeleteEmergencyContactUseCase
import com.studiolexair.movaphone.domain.emergency.usecase.RenderEmergencySmsUseCase
import com.studiolexair.movaphone.domain.emergency.usecase.ReorderEmergencyContactsUseCase
import com.studiolexair.movaphone.domain.emergency.usecase.SaveEmergencyContactUseCase
import com.studiolexair.movaphone.domain.automation.usecase.AutomationTemplatesUseCase
import com.studiolexair.movaphone.domain.automation.usecase.SaveAutomationRuleUseCase
import com.studiolexair.movaphone.domain.automation.usecase.ToggleAutomationRuleUseCase
import com.studiolexair.movaphone.domain.automation.usecase.ValidateAutomationRuleUseCase
import com.studiolexair.movaphone.services.calls.CallServiceDependencies
import com.studiolexair.movaphone.services.location.BackgroundLocationWorker
import com.studiolexair.movaphone.services.location.LocationDepsHolder
import com.studiolexair.movaphone.services.location.LocationServiceDependencies
import com.studiolexair.movaphone.services.notifications.AutomationNotifierImpl
import com.studiolexair.movaphone.services.notifications.EmergencyAlertSinkImpl
import com.studiolexair.movaphone.services.notifications.EmergencyServiceDependencies
import com.studiolexair.movaphone.services.notifications.MovaNotificationChannels
import com.studiolexair.movaphone.services.notifications.MovaNotifications
import com.studiolexair.movaphone.services.sms.SmsServiceDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Contenedor de dependencias de MOVA Phone.
 *
 * Se construye una sola vez en [com.studiolexair.movaphone.MovaApplication] y se
 * reparte por constructor a los ViewModels (inyección explícita, sin reflexión:
 * se ve de un vistazo de qué depende cada capa) y por los puentes `*Dependencies`
 * a los componentes que Android instancia por sí mismo (servicios y receptores).
 */
class MovaContainer(
    private val context: Context,
    private val scope: CoroutineScope
) {

    // ---------- Infraestructura ----------
    val database = DatabaseFactory.create(context)
    val settingsStore = MovaSettingsStore(context)
    val cryptoManager = CryptoManager()
    val pinManager = PinManager(context, cryptoManager)
    val biometricManager = BiometricAuthManager(context)
    val securityEventLogger = SecurityEventLogger(database.securityDao())
    val appLockController = AppLockController()
    val permissionChecker = PermissionChecker(context)
    val capabilities = DeviceCapabilitiesReader.read(context)

    // ---------- Datos ----------
    val contactsRepository = ContactsRepositoryImpl(database.contactDao(), DeviceContactsDataSource(context))
    val callsRepository = CallsRepositoryImpl(database.callRecordDao(), DeviceCallLogDataSource(context))
    val spamClassifier = SpamClassifierImpl(database.securityDao())
    val messageRepository = MessageRepositoryImpl(
        messageDao = database.messageDao(),
        smsProvider = SmsMessageProvider(context),
        deviceSms = DeviceSmsDataSource(context)
    )
    val templateRepository = SmsTemplateRepositoryImpl(database.messageDao())
    val locationRepository = LocationRepositoryImpl(AndroidLocationDataSource(context), database.locationDao())
    val emergencyRepository = EmergencyRepositoryImpl(database.emergencyContactDao(), database.locationDao())
    val automationRepository = AutomationRepositoryImpl(database.automationDao())
    val callLauncher = CallLauncherImpl(context)

    // ---------- Servicios compartidos ----------
    val notifications = MovaNotifications(context)
    val automationNotifier = AutomationNotifierImpl(context)
    val contactNameCache = ContactNameCache(database.contactDao(), scope)
    val drivingModeController = SettingsDrivingModeController(settingsStore, scope)
    val clock = SystemAutomationClock()
    val batteryReader = BatteryReader(context)

    @Volatile
    private var automationsEnabled: Boolean = true

    @Volatile
    private var senderName: String = "Usuario"

    init {
        scope.launch {
            settingsStore.settings.collect { settings ->
                automationsEnabled = settings.automationsEnabled
                senderName = settings.userName
            }
        }
    }

    // ---------- Emergencias ----------
    val sosOrchestrator: SosOrchestrator = SosOrchestratorImpl(
        emergencyRepository = emergencyRepository,
        locationRepository = locationRepository,
        messageRepository = messageRepository,
        templateRepository = templateRepository,
        callLauncher = callLauncher,
        alertSink = EmergencyAlertSinkImpl(context),
        batteryReader = { batteryReader.batteryPercent() },
        onSecurityEvent = { description ->
            securityEventLogger.log(SecurityEventType.SOS_TRIGGERED, description)
        },
        renderSms = RenderEmergencySmsUseCase(),
        senderName = senderName
    )

    // ---------- Automatizaciones ----------
    private val conditionEvaluator = AutomationConditionEvaluator(clock, drivingModeController)

    private val actionExecutor = AutomationActionExecutor(
        messageRepository = messageRepository,
        callLauncher = callLauncher,
        locationRepository = locationRepository,
        notifier = automationNotifier,
        drivingMode = drivingModeController,
        sosOrchestrator = sosOrchestrator,
        contactDao = database.contactDao(),
        securityDao = database.securityDao(),
        onSecurityEvent = { description ->
            securityEventLogger.log(SecurityEventType.AUTOMATION_EXECUTED, description)
        }
    )

    val automationEngine: AutomationEngine = AutomationEngineImpl(
        repository = automationRepository,
        conditionEvaluator = conditionEvaluator,
        actionExecutor = actionExecutor,
        clock = clock,
        enabled = { automationsEnabled }
    )

    // ---------- Acciones reutilizadas por la UI ----------
    val placeCall = PlaceCallUseCase(callLauncher)
    val saveContact = SaveContactUseCase(contactsRepository)
    val deleteContact = DeleteContactUseCase(contactsRepository)
    val toggleFavorite = ToggleFavoriteUseCase(contactsRepository)
    val saveEmergencyContact = SaveEmergencyContactUseCase(emergencyRepository)
    val deleteEmergencyContact = DeleteEmergencyContactUseCase(emergencyRepository)
    val reorderEmergencyContacts = ReorderEmergencyContactsUseCase(emergencyRepository)
    val saveAutomationRule = SaveAutomationRuleUseCase(automationRepository, ValidateAutomationRuleUseCase())
    val toggleAutomationRule = ToggleAutomationRuleUseCase(automationRepository)
    val automationTemplates = AutomationTemplatesUseCase()

    /** Clasifica un número como spam usando las reglas locales y el historial. */
    val classifyNumber: suspend (String) -> com.studiolexair.movaphone.domain.calls.model.SpamVerdict = { number ->
        spamClassifier.classify(number)
    }

    /** Bloquea un número de verdad: se guarda y el filtrado lo rechazará. */
    val blockNumber: suspend (String) -> Unit = { number ->
        if (number.isNotBlank()) {
            database.securityDao().block(
                BlockedNumberEntity(
                    normalizedNumber = PhoneNumbers.normalize(number),
                    phoneNumber = number,
                    label = null,
                    createdAt = System.currentTimeMillis()
                )
            )
            securityEventLogger.log(SecurityEventType.NUMBER_BLOCKED, "Número bloqueado desde la aplicación")
        }
    }

    /** Marca un número como spam (regla local + historial). */
    val markSpam: suspend (String) -> Unit = { number ->
        MarkSpamUseCase(callsRepository, spamClassifier)(number)
        Unit
    }

    /**
     * Conecta el contenedor con los componentes que Android crea por su cuenta.
     * Sin esto, los servicios y receptores no tendrían datos ni motor de automatización.
     */
    fun wireSystemComponents() {
        CallServiceDependencies.securityDao = database.securityDao()
        CallServiceDependencies.callRecordDao = database.callRecordDao()
        CallServiceDependencies.spamClassifier = spamClassifier
        CallServiceDependencies.spamDetectionEnabled = { settingsSnapshot.spamDetectionEnabled }
        CallServiceDependencies.automationEngine = automationEngine
        CallServiceDependencies.contactNameResolver = { number -> contactNameCache.resolve(number) }
        CallServiceDependencies.onIncomingCall = { number ->
            automationEngine.onTrigger(
                com.studiolexair.movaphone.domain.automation.model.TriggerType.CALL_INCOMING,
                com.studiolexair.movaphone.domain.automation.repository.TriggerPayload(
                    number = number,
                    contactName = contactNameCache.resolve(number)
                )
            )
        }

        SmsServiceDependencies.messageRepository = messageRepository
        SmsServiceDependencies.notifier = notifications
        SmsServiceDependencies.automationEngine = automationEngine
        SmsServiceDependencies.contactNameResolver = { number -> contactNameCache.resolve(number) }

        LocationServiceDependencies.locationRepository = locationRepository
        LocationDepsHolder.locationHistoryEnabled = { settingsSnapshot.storeLocationHistory }

        EmergencyServiceDependencies.sosOrchestrator = sosOrchestrator
        AutomationWorkerDependencies.engine = automationEngine
        AutomationWorkerDependencies.batteryReader = { batteryReader.batteryPercent() }

        // Disparadores de sistema y de la propia app (conducción, SOS, desbloqueo):
        // el motor de automatizaciones recibe el 100% de los eventos que ofrece el editor.
        AutomationEventBridge.engine = automationEngine
        // Avisos de batería: broadcast *sticky*, se registra con la app viva (requisito de Android).
        SystemEventsRegistrar(context).register()

        MovaLog.i(TAG, "Componentes del sistema conectados con el contenedor de dependencias")
    }

    /** Preferencias cacheadas para las lambdas síncronas de los componentes del sistema. */
    @Volatile
    private var settingsSnapshot: com.studiolexair.movaphone.core.security.settings.MovaSettings =
        com.studiolexair.movaphone.core.security.settings.MovaSettings.DEFAULT

    /** Arranca los trabajos periódicos y los canales de notificación. */
    /** El desbloqueo de la app es un disparador de automatizaciones más (requisito 18). */
    fun onAppUnlocked() {
        AutomationEventBridge.fire(
            com.studiolexair.movaphone.domain.automation.model.TriggerType.APP_UNLOCKED
        )
    }

    fun startBackgroundWork() {
        MovaNotificationChannels.create(context)
        scope.launch {
            settingsStore.settings.collect { settingsSnapshot = it }
        }
        AutomationWorker.schedule(context)
        scope.launch {
            if (settingsSnapshot.storeLocationHistory) BackgroundLocationWorker.schedule(context)
        }
        scope.launch { templateRepository.ensureDefaults() }
        scope.launch { runCatching { contactsRepository.importFromDevice() } }
        scope.launch { runCatching { callsRepository.enrichFromDevice() } }
    }

    private companion object {
        const val TAG = "MovaContainer"
    }
}
