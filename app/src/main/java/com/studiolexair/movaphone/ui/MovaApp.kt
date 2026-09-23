package com.studiolexair.movaphone.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Anfitrión de la aplicación: tema, barra inferior del mockup, grafo de navegación
 * y puerta de bloqueo cuando el usuario protege MOVA Phone con PIN o biometría.
 */
@Composable
fun MovaApp(container: MovaContainer) {
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = MovaSettings.DEFAULT)
    val navController = rememberNavController()
    val navigator = remember(navController) { MovaNavigator(navController) }

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
                startDestination = MovaRoutes.SPLASH,
                settings = settings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
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
