package com.daniel.treino.data.backup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class EstadoBackup(
    val destino: Uri? = null,
    val nomeArquivo: String? = null,
    val local: String? = null,
    val destinoPermanente: Boolean = false,
    val automatico: Boolean = false,
    val ultimoBackup: Long? = null,
    val erro: String? = null,
    val salvando: Boolean = false
)

class GerenciadorBackup(
    val context: Context,
    private val backupJson: BackupJson,
    private val escopo: CoroutineScope
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("backup", Context.MODE_PRIVATE)
    }

    private val mutex = Mutex()

    private val _estado: MutableStateFlow<EstadoBackup>
    val estado: StateFlow<EstadoBackup>

    init {
        val destinoStr = prefs.getString("destino", null)
        val destinoUri = destinoStr?.let { Uri.parse(it) }
        val ultimo = prefs.getLong("ultimo_backup", -1L).takeIf { it > 0L }

        _estado = MutableStateFlow(
            EstadoBackup(
                destino = destinoUri,
                nomeArquivo = prefs.getString("nome", null),
                local = prefs.getString("local", null),
                destinoPermanente = destinoUri != null,
                automatico = prefs.getBoolean("automatico", true),
                ultimoBackup = ultimo,
                erro = prefs.getString("erro", null),
                salvando = false
            )
        )
        estado = _estado.asStateFlow()

        if (destinoUri != null) {
            escopo.launch(Dispatchers.IO) {
                val temPermissao = runCatching {
                    context.contentResolver.persistedUriPermissions.any {
                        it.uri == destinoUri && it.isWritePermission
                    }
                }.getOrDefault(false)

                if (!temPermissao) {
                    _estado.update { it.copy(destinoPermanente = false) }
                }
            }
        }
    }

    fun definirAutomatico(ativo: Boolean) {
        prefs.edit().putBoolean("automatico", ativo).apply()
        _estado.update { it.copy(automatico = ativo) }
    }

    suspend fun definirDestino(uri: Uri, automatico: Boolean = true): Boolean {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val permanente = try {
            context.contentResolver.takePersistableUriPermission(uri, flag)
            true
        } catch (e: SecurityException) {
            Log.w("BackupArquivo", "Sem permissão permanente para $uri", e)
            false
        }

        val nome = runCatching {
            context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

        val authority = uri.authority
        val local = when {
            authority != null && authority.startsWith("com.google.android.apps.docs") -> "no Google Drive"
            authority in setOf(
                "com.android.externalstorage.documents",
                "com.android.providers.downloads.documents",
                "com.android.providers.media.documents"
            ) -> "neste celular"
            else -> "em outro app de arquivos"
        }

        prefs.edit()
            .putString("destino", uri.toString())
            .putString("nome", nome)
            .putString("local", local)
            .apply()

        _estado.update {
            it.copy(
                destino = uri,
                nomeArquivo = nome,
                local = local,
                destinoPermanente = permanente,
                erro = null
            )
        }

        fazerBackupAgora()
        return permanente
    }

    fun aoConcluirSessao() {
        val atual = estado.value
        if (!atual.automatico || atual.destino == null) return
        escopo.launch {
            fazerBackupAgora()
        }
    }

    suspend fun fazerBackupAgora(): Boolean {
        val uri = estado.value.destino ?: return false
        return mutex.withLock {
            _estado.update { it.copy(salvando = true, erro = null) }
            try {
                withContext(Dispatchers.IO) {
                    val conteudo = backupJson.exportar()
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Não foi possível abrir o arquivo de backup.")
                    stream.use { out ->
                        out.write(conteudo.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }
                val agora = System.currentTimeMillis()
                prefs.edit()
                    .putLong("ultimo_backup", agora)
                    .remove("erro")
                    .apply()
                _estado.update {
                    it.copy(
                        ultimoBackup = agora,
                        erro = null,
                        salvando = false
                    )
                }
                true
            } catch (e: Exception) {
                Log.w("BackupArquivo", "Falha ao salvar backup", e)
                val msg = if (e is SecurityException) {
                    "Permissão expirada. Escolha o arquivo novamente."
                } else {
                    "Não foi possível salvar no arquivo de backup."
                }
                prefs.edit().putString("erro", msg).apply()
                _estado.update {
                    it.copy(
                        destinoPermanente = if (e is SecurityException) false else it.destinoPermanente,
                        erro = msg,
                        salvando = false
                    )
                }
                false
            }
        }
    }

    suspend fun lerArquivo(uri: Uri): DadosBackup = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw BackupInvalidoException("Não foi possível abrir o arquivo.")
        val texto = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        backupJson.ler(texto)
    }

    suspend fun lerBackup(uri: Uri): DadosBackup = lerArquivo(uri)

    suspend fun restaurar(dados: DadosBackup) = withContext(Dispatchers.IO) {
        backupJson.restaurar(dados)
    }
}

typealias BackupArquivo = GerenciadorBackup
