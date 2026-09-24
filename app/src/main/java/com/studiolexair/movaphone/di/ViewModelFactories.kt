package com.studiolexair.movaphone.di

import com.studiolexair.movaphone.feature.assistant.SmartAssistantViewModel
import com.studiolexair.movaphone.feature.automation.AutomationViewModel
import com.studiolexair.movaphone.feature.automation.PlacesViewModel
import com.studiolexair.movaphone.feature.calls.CallsViewModel
import com.studiolexair.movaphone.feature.contacts.ContactsViewModel
import com.studiolexair.movaphone.feature.dialer.DialerViewModel
import com.studiolexair.movaphone.feature.emergency.EmergencyViewModel
import com.studiolexair.movaphone.feature.home.HomeViewModel
import com.studiolexair.movaphone.feature.location.LocationViewModel
import com.studiolexair.movaphone.feature.messages.MessagesViewModel
import com.studiolexair.movaphone.feature.security.SecurityViewModel
import com.studiolexair.movaphone.feature.settings.SettingsViewModel
import com.studiolexair.movaphone.domain.calls.usecase.FilterCallLogUseCase

/**
 * Fábricas de ViewModel expuestas por el contenedor.
 * Cada pantalla pide la suya (`viewModel(factory = container.homeFactory)`), de modo
 * que la UI nunca construye dependencias ni conoce implementaciones concretas.
 */
val MovaContainer.homeFactory get() = HomeViewModel.factory(
    callsRepository = callsRepository,
    contactsRepository = contactsRepository,
    emergencyRepository = emergencyRepository,
    telephonyAvailable = capabilities.hasTelephony
)

val MovaContainer.dialerFactory get() = DialerViewModel.factory(
    contactsRepository = contactsRepository,
    placeCall = placeCall,
    saveContact = saveContact,
    classifyNumber = classifyNumber,
    blockNumber = blockNumber
)

val MovaContainer.callsFactory get() = CallsViewModel.factory(
    callsRepository = callsRepository,
    contactsRepository = contactsRepository,
    filterUseCase = FilterCallLogUseCase(),
    onMarkSpam = markSpam,
    onBlockNumber = blockNumber
)

val MovaContainer.contactsFactory get() = ContactsViewModel.factory(
    repository = contactsRepository,
    saveContact = saveContact,
    deleteContact = deleteContact,
    toggleFavorite = toggleFavorite
)

val MovaContainer.emergencyFactory get() = EmergencyViewModel.factory(
    repository = emergencyRepository,
    orchestrator = sosOrchestrator,
    saveContact = saveEmergencyContact,
    deleteContact = deleteEmergencyContact,
    reorder = reorderEmergencyContacts
)

val MovaContainer.messagesFactory get() = MessagesViewModel.factory(
    messageRepository = messageRepository,
    templateRepository = templateRepository,
    locationRepository = locationRepository
)

val MovaContainer.locationFactory get() = LocationViewModel.factory(locationRepository)

val MovaContainer.automationFactory get() = AutomationViewModel.factory(
    repository = automationRepository,
    engine = automationEngine,
    saveRule = saveAutomationRule,
    toggleRule = toggleAutomationRule,
    templates = automationTemplates
)

val MovaContainer.placesFactory get() = PlacesViewModel.factory(
    store = geofenceStore,
    monitor = geofenceMonitor,
    locationRepository = locationRepository
)

val MovaContainer.securityFactory get() = SecurityViewModel.factory(
    securityDao = database.securityDao(),
    settingsStore = settingsStore,
    pinManager = pinManager,
    biometricManager = biometricManager,
    eventLogger = securityEventLogger
)

val MovaContainer.settingsFactory get() = SettingsViewModel.factory(settingsStore)

val MovaContainer.assistantFactory get() = SmartAssistantViewModel.factory(
    context = applicationContext,
    contactsRepository = contactsRepository,
    placeCall = placeCall,
    messageRepository = messageRepository,
    locationRepository = locationRepository,
    sosOrchestrator = sosOrchestrator,
    modelManager = localModelManager,
    preferences = assistantPreferences
)
