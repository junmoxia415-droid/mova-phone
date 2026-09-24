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
import com.studiolexair.movaphone.data.messages.provider.SmsIntentFactory
import com.studiolexair.movaphone.data.messages.source.SmsProviderWriter
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
import com.studiolexair.movaphone.data.automation.geofence.GeofenceMonitor
import com.studiolexair.movaphone.feature.assistant.ai.AssistantPreferences
import com.studiolexair.movaphone.feature.assistant.ai.LocalModelManager
import com.studiolexair.movaphone.data.automation.geofence.GeofenceServiceDependencies
import com.studiolexair.movaphone.data.automation.geofence.GeofenceStore
import com.studiolexair.movaphone.data.automation.worker.GeofenceCheckWorker
import com.studiolexair.movaphone.data.location.model.LocationResult
import com.studiolexair.movaphone.services.calls.CallSessionHolder
import com.studiolexair.movaphone.services.sms.SmsServiceDependencies
import com.studiolexair.movaphone.services.wear.CallSessionHolderBridge
import com.studiolexair.movaphone.services.wear.WearBridge
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

    /** Contexto de aplicación para los componentes que lo necesitan (modelo local, ajustes). */
    val applicationContext: Context get() = context.applicationContext

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
    /** Escritura en el proveedor del sistema (necesaria si MOVA es la app de mensajes). */
    val providerWriter = SmsProviderWriter(context)

    val messageRepository = MessageRepositoryImpl(
        messageDao = database.messageDao(),
        smsProvider = SmsMessageProvider(context),
        deviceSms = DeviceSmsDataSource(context),
        intentFactory = SmsIntentFactory(context),
        providerWriter = providerWriter
    )
    val templateRepository = SmsTemplateRepositoryImpl(database.messageDao())
    val locationRepository = LocationRepositoryImpl(AndroidLocationDataSource(context), database.locationDao())
    val emergencyRepository = EmergencyRepositoryImpl(database.emergencyContactDao(), database.locationDao())
    val automationRepository = AutomationRepositoryImpl(database.automationDao())
    /** Lugares guardados (geovallas propias, sin Google Play Services). */
    val geofenceStore = GeofenceStore(context)
    /** Modelo de lenguaje en el teléfono + preferencias del asistente (sin nube). */
    val localModelManager = LocalModelManager(context)
    val assistantPreferences = AssistantPreferences(context)
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

    /**
     * Vigilancia de lugares: entra o sale de «Casa», «Trabajo»… y dispara las reglas.
     * La posición la da el propio teléfono (sin Play Services) y la decisión de entrar o
     * salir se calcula aquí, en el dispositivo.
     */
    val geofenceMonitor = GeofenceMonitor(
        store = geofenceStore,
        engine = automationEngine,
        location = {
            when (val result = locationRepository.currentLocation()) {
                is LocationResult.Available -> result.fix.latitude to result.fix.longitude
                is LocationResult.Unavailable -> null
            }
        }
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
            // El número puede llegar vacío en llamadas entrantes ocultas.
            if (!number.isNullOrBlank()) {
                val name = contactNameCache.resolve(number)
                // El reloj Wear OS también recibe el aviso (Bluetooth directo, sin Google).
                WearBridge.notifyIncomingCall(number, name)
                automationEngine.onTrigger(
                    com.studiolexair.movaphone.domain.automation.model.TriggerType.CALL_INCOMING,
                    com.studiolexair.movaphone.domain.automation.repository.TriggerPayload(
                        number = number,
                        contactName = name
                    )
                )
            }
        }

        // Órdenes del reloj: contestar, colgar, responder y SOS (confirmado en la muñeca).
        CallSessionHolderBridge.answer = { CallSessionHolder.state.value.primary?.let { CallSessionHolder.answer(it.id) } }
        CallSessionHolderBridge.hangUp = { CallSessionHolder.state.value.primary?.let { CallSessionHolder.hangUp(it.id) } }
        CallSessionHolderBridge.sos = { scope.launch { runCatching { sosOrchestrator.start(trigger = "wear") } } }
        CallSessionHolderBridge.reply = { to, body ->
            scope.launch {
                runCatching {
                    messageRepository.sendMessage(
                        address = to,
                        normalizedAddress = PhoneNumbers.normalize(to),
                        body = body,
                        contactName = contactNameCache.resolve(to)
                    )
                }
            }
        }

        SmsServiceDependencies.messageRepository = messageRepository
        SmsServiceDependencies.onSmsReceived = { address, name, body, at ->
            WearBridge.notifyMessage(address = address, name = name ?: contactNameCache.resolve(address), body = body, at = at)
        }
        SmsServiceDependencies.providerWriter = providerWriter
        SmsServiceDependencies.notifier = notifications
        SmsServiceDependencies.automationEngine = automationEngine
        SmsServiceDependencies.contactNameResolver = { number -> contactNameCache.resolve(number) }

        LocationServiceDependencies.locationRepository = locationRepository
        LocationDepsHolder.locationHistoryEnabled = { settingsSnapshot.storeLocationHistory }

        EmergencyServiceDependencies.sosOrchestrator = sosOrchestrator
        AutomationWorkerDependencies.engine = automationEngine
        GeofenceServiceDependencies.monitor = geofenceMonitor
        AutomationWorkerDependencies.batteryReader = { batteryReader.batteryPercent() }

        // Pantalla de llamada propia: cuando Telecom entrega una llamada, MOVA la muestra.
        CallServiceDependencies.onShowInCallUi = {
            val intent = android.content.Intent(context, com.studiolexair.movaphone.feature.incall.InCallActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            runCatching { context.startActivity(intent) }
        }
        CallServiceDependencies.onHideInCallUi = {
            MovaLog.i(TAG, "Llamadas finalizadas: la pantalla de llamada se cierra sola")
        }

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
        // Los lugares se comprueban cada 15 minutos aunque la app esté cerrada, y también
        // cada vez que llega una posición nueva mientras MOVA está viva.
        GeofenceCheckWorker.schedule(context)
        BackgroundLocationWorker.schedule(context)
        scope.launch {
            // Primera comprobación al abrir: recupera el estado si el teléfono estuvo apagado.
            runCatching { geofenceMonitor.check() }
        }
        scope.launch {
            while (true) {
                batteryReader.batteryPercent()?.let { runCatching { WearBridge.notifyBattery(it) } }
                kotlinx.coroutines.delay(5 * 60 * 1000L)
            }
        }
        scope.launch { templateRepository.ensureDefaults() }
        scope.launch { runCatching { contactsRepository.importFromDevice() } }
        scope.launch { runCatching { callsRepository.enrichFromDevice() } }
    }

    private companion object {
        const val TAG = "MovaContainer"
    }
}
