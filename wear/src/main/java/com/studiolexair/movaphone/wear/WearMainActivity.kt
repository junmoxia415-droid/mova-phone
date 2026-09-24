package com.studiolexair.movaphone.wear

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

/**
 * Aplicación del reloj (Wear OS) de MOVA Phone.
 *
 * Muestra en la muñeca lo mismo que importa del teléfono —quién llama, quién ha escrito y
 * la batería— y permite contestar, colgar, responder con un mensaje corto y lanzar el SOS
 * (siempre confirmándolo antes). La conexión es por Bluetooth directo, sin Google Play
 * Services y sin nube.
 */
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearApp()
            }
        }
    }
}

private val QUICK_REPLIES = listOf("Voy en camino", "Vale", "Llámame")

@Composable
private fun WearApp() {
    val context = LocalContext.current
    val client = remember { WearBridgeClient(context) }
    val state by client.state.collectAsStateWithLifecycle()
    var sosConfirming by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { client.start() }

    LaunchedEffect(Unit) {
        if (client.hasPermissions()) client.start() else requestBluetooth(permissionLauncher)
    }
    DisposableEffect(Unit) { onDispose { client.stop() } }

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Card(onClick = { client.start() }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "MOVA Phone",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = state.detail, style = MaterialTheme.typography.body2)
                    state.phone.battery?.let { battery ->
                        Text(text = "Batería del teléfono: $battery %", style = MaterialTheme.typography.body2)
                    }
                    if (!state.connected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        CompactChip(onClick = { client.start() }, label = { Text("Buscar el teléfono") })
                    }
                }
            }

            state.phone.call?.let { call ->
                Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (call.ringing) "Llamada entrante" else "Llamada en curso",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = call.name, style = MaterialTheme.typography.body1)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (call.ringing) {
                            Button(
                                onClick = { client.sendCommand(WearProtocol.answer()) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Contestar") }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { client.sendCommand(WearProtocol.reject()) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Rechazar") }
                        } else {
                            Button(
                                onClick = { client.sendCommand(WearProtocol.hangUp()) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Colgar") }
                        }
                    }
                }
            }

            state.phone.messages.firstOrNull()?.let { message ->
                Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Te ha escrito ${message.name}",
                            style = MaterialTheme.typography.title3,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = message.body, style = MaterialTheme.typography.body2)
                        Spacer(modifier = Modifier.height(6.dp))
                        QUICK_REPLIES.forEach { reply ->
                            CompactChip(
                                onClick = { client.sendCommand(WearProtocol.reply(message.from, reply)) },
                                label = { Text(reply) }
                            )
                        }
                    }
                }
            }

            if (state.phone.messages.size > 1) {
                Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Mensajes anteriores", style = MaterialTheme.typography.title3)
                        state.phone.messages.drop(1).take(3).forEach { previous ->
                            Text(
                                text = "${previous.name}: ${previous.body}",
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
            }

            Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (!sosConfirming) {
                        Text("Emergencia", style = MaterialTheme.typography.title3, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Enviar tu ubicación a tus contactos de emergencia",
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = { sosConfirming = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("SOS")
                        }
                    } else {
                        Text("¿Confirmas el SOS?", style = MaterialTheme.typography.title3, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                client.sendCommand(WearProtocol.sos())
                                sosConfirming = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Sí, activar el SOS") }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { sosConfirming = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar")
                        }
                    }
                }
            }

            state.lastSent?.let {
                Text(
                    text = "Última orden enviada al teléfono",
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

/** Pide los permisos de Bluetooth que necesita el reloj (Android 12 o superior; antes, ubicación). */
private fun requestBluetooth(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    launcher.launch(permissions)
}
