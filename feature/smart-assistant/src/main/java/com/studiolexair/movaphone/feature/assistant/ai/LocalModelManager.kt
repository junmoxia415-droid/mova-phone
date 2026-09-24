package com.studiolexair.movaphone.feature.assistant.ai

import android.app.ActivityManager
import android.content.Context
import com.studiolexair.movaphone.core.logging.MovaLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Modelos de lenguaje que se ejecutan **en el propio teléfono**.
 *
 * MOVA no usa servicios en la nube ni claves de API: descarga el modelo a su carpeta privada
 * (una sola vez, con permiso del usuario) y razona con él sin conexión. Se ofrecen dos
 * tamaños, del más ligero al más capaz:
 *
 *  - **Ligero (159 MB)**: SmolLM-135M — entiende órdenes cortas y responde al instante.
 *  - **Recomendado (521 MB)**: Qwen2.5-0.5B — entiende mejor frases completas en español.
 *
 * Si no hay modelo descargado, MOVA sigue funcionando igual con su intérprete de reglas
 * local: el modelo es una mejora, nunca un requisito.
 */
data class LocalModel(
    val id: String,
    val name: String,
    val description: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long
) {
    val sizeMb: Long get() = sizeBytes / (1024 * 1024)
}

enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, READY, ERROR }

class LocalModelManager(private val context: Context) {

    private val state = MutableStateFlow(ModelStatus.NOT_DOWNLOADED)
    val status: StateFlow<ModelStatus> = state.asStateFlow()

    private val progress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = progress.asStateFlow()

    private val message = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = message.asStateFlow()

    init {
        refresh()
    }

    fun models(): List<LocalModel> = listOf(
        LocalModel(
            id = "smollm-135m",
            name = "Modelo ligero",
            description = "159 MB · responde al instante, entiende órdenes cortas",
            fileName = "smollm-135m.task",
            url = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/" +
                "SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            sizeBytes = 166_723_584L
        ),
        LocalModel(
            id = "qwen2.5-0.5b",
            name = "Modelo recomendado",
            description = "521 MB · razona frases completas y entiende mejor el español",
            fileName = "qwen2.5-0.5b.task",
            url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/" +
                "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            sizeBytes = 546_308_096L
        )
    )

    fun modelFile(model: LocalModel): File = File(context.filesDir, model.fileName)

    fun isDownloaded(model: LocalModel): Boolean =
        modelFile(model).exists() && modelFile(model).length() > MIN_SIZE

    fun downloadedModel(): LocalModel? = models().firstOrNull { isDownloaded(it) }

    /** ¿Cabe y puede moverse este modelo en este teléfono? */
    fun canRun(model: LocalModel): Pair<Boolean, String> {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val isLowRam = activityManager?.isLowRamDevice == true
        val freeBytes = context.filesDir.usableSpace
        return when {
            isLowRam && model.sizeBytes > 200L * 1024 * 1024 ->
                false to "Este teléfono tiene poca memoria para el modelo grande. Prueba el ligero (159 MB)."
            freeBytes < model.sizeBytes * 1.2 ->
                false to "No hay espacio suficiente: hacen falta unos ${model.sizeMb} MB libres."
            else -> true to "Listo para descargar (${model.sizeMb} MB). Se guarda sólo en tu teléfono."
        }
    }

    /** Descarga el modelo con progreso real. Todo se queda en el teléfono. */
    suspend fun download(model: LocalModel): Boolean = withContext(Dispatchers.IO) {
        val canUse = canRun(model)
        if (!canUse.first) {
            message.value = canUse.second
            state.value = ModelStatus.ERROR
            return@withContext false
        }
        val target = modelFile(model)
        val partial = File(context.filesDir, model.fileName + ".part")
        state.value = ModelStatus.DOWNLOADING
        progress.value = 0f
        message.value = "Descargando ${model.name} (${model.sizeMb} MB)… puedes seguir usando MOVA mientras tanto."

        var connection: HttpURLConnection? = null
        return@withContext try {
            connection = (URL(model.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            connection.connect()
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: model.sizeBytes
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    var read = input.read(buffer)
                    var written = 0L
                    while (read >= 0) {
                        output.write(buffer, 0, read)
                        written += read
                        progress.value = (written.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        read = input.read(buffer)
                    }
                }
            }
            if (partial.length() < MIN_SIZE) {
                partial.delete()
                state.value = ModelStatus.ERROR
                message.value = "La descarga se interrumpió. Prueba de nuevo con buena conexión."
                return@withContext false
            }
            if (target.exists()) target.delete()
            partial.renameTo(target)
            state.value = ModelStatus.READY
            progress.value = 1f
            message.value = "${model.name} listo: MOVA ya razona en el propio teléfono, sin conexión."
            MovaLog.i(TAG, "Modelo ${model.id} descargado (${target.length()} bytes)")
            true
        } catch (t: Throwable) {
            MovaLog.e(TAG, "Fallo descargando el modelo local", t)
            partial.delete()
            state.value = ModelStatus.ERROR
            message.value = "No se pudo descargar el modelo: ${t.message ?: "error de red"}."
            false
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    fun delete(model: LocalModel) {
        runCatching { modelFile(model).delete() }
        state.value = ModelStatus.NOT_DOWNLOADED
        progress.value = 0f
        message.value = "Modelo borrado. MOVA vuelve a su intérprete local (sigue funcionando igual)."
    }

    fun refresh() {
        state.value = if (downloadedModel() != null) ModelStatus.READY else ModelStatus.NOT_DOWNLOADED
    }

    fun clearMessage() {
        message.value = null
    }

    private companion object {
        const val TAG = "LocalModelManager"
        const val MIN_SIZE = 10L * 1024 * 1024
    }
}
