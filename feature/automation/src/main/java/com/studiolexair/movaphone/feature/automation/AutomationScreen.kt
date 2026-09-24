package com.studiolexair.movaphone.feature.automation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studiolexair.movaphone.core.common.util.TextFormatters
import com.studiolexair.movaphone.core.designsystem.component.AuroraBackground
import com.studiolexair.movaphone.core.designsystem.component.MovaEmptyState
import com.studiolexair.movaphone.core.designsystem.component.MovaInfoBanner
import com.studiolexair.movaphone.core.designsystem.component.MovaListRow
import com.studiolexair.movaphone.core.designsystem.component.MovaPrimaryButton
import com.studiolexair.movaphone.core.designsystem.component.MovaScreenHeader
import com.studiolexair.movaphone.core.designsystem.component.PillTone
import com.studiolexair.movaphone.core.designsystem.theme.MovaDimens
import com.studiolexair.movaphone.core.designsystem.theme.MovaTheme
import com.studiolexair.movaphone.core.navigation.MovaNavigator
import com.studiolexair.movaphone.domain.automation.model.AutomationRule

/** Lista de reglas con interruptor, "probar ahora" e historial de ejecuciones. */
@Composable
fun AutomationRoute(
    navigator: MovaNavigator,
    viewModel: AutomationViewModel,
    modifier: Modifier = Modifier
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()

    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(
                title = "Automatizaciones",
                subtitle = "Reglas inteligentes para tu día a día",
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = "Lugares guardados",
                        tint = MovaTheme.extra.violet,
                        modifier = Modifier.padding(end = MovaDimens.spaceSm)
                            .clickable { navigator.toAutomationPlaces() }
                    )
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Historial",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = MovaDimens.spaceSm)
                            .clickable { navigator.toAutomationHistory() }
                    )
                }
            )

            message?.let { MovaInfoBanner(message = it, tone = PillTone.Brand, modifier = Modifier.padding(MovaDimens.spaceLg)) }

            Row(
                modifier = Modifier.padding(horizontal = MovaDimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
            ) {
                AssistChip(onClick = { navigator.toAutomationEditor(null) }, label = { Text("Nueva regla") })
                AssistChip(onClick = { navigator.toAutomationHistory() }, label = { Text("Historial") })
                AssistChip(onClick = { navigator.toAutomationPlaces() }, label = { Text("Lugares") })
            }

            if (rules.isEmpty()) {
                MovaEmptyState(
                    title = "Sin automatizaciones",
                    description = "Crea reglas como «si activo SOS, compartir ubicación» o «si estoy conduciendo, activar modo conducción».",
                    icon = Icons.Filled.Bolt,
                    action = {
                        MovaPrimaryButton(
                            text = "Crear automatización",
                            icon = Icons.Filled.Add,
                            onClick = { navigator.toAutomationEditor(null) }
                        )
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(MovaDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        AutomationRow(
                            rule = rule,
                            onToggle = { enabled -> viewModel.toggle(rule, enabled) },
                            onRun = { viewModel.runNow(rule) },
                            onEdit = { navigator.toAutomationEditor(rule.id) },
                            onDelete = { viewModel.delete(rule) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationRow(
    rule: AutomationRule,
    onToggle: (Boolean) -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceXs)) {
        MovaListRow(
            title = rule.name,
            subtitle = "SI ${rule.trigger.type.label} → ${rule.action.type.label}" +
                (rule.condition?.takeIf { it.type.name != "ALWAYS" }?.let { " · SI ${it.type.label}" } ?: ""),
            leading = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = MovaTheme.extra.violet) },
            trailing = { Switch(checked = rule.enabled, onCheckedChange = onToggle) },
            onClick = onEdit
        )
        Row(
            modifier = Modifier.padding(start = MovaDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MovaDimens.spaceLg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onRun)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Probar", tint = MovaTheme.extra.success)
                Text("Probar ahora", style = MaterialTheme.typography.labelSmall, color = MovaTheme.extra.textSecondary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onDelete)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MovaTheme.extra.danger)
                Text("Eliminar", style = MaterialTheme.typography.labelSmall, color = MovaTheme.extra.textSecondary)
            }
        }
    }
}

/** Historial de ejecuciones (auditoría del motor de automatización). */
@Composable
fun AutomationHistoryRoute(
    viewModel: AutomationViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    AuroraBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            MovaScreenHeader(title = "Historial de automatizaciones", subtitle = "Qué se ejecutó y con qué resultado")
            if (history.isEmpty()) {
                MovaEmptyState(
                    title = "Sin ejecuciones",
                    description = "Cuando una regla se ejecute verás aquí el resultado real.",
                    icon = Icons.Filled.History
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(MovaDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceSm)
                ) {
                    items(history.size) { index ->
                        val outcome = history[index]
                        MovaListRow(
                            title = outcome.ruleName,
                            subtitle = (outcome.detail ?: "sin detalle") + " · " + (if (outcome.success) "correcto" else "con error"),
                            accent = if (outcome.success) MovaTheme.extra.success else MovaTheme.extra.danger
                        )
                    }
                }
            }
        }
    }
}

