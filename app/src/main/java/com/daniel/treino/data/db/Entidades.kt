package com.daniel.treino.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Exercicio",
    indices = [Index(value = ["nome"], unique = true)]
)
data class Exercicio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String
)

@Entity(tableName = "Treino")
data class Treino(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val icone: String? = null
)

@Entity(
    tableName = "ExercicioModelo",
    foreignKeys = [
        ForeignKey(
            entity = Treino::class,
            parentColumns = ["id"],
            childColumns = ["treinoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercicio::class,
            parentColumns = ["id"],
            childColumns = ["exercicioId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["treinoId"]),
        Index(value = ["exercicioId"])
    ]
)
data class ExercicioModelo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treinoId: Long,
    val exercicioId: Long,
    val ordem: Int,
    val series: Int,
    val repeticoes: Int
)

@Entity(
    tableName = "AgendaDia",
    foreignKeys = [
        ForeignKey(
            entity = Treino::class,
            parentColumns = ["id"],
            childColumns = ["treinoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["treinoId"])]
)
data class AgendaDia(
    @PrimaryKey
    val diaSemana: Int,
    val treinoId: Long
)

@Entity(
    tableName = "Sessao",
    foreignKeys = [
        ForeignKey(
            entity = Treino::class,
            parentColumns = ["id"],
            childColumns = ["treinoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["treinoId"]),
        Index(value = ["inicio"]),
        Index(value = ["emAndamento"])
    ]
)
data class Sessao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treinoId: Long?,
    val treinoNome: String,
    val inicio: Long,
    val fim: Long? = null,
    val emAndamento: Boolean = true
) {
    val nomeTreino: String get() = treinoNome
}

@Entity(
    tableName = "SerieRealizada",
    foreignKeys = [
        ForeignKey(
            entity = Sessao::class,
            parentColumns = ["id"],
            childColumns = ["sessaoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercicio::class,
            parentColumns = ["id"],
            childColumns = ["exercicioId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sessaoId"]),
        Index(value = ["exercicioId"])
    ]
)
data class SerieRealizada(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessaoId: Long,
    val exercicioId: Long,
    val ordemExercicio: Int,
    val numeroSerie: Int,
    val repeticoes: Int?,
    val carga: Double?,
    val concluida: Boolean = false
)

// ==================== Query Projections / DTOs ====================

data class ExercicioDoTreino(
    val id: Long,
    val exercicioId: Long,
    val nome: String,
    val ordem: Int,
    val series: Int,
    val repeticoes: Int
) {
    val nomeExercicio: String get() = nome
    val seriesPlanejadas: Int get() = series
    val repeticoesAlvo: Int get() = repeticoes
}

data class ResumoModelo(
    val treinoId: Long,
    val exercicios: Int,
    val series: Int
)

typealias ResumoTreino = ResumoModelo

data class DiaAgendado(
    val diaSemana: Int,
    val treinoId: Long,
    val treinoNome: String
)

data class SerieDaSessao(
    val id: Long,
    val exercicioId: Long,
    val nome: String,
    val ordemExercicio: Int,
    val numeroSerie: Int,
    val repeticoes: Int?,
    val carga: Double?,
    val concluida: Boolean
) {
    val nomeExercicio: String get() = nome
    val ordem: Int get() = numeroSerie
}

data class SessaoNoHistorico(
    val id: Long,
    val treinoId: Long?,
    val treinoNome: String,
    val inicio: Long,
    val series: Int,
    val volume: Double
) {
    val nomeTreino: String get() = treinoNome
}

typealias SessaoHistorico = SessaoNoHistorico

data class UltimaSessao(
    val treinoId: Long,
    val inicio: Long
)

data class ResumoPeriodo(
    val treinos: Int,
    val series: Int,
    val volume: Double
)

data class VolumeSemana(
    val semana: String,
    val volume: Double
)

typealias VolumeSemanal = VolumeSemana

data class CargaNaSessao(
    val inicio: Long,
    val carga: Double
) {
    val cargaMaxima: Double get() = carga
}

typealias EvolucaoCarga = CargaNaSessao

data class Recorde(
    val exercicioId: Long,
    val nome: String,
    val carga: Double,
    val repeticoes: Int?,
    val data: Long
) {
    val cargaMaxima: Double get() = carga
    val inicio: Long get() = data
}

typealias RecordePessoal = Recorde
