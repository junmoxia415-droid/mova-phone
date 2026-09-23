package com.studiolexair.movaphone.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.core.navigation.MovaRoutes
import com.studiolexair.movaphone.core.navigation.TopLevelDestination
import com.studiolexair.movaphone.core.security.settings.MovaSettings
import com.studiolexair.movaphone.di.MovaContainer
import kotlinx.coroutines.launch

/**
 * Anfitrión de la aplicación: tema, splash, barra inferior del mockup, grafo de
 * navegación, asistente de permisos del primer uso y puerta de bloqueo si el usuario
 * protege MOVA Phone con PIN o biometría.
 */
@Composable
fun MovaApp(container: MovaContainer) {
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = MovaSettings.DEFAULT)
    val navController = rememberNavController()
    val navigator = remember(navController) { MovaNavigator(navController) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }

    val openSystemSettings: () -> Unit = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
    LaunchedEffect(Unit) { navigator.onOpenSystemSettings = openSystemSettings }

    // El bloqueo se reactiva cuando la app vuelve del segundo plano.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> container.appLockController.onAppBackgrounded()
                Lifecycle.Event.ON_START -> container.appLockController.onAppForegrounded(settings.lockTimeoutSeconds)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MovaTheme(
        darkTheme = if (settings.followSystemTheme) isSystemInDarkTheme() else settings.darkTheme
    ) {
        val currentEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentEntry?.destination?.route
        val topLevelRoutes = TopLevelDestination.values().map { it.route }
        val showBottomBar = currentRoute in topLevelRoutes

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        MovaBottomBar(currentRoute = currentRoute, onSelect = navigator::toTopLevel)
                    }
                }
            ) { padding ->
                MovaNavHost(
                    navController = navController,
                    container = container,
                    navigator = navigator,
                    startDestination = MovaRoutes.HOME,
                    settings = settings,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            // Pantalla de inicio de marca (mockup) mientras arranca la infraestructura.
            if (showSplash) {
                SplashScreen(onReady = { showSplash = false })
            }

            // Primer uso: se piden los permisos necesarios con su explicación.
            if (!showSplash && !settings.onboardingCompleted) {
                PermissionsScreen(
                    navigator = navigator,
                    firstRun = true,
                    onFinish = { scope.launch { container.settingsStore.setOnboardingCompleted(true) } },
                    onOpenSystemSettings = openSystemSettings
                )
            }
        }

        AppLockGate(container = container, settings = settings)
    }
}

/** Barra inferior con los cuatro destinos del mockup. */
@Composable
private fun MovaBottomBar(currentRoute: String?, onSelect: (TopLevelDestination) -> Unit) {
    NavigationBar {
        TopLevelDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onSelect(destination) },
                icon = { Icon(imageVector = destination.icon(), contentDescription = null) },
                label = { Text(destination.label) }
            )
        }
    }
}

private fun TopLevelDestination.icon(): ImageVector = when (this) {
    TopLevelDestination.HOME -> Icons.Filled.Home
    TopLevelDestination.CONTACTS -> Icons.Filled.Contacts
    TopLevelDestination.CALLS -> Icons.Filled.Call
    TopLevelDestination.MORE -> Icons.Filled.MoreHoriz
}
