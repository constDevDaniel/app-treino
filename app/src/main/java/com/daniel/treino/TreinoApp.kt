package com.daniel.treino

import android.app.Application
import androidx.room.Room
import com.daniel.treino.data.backup.BackupJson
import com.daniel.treino.data.backup.GerenciadorBackup
import com.daniel.treino.data.db.TreinoDatabase
import com.daniel.treino.data.planilha.ImportadorPlanilha
import com.daniel.treino.data.repository.CatalogoInicial
import com.daniel.treino.data.repository.SessaoRepository
import com.daniel.treino.data.repository.TreinoRepository
import com.daniel.treino.timer.DescansoTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TreinoApp : Application() {
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val db: TreinoDatabase by lazy {
        Room.databaseBuilder(this, TreinoDatabase::class.java, "treino.db")
            .addMigrations(TreinoDatabase.MIGRATION_1_2)
            .build()
    }

    val sessoes: SessaoRepository by lazy {
        SessaoRepository(db, escopo) {
            backup.aoConcluirSessao()
        }
    }

    val treinos: TreinoRepository by lazy {
        TreinoRepository(db)
    }

    val descanso: DescansoTimer by lazy {
        DescansoTimer(this, escopo)
    }

    val catalogo: CatalogoInicial by lazy {
        CatalogoInicial(db, treinos)
    }

    val backup: GerenciadorBackup by lazy {
        GerenciadorBackup(this, BackupJson(db), escopo)
    }

    val importadorHistorico: ImportadorPlanilha by lazy {
        ImportadorPlanilha(db)
    }

    val database: TreinoDatabase get() = db
    val sessaoRepository: SessaoRepository get() = sessoes
    val treinoRepository: TreinoRepository get() = treinos
    val descansoTimer: DescansoTimer get() = descanso
    val backupArquivo: GerenciadorBackup get() = backup

    override fun onCreate() {
        super.onCreate()
        escopo.launch(Dispatchers.IO) {
            catalogo.garantirCatalogo(getExternalFilesDir(null))
        }
    }
}
