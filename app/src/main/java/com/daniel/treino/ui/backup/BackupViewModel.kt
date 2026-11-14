package com.daniel.treino.ui.backup

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daniel.treino.data.backup.BackupArquivo
import com.daniel.treino.data.backup.ConteudoBackup
import com.daniel.treino.data.backup.EstadoBackup
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Reconstructed from U1/s.java, U1/d.java, U1/o..r.java, B0/C0083p.java (cases 3-7), B0/C0085q.java (case 6)

class BackupViewModel(
    private val arquivo: BackupArquivo,
) : ViewModel() {

    val estado: StateFlow<EstadoBackup> = arquivo.estado

    var previaRestauracao by mutableStateOf<ConteudoBackup?>(null)
        private set

    var mensagem by mutableStateOf<String?>(null)
        private set

    var ocupado by mutableStateOf(false)
        private set

    fun escolherDestino(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            ocupado = true
            val permanente = arquivo.definirDestino(uri, automatico = true)
            val salvou = arquivo.fazerBackupAgora()
            ocupado = false
            if (salvou) {
                mensagem = if (permanente) {
                    "Backup salvo. A partir de agora o app atualiza esse arquivo sozinho a cada treino."
                } else {
                    "Backup salvo neste arquivo. O provedor não permitiu acesso automático; use \"Salvar em outro local\" quando quiser atualizar."
                }
            }
        }
    }

    fun fazerBackupAgora() {
        viewModelScope.launch {
            ocupado = true
            val salvou = arquivo.fazerBackupAgora()
            ocupado = false
            if (salvou) {
                mensagem = "Backup atualizado."
            }
        }
    }

    fun definirAutomatico(ativo: Boolean) {
        arquivo.definirAutomatico(ativo)
    }

    fun escolherArquivoRestaurar(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            ocupado = true
            runCatching { arquivo.lerBackup(uri) }
                .onSuccess { previaRestauracao = it }
                .onFailure { e ->
                    mensagem = e.message ?: "Não foi possível ler o arquivo de backup."
                }
            ocupado = false
        }
    }

    fun confirmarRestauracao() {
        val dados = previaRestauracao ?: return
        previaRestauracao = null
        viewModelScope.launch {
            ocupado = true
            runCatching { arquivo.restaurar(dados) }
                .onSuccess {
                    val r = dados.resumo()
                    mensagem = "Backup restaurado: ${r.treinos} treinos e ${r.sessoesConcluidas} sessões."
                }
                .onFailure { e ->
                    mensagem = "Falha ao restaurar: ${e.message ?: "erro desconhecido"}"
                }
            ocupado = false
        }
    }

    fun cancelarRestauracao() {
        previaRestauracao = null
    }

    fun limparMensagem() {
        mensagem = null
    }

    companion object {
        fun factory(arquivo: BackupArquivo): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BackupViewModel(arquivo)
                }
            }
    }
}
