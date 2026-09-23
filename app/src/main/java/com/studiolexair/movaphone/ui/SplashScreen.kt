package com.studiolexair.movaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.studiolexair.movaphone.core.designsystem.branding.BrandConfig
import com.studiolexair.movaphone.core.designsystem.branding.MovaLogoLockup
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import kotlinx.coroutines.delay

/**
 * Pantalla de bienvenida del mockup: marca, eslogan y arranque de la infraestructura.
 * Dura lo justo para que el usuario vea la identidad de MOVA Phone.
 */
@Composable
fun SplashScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1_200)
        onReady()
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MovaDimens.spaceXl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MovaLogoLockup(showTagline = true, markSize = MovaDimens.logoMarkSize)
            Text(
                text = BrandConfig.SHORT_TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = MovaTheme.extra.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MovaDimens.spaceLg)
            )
            Text(
                text = "Cargando…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = MovaDimens.spaceXxl)
            )
            Text(
                text = BrandConfig.DEVELOPER_CREDIT,
                style = MaterialTheme.typography.labelSmall,
                color = MovaTheme.extra.textMuted,
                modifier = Modifier.padding(top = MovaDimens.spaceMd)
            )
        }
    }
}
