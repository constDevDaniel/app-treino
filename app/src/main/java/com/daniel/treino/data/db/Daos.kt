package com.daniel.treino.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExercicioDao {
    @Query("SELECT * FROM Exercicio ORDER BY nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<Exercicio>>

    @Query("SELECT * FROM Exercicio WHERE LOWER(nome) = LOWER(:nome) LIMIT 1")
    suspend fun buscarPorNome(nome: String): Exercicio?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(exercicio: Exercicio): Long

    @Query("UPDATE Exercicio SET nome = :nome WHERE id = :id")
    suspend fun atualizarNome(id: Long, nome: String)

    @Query("UPDATE ExercicioModelo SET exercicioId = :destinoId WHERE exercicioId = :origemId")
    suspend fun reatribuirModelos(origemId: Long, destinoId: Long)

    @Query("UPDATE SerieRealizada SET exercicioId = :destinoId WHERE exercicioId = :origemId")
    suspend fun reatribuirSeries(origemId: Long, destinoId: Long)

    @Query("DELETE FROM Exercicio WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface TreinoDao {
    @Query("SELECT * FROM Treino ORDER BY id")
    fun observarTodos(): Flow<List<Treino>>

    @Query("SELECT * FROM Treino ORDER BY id")
    suspend fun listarTodos(): List<Treino>

    @Query("SELECT * FROM Treino WHERE id = :id")
    suspend fun buscarPorId(id: Long): Treino?

    @Query("SELECT COUNT(*) FROM Treino")
    suspend fun contar(): Int

    @Insert
    suspend fun inserir(treino: Treino): Long

    @Update
    suspend fun atualizar(treino: Treino)

    @Query("DELETE FROM Treino WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query(
        """
        SELECT em.id, em.exercicioId, e.nome, em.ordem, em.series, em.repeticoes
        FROM ExercicioModelo em JOIN Exercicio e ON e.id = em.exercicioId
        WHERE em.treinoId = :treinoId
        ORDER BY em.ordem
        """
    )
    fun observarModelo(treinoId: Long): Flow<List<ExercicioDoTreino>>

    @Query("SELECT * FROM ExercicioModelo WHERE treinoId = :treinoId ORDER BY ordem")
    suspend fun listarModelo(treinoId: Long): List<ExercicioModelo>

    @Query(
        """
        SELECT treinoId, COUNT(*) AS exercicios, COALESCE(SUM(series), 0) AS series
        FROM ExercicioModelo
        GROUP BY treinoId
        """
    )
    fun observarResumosModelo(): Flow<List<ResumoModelo>>

    @Query("DELETE FROM ExercicioModelo WHERE treinoId = :treinoId")
    suspend fun limparModelo(treinoId: Long)

    @Insert
    suspend fun inserirModelos(itens: List<ExercicioModelo>)

    @Transaction
    suspend fun substituirModelo(treinoId: Long, itens: List<ExercicioModelo>) {
        limparModelo(treinoId)
        inserirModelos(itens)
    }
}

@Dao
interface AgendaDao {
    @Query(
        """
        SELECT a.diaSemana, a.treinoId, t.nome AS treinoNome
        FROM AgendaDia a JOIN Treino t ON t.id = a.treinoId
        ORDER BY a.diaSemana
        """
    )
    fun observarAgenda(): Flow<List<DiaAgendado>>

    @Upsert
    suspend fun salvar(agendaDia: AgendaDia)

    @Query("DELETE FROM AgendaDia WHERE diaSemana = :diaSemana")
    suspend fun remover(diaSemana: Int)
}

@Dao
interface SessaoDao {
    @Query("SELECT * FROM Sessao WHERE emAndamento = 1 ORDER BY inicio DESC LIMIT 1")
    fun observarEmAndamento(): Flow<Sessao?>

    @Query("SELECT * FROM Sessao WHERE emAndamento = 0 AND inicio >= :desde ORDER BY inicio")
    fun observarConcluidasDesde(desde: Long): Flow<List<Sessao>>

    @Query("SELECT * FROM Sessao WHERE emAndamento = 0 AND inicio >= :inicio AND inicio < :fim")
    suspend fun listarConcluidasNoIntervalo(inicio: Long, fim: Long): List<Sessao>

    @Query(
        """
        SELECT s.id, s.treinoId, s.treinoNome, s.inicio,
               COUNT(sr.id) AS series,
               COALESCE(SUM(sr.repeticoes * sr.carga), 0) AS volume
        FROM Sessao s
        LEFT JOIN SerieRealizada sr ON sr.sessaoId = s.id AND sr.concluida = 1
        WHERE s.emAndamento = 0
        GROUP BY s.id
        ORDER BY s.inicio DESC
        """
    )
    fun observarHistorico(): Flow<List<SessaoNoHistorico>>

    @Query(
        """
        SELECT treinoId, MAX(inicio) AS inicio
        FROM Sessao
        WHERE emAndamento = 0 AND treinoId IS NOT NULL
        GROUP BY treinoId
        """
    )
    fun observarUltimasSessoes(): Flow<List<UltimaSessao>>

    @Query(
        """
        SELECT s.id, s.exercicioId, e.nome, s.ordemExercicio, s.numeroSerie,
               s.repeticoes, s.carga, s.concluida
        FROM SerieRealizada s JOIN Exercicio e ON e.id = s.exercicioId
        WHERE s.sessaoId = :sessaoId
        ORDER BY s.ordemExercicio, s.numeroSerie
        """
    )
    fun observarSeries(sessaoId: Long): Flow<List<SerieDaSessao>>

    @Query(
        """
        SELECT sr.* FROM SerieRealizada sr
        JOIN Sessao s ON s.id = sr.sessaoId
        WHERE sr.exercicioId = :exercicioId AND sr.numeroSerie = :numeroSerie
              AND s.emAndamento = 0 AND sr.concluida = 1
        ORDER BY s.inicio DESC
        LIMIT 1
        """
    )
    suspend fun buscarUltimaSerie(exercicioId: Long, numeroSerie: Int): SerieRealizada?

    @Insert
    suspend fun inserirSessao(sessao: Sessao): Long

    @Insert
    suspend fun inserirSeries(series: List<SerieRealizada>)

    @Query(
        """
        UPDATE SerieRealizada
        SET repeticoes = :repeticoes, carga = :carga, concluida = :concluida
        WHERE id = :id
        """
    )
    suspend fun atualizarSerie(id: Long, repeticoes: Int?, carga: Double?, concluida: Boolean)

    @Query("UPDATE Sessao SET emAndamento = 0, fim = :fim WHERE id = :id")
    suspend fun concluirSessao(id: Long, fim: Long)

    @Query("DELETE FROM Sessao WHERE id = :id")
    suspend fun excluirSessao(id: Long)
}

@Dao
interface ProgressoDao {
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM Sessao WHERE emAndamento = 0 AND inicio >= :desde) AS treinos,
            COUNT(sr.id) AS series,
            COALESCE(SUM(sr.repeticoes * sr.carga), 0) AS volume
        FROM SerieRealizada sr JOIN Sessao s ON s.id = sr.sessaoId
        WHERE s.emAndamento = 0 AND s.inicio >= :desde AND sr.concluida = 1
        """
    )
    fun observarResumoPeriodo(desde: Long): Flow<ResumoPeriodo>

    @Query(
        """
        SELECT strftime('%Y-%W', s.inicio / 1000, 'unixepoch', 'localtime') AS semana,
               COALESCE(SUM(sr.repeticoes * sr.carga), 0) AS volume
        FROM Sessao s JOIN SerieRealizada sr ON sr.sessaoId = s.id
        WHERE s.emAndamento = 0 AND s.inicio >= :desde AND sr.concluida = 1
        GROUP BY semana
        ORDER BY semana
        """
    )
    fun observarVolumeSemanal(desde: Long): Flow<List<VolumeSemana>>

    @Query(
        """
        SELECT DISTINCT e.* FROM Exercicio e
        JOIN SerieRealizada sr ON sr.exercicioId = e.id
        JOIN Sessao s ON s.id = sr.sessaoId
        WHERE s.emAndamento = 0 AND s.inicio >= :desde AND sr.concluida = 1 AND sr.carga IS NOT NULL
        ORDER BY e.nome COLLATE NOCASE
        """
    )
    fun observarExerciciosComCarga(desde: Long): Flow<List<Exercicio>>

    @Query(
        """
        SELECT s.inicio AS inicio, MAX(sr.carga) AS carga
        FROM Sessao s JOIN SerieRealizada sr ON sr.sessaoId = s.id
        WHERE s.emAndamento = 0 AND s.inicio >= :desde AND sr.exercicioId = :exercicioId
              AND sr.concluida = 1 AND sr.carga IS NOT NULL
        GROUP BY s.id
        ORDER BY s.inicio
        """
    )
    fun observarCargasExercicio(desde: Long, exercicioId: Long): Flow<List<CargaNaSessao>>

    @Query(
        """
        SELECT e.id AS exercicioId, e.nome AS nome, sr.carga AS carga, sr.repeticoes AS repeticoes,
               MIN(s.inicio) AS data
        FROM SerieRealizada sr
        JOIN Sessao s ON s.id = sr.sessaoId
        JOIN Exercicio e ON e.id = sr.exercicioId
        WHERE s.emAndamento = 0 AND sr.concluida = 1 AND sr.carga IS NOT NULL
          AND sr.carga = (
              SELECT MAX(sr2.carga) FROM SerieRealizada sr2 JOIN Sessao s2 ON s2.id = sr2.sessaoId
              WHERE sr2.exercicioId = sr.exercicioId AND s2.emAndamento = 0 AND sr2.concluida = 1
          )
        GROUP BY e.id
        ORDER BY e.nome COLLATE NOCASE
        """
    )
    fun observarRecordes(): Flow<List<Recorde>>

    fun resumoPeriodo(desde: Long): Flow<ResumoPeriodo> = observarResumoPeriodo(desde)
    fun volumeSemanal(desde: Long): Flow<List<VolumeSemana>> = observarVolumeSemanal(desde)
    fun exerciciosNoPeriodo(desde: Long): Flow<List<Exercicio>> = observarExerciciosComCarga(desde)
    fun evolucaoCarga(exercicioId: Long, desde: Long): Flow<List<CargaNaSessao>> = observarCargasExercicio(desde, exercicioId)
    fun recordesPessoais(): Flow<List<Recorde>> = observarRecordes()
}

typealias RelatoriosDao = ProgressoDao

@Dao
interface BackupDao {
    @Query("SELECT * FROM Exercicio ORDER BY id")
    suspend fun listarExercicios(): List<Exercicio>

    @Query("SELECT * FROM Treino ORDER BY id")
    suspend fun listarTreinos(): List<Treino>

    @Query("SELECT * FROM ExercicioModelo ORDER BY id")
    suspend fun listarModelos(): List<ExercicioModelo>

    @Query("SELECT * FROM AgendaDia ORDER BY diaSemana")
    suspend fun listarAgenda(): List<AgendaDia>

    @Query("SELECT * FROM Sessao ORDER BY id")
    suspend fun listarSessoes(): List<Sessao>

    @Query("SELECT * FROM SerieRealizada ORDER BY id")
    suspend fun listarSeries(): List<SerieRealizada>

    @Insert
    suspend fun inserirExercicios(itens: List<Exercicio>)

    @Insert
    suspend fun inserirTreinos(itens: List<Treino>)

    @Insert
    suspend fun inserirModelos(itens: List<ExercicioModelo>)

    @Insert
    suspend fun inserirAgenda(itens: List<AgendaDia>)

    @Insert
    suspend fun inserirSessoes(itens: List<Sessao>)

    @Insert
    suspend fun inserirSeries(itens: List<SerieRealizada>)

    @Query("DELETE FROM SerieRealizada")
    suspend fun limparSeries()

    @Query("DELETE FROM Sessao")
    suspend fun limparSessoes()

    @Query("DELETE FROM AgendaDia")
    suspend fun limparAgenda()

    @Query("DELETE FROM ExercicioModelo")
    suspend fun limparModelos()

    @Query("DELETE FROM Treino")
    suspend fun limparTreinos()

    @Query("DELETE FROM Exercicio")
    suspend fun limparExercicios()

    @Transaction
    suspend fun restaurarTudo(
        exercicios: List<Exercicio>,
        treinos: List<Treino>,
        modelos: List<ExercicioModelo>,
        agenda: List<AgendaDia>,
        sessoes: List<Sessao>,
        series: List<SerieRealizada>
    ) {
        limparSeries()
        limparSessoes()
        limparAgenda()
        limparModelos()
        limparTreinos()
        limparExercicios()
        inserirExercicios(exercicios)
        inserirTreinos(treinos)
        inserirModelos(modelos)
        inserirAgenda(agenda)
        inserirSessoes(sessoes)
        inserirSeries(series)
    }
}
