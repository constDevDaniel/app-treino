package com.daniel.treino.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Exercicio::class,
        Treino::class,
        ExercicioModelo::class,
        AgendaDia::class,
        Sessao::class,
        SerieRealizada::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TreinoDatabase : RoomDatabase() {
    abstract fun exercicioDao(): ExercicioDao
    abstract fun treinoDao(): TreinoDao
    abstract fun agendaDao(): AgendaDao
    abstract fun sessaoDao(): SessaoDao
    abstract fun progressoDao(): ProgressoDao
    abstract fun backupDao(): BackupDao

    fun relatoriosDao(): ProgressoDao = progressoDao()

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Treino ADD COLUMN icone TEXT")
            }
        }
    }
}