/** Editor de reglas: disparador, condición y acción con validación del dominio. */
@Composable
fun AutomationEditorRoute(
    ruleId: Long?,
    navigator: MovaNavigator,
    viewModel: AutomationViewModel,
    modifier: Modifier = Modifier
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val existing = rules.firstOrNull { it.id == ruleId }
    AutomationEditorScreen(existing = existing, viewModel = viewModel, navigator = navigator, modifier = modifier)
}

@Composable
private fun AutomationEditorScreen(
    existing: AutomationRule?,
    viewModel: AutomationViewModel,
    navigator: MovaNavigator,
    modifier: Modifier = Modifier
) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(existing?.name.orEmpty()) }
    var trigger by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(existing?.trigger?.type ?: viewModel.availableTriggers().first())
    }
    var condition by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(existing?.condition?.type ?: com.studiolexair.movaphone.domain.automation.model.ConditionType.ALWAYS)
    }
    var action by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(existing?.action?.type ?: viewModel.availableActions().first())
    }
    var value by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(existing?.action?.value.orEmpty()) }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(MovaDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(MovaDimens.spaceMd)
        ) {
            item { MovaScreenHeader(title = if (existing == null) "Nueva automatización" else "Editar automatización") }

            item {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la regla") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("SI (disparador)", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.textMuted)
                TriggerPicker(selected = trigger, options = viewModel.availableTriggers()) { trigger = it }
            }

            item {
                Text("Y ADEMÁS (condición)", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.textMuted)
                ConditionPicker(selected = condition, options = viewModel.availableConditions()) { condition = it }
            }

            item {
                Text("ENTONCES (acción)", style = MaterialTheme.typography.labelMedium, color = MovaTheme.extra.textMuted)
                ActionPicker(selected = action, options = viewModel.availableActions()) { action = it }
            }

            item {
                androidx.compose.material3.OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Valor o destinatario (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Las acciones sensibles (llamadas, SMS, SOS) se ejecutan con las APIs reales del dispositivo y quedan registradas en el historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MovaTheme.extra.textSecondary
                )
            }

            item {
                MovaPrimaryButton(
                    text = "Guardar automatización",
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.save(
                            ruleId = existing?.id ?: 0L,
                            name = name,
                            trigger = trigger,
                            triggerValue = null,
                            condition = condition,
                            conditionValue = null,
                            action = action,
                            actionValue = value
                        )
                        navigator.back()
                    }
                )
            }
        }
    }
}

@Composable
private fun TriggerPicker(
    selected: com.studiolexair.movaphone.domain.automation.model.TriggerType,
    options: List<com.studiolexair.movaphone.domain.automation.model.TriggerType>,
    onSelect: (com.studiolexair.movaphone.domain.automation.model.TriggerType) -> Unit
) {
    Column {
        options.forEach { option ->
            MovaListRow(
                title = option.label,
                trailing = { if (option == selected) Text("✓", color = MovaTheme.extra.success) else null },
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun ConditionPicker(
    selected: com.studiolexair.movaphone.domain.automation.model.ConditionType,
    options: List<com.studiolexair.movaphone.domain.automation.model.ConditionType>,
    onSelect: (com.studiolexair.movaphone.domain.automation.model.ConditionType) -> Unit
) {
    Column {
        options.forEach { option ->
            MovaListRow(
                title = option.label,
                trailing = { if (option == selected) Text("✓", color = MovaTheme.extra.success) else null },
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun ActionPicker(
    selected: com.studiolexair.movaphone.domain.automation.model.ActionType,
    options: List<com.studiolexair.movaphone.domain.automation.model.ActionType>,
    onSelect: (com.studiolexair.movaphone.domain.automation.model.ActionType) -> Unit
) {
    Column {
        options.forEach { option ->
            MovaListRow(
                title = option.label,
                trailing = { if (option == selected) Text("✓", color = MovaTheme.extra.success) else null },
                onClick = { onSelect(option) }
            )
        }
    }
}
